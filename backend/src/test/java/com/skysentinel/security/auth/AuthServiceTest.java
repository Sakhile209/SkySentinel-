package com.skysentinel.security.auth;

import com.skysentinel.security.config.JwtTokenService;
import org.springframework.beans.factory.ObjectProvider;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtTokenService jwtTokenService = new JwtTokenService("test-secret-with-enough-entropy", 60000);
    private final CapturingNotificationService notificationService = new CapturingNotificationService();

    @Test
    void registrationSendsOtpChallengeBeforeAccess() {
        AtomicReference<User> savedUser = new AtomicReference<>();
        AtomicReference<OtpChallenge> savedChallenge = new AtomicReference<>();
        AuthService service = serviceWithOtpExpiryMinutes(
                5,
                registeringUserRepository(savedUser),
                otpChallengeRepository(savedChallenge)
        );

        RegistrationResponse response = service.register(new RegisterRequest(
                "Ava Operator",
                "ops@skysentinel.test",
                "082 123 4567",
                "OP-101",
                "secret123",
                null
        ));

        assertThat(response.challengeId()).isNotBlank();
        assertThat(response.expiresAt()).isNotNull();
        assertThat(response.message()).contains("registered email and South African cellphone");
        assertThat(notificationService.sentCode.get()).matches("\\d{6}");
        assertThat(savedChallenge.get().getUser()).isSameAs(savedUser.get());
        assertThat(savedUser.get().getCellphoneNumber()).isEqualTo("+27821234567");
    }

    @Test
    void loginRequiresOtpBeforeIssuingJwt() {
        User user = operator();
        AtomicReference<OtpChallenge> savedChallenge = new AtomicReference<>();
        UserRepository userRepository = userRepository(user);
        OtpChallengeRepository otpChallengeRepository = otpChallengeRepository(savedChallenge);
        AuthService service = serviceWithOtpExpiryMinutes(5, userRepository, otpChallengeRepository);

        OtpChallengeResponse challengeResponse = service.login(new LoginRequest(user.getEmail(), "secret123"));

        assertThat(challengeResponse.challengeId()).isNotBlank();
        assertThat(notificationService.sentCode.get()).matches("\\d{6}");

        AuthResponse authResponse = service.verifyOtp(new VerifyOtpRequest(challengeResponse.challengeId(), notificationService.sentCode.get()));

        assertThat(authResponse.token()).isNotBlank();
        assertThat(authResponse.user().email()).isEqualTo(user.getEmail());
        assertThat(savedChallenge.get().getUsedAt()).isNotNull();
    }

    @Test
    void resendIsBlockedUntilCurrentOtpExpires() {
        OtpChallenge challenge = new OtpChallenge(
                "challenge-123",
                operator(),
                passwordEncoder.encode("123456"),
                java.time.Instant.now().plusSeconds(300)
        );
        AtomicReference<OtpChallenge> savedChallenge = new AtomicReference<>(challenge);
        AuthService service = serviceWithOtpExpiryMinutes(5, userRepository(challenge.getUser()), otpChallengeRepository(savedChallenge));

        assertThatThrownBy(() -> service.resendOtp(new ResendOtpRequest("challenge-123")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("after the current code expires");
    }

    @Test
    void registrationRejectsNonSouthAfricanCellphoneNumbers() {
        AuthService service = serviceWithOtpExpiryMinutes(
                5,
                registeringUserRepository(new AtomicReference<>()),
                otpChallengeRepository(new AtomicReference<>())
        );

        assertThatThrownBy(() -> service.register(new RegisterRequest(
                "Ava Operator",
                "ops@skysentinel.test",
                "+14155552671",
                "OP-101",
                "secret123",
                null
        )))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("valid South African cellphone number");
    }

    private AuthService serviceWithOtpExpiryMinutes(long expiryMinutes, UserRepository userRepository, OtpChallengeRepository otpChallengeRepository) {
        return new AuthService(
                userRepository,
                otpChallengeRepository,
                passwordEncoder,
                jwtTokenService,
                notificationService,
                expiryMinutes,
                0,
                3,
                5
        );
    }

    private UserRepository userRepository(User user) {
        return (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByEmail" -> Optional.of(user).filter(value -> value.getEmail().equals(args[0]));
                    case "existsByEmail" -> user.getEmail().equals(args[0]);
                    case "existsByCellphoneNumber" -> user.getCellphoneNumber().equals(args[0]);
                    case "save" -> args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private UserRepository registeringUserRepository(AtomicReference<User> savedUser) {
        return (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByEmail" -> Optional.ofNullable(savedUser.get())
                            .filter(value -> value.getEmail().equals(args[0]));
                    case "existsByEmail", "existsByCellphoneNumber" -> false;
                    case "save" -> {
                        User user = (User) args[0];
                        user.setId(1L);
                        savedUser.set(user);
                        yield user;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private OtpChallengeRepository otpChallengeRepository(AtomicReference<OtpChallenge> savedChallenge) {
        return (OtpChallengeRepository) Proxy.newProxyInstance(
                OtpChallengeRepository.class.getClassLoader(),
                new Class<?>[]{OtpChallengeRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByChallengeId" -> Optional.ofNullable(savedChallenge.get())
                            .filter(challenge -> challenge.getChallengeId().equals(args[0]));
                    case "save" -> {
                        savedChallenge.set((OtpChallenge) args[0]);
                        yield args[0];
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private User operator() {
        User user = new User(
                "ops@skysentinel.test",
                passwordEncoder.encode("secret123"),
                "Ava Operator",
                "+27821234567",
                "CONTROL_ROOM_OPERATOR",
                "OP-101"
        );
        user.setId(1L);
        return user;
    }

    private static class CapturingNotificationService extends OperatorNotificationService {
        private final AtomicReference<String> sentCode = new AtomicReference<>();

        private CapturingNotificationService() {
            super(new EmptyMailProvider(), RestClient.builder(), "no-reply@skysentinel.test", "", "");
        }

        @Override
        public OtpDeliveryResult sendOtp(User user, String code) {
            sentCode.set(code);
            return new OtpDeliveryResult(true, true);
        }
    }

    private static class EmptyMailProvider implements ObjectProvider<JavaMailSender> {
        @Override
        public JavaMailSender getObject(Object... args) {
            throw new UnsupportedOperationException();
        }

        @Override
        public JavaMailSender getIfAvailable() {
            return null;
        }

        @Override
        public JavaMailSender getIfUnique() {
            return null;
        }

        @Override
        public JavaMailSender getObject() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Stream<JavaMailSender> stream() {
            return Stream.empty();
        }

        @Override
        public Stream<JavaMailSender> orderedStream() {
            return Stream.empty();
        }
    }
}
