package com.skysentinel.security.auth;

import com.skysentinel.security.config.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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
        assertThat(response.message()).isEqualTo("A 6-digit verification code has been sent to your registered phone number.");
        assertThat(notificationService.sentTo.get()).isEqualTo("+27821234567");
        assertThat(savedChallenge.get().getUser()).isSameAs(savedUser.get());
        assertThat(savedChallenge.get().getProviderChallengeId()).isEqualTo("pin-123");
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
        assertThat(notificationService.sentTo.get()).isEqualTo(user.getCellphoneNumber());

        AuthResponse authResponse = service.verifyOtp(new VerifyOtpRequest(challengeResponse.challengeId(), "123456"));

        assertThat(authResponse.token()).isNotBlank();
        assertThat(authResponse.user().email()).isEqualTo(user.getEmail());
        assertThat(savedChallenge.get().getUsedAt()).isNotNull();
    }

    @Test
    void forgottenPasswordCanBeResetWithOtpAndRegisteredCellphoneNumber() {
        User user = operator();
        AtomicReference<OtpChallenge> savedChallenge = new AtomicReference<>();
        UserRepository userRepository = userRepository(user);
        OtpChallengeRepository otpChallengeRepository = otpChallengeRepository(savedChallenge);
        AuthService service = serviceWithOtpExpiryMinutes(5, userRepository, otpChallengeRepository);

        OtpChallengeResponse challengeResponse = service.forgotPassword(new ForgotPasswordRequest(user.getEmail(), user.getCellphoneNumber()));

        assertThat(challengeResponse.challengeId()).isNotBlank();
        assertThat(notificationService.sentTo.get()).isEqualTo(user.getCellphoneNumber());

        String message = service.resetPassword(new ResetPasswordRequest(challengeResponse.challengeId(), "123456", "new-secret-321"));

        assertThat(message).isEqualTo("Password reset successfully.");
        assertThat(passwordEncoder.matches("new-secret-321", user.getPasswordHash())).isTrue();
    }

    @Test
    void localDevelopmentFallbackAllowsRegistrationAndOtpVerificationWithoutInfobipConfiguration() {
        OperatorNotificationService localNotificationService = new OperatorNotificationService(
                RestClient.builder(),
                "",
                "",
                "",
                ""
        );
        User user = new User(
                "ops+other@skysentinel.test",
                passwordEncoder.encode("secret123"),
                "Ava Operator",
                "+27881234567",
                "CONTROL_ROOM_OPERATOR",
                "OP-202"
        );
        user.setId(2L);
        AtomicReference<OtpChallenge> savedChallenge = new AtomicReference<>();
        AuthService service = new AuthService(
                userRepository(user),
                otpChallengeRepository(savedChallenge),
                passwordEncoder,
                jwtTokenService,
                localNotificationService,
                5,
                0,
                3,
                5
        );

        OtpChallengeResponse challengeResponse = service.login(new LoginRequest(user.getEmail(), "secret123"));
        String localDevCode = OperatorNotificationService.localDevelopmentCodeFor(user.getCellphoneNumber());

        AuthResponse authResponse = service.verifyOtp(new VerifyOtpRequest(challengeResponse.challengeId(), localDevCode));

        assertThat(challengeResponse.challengeId()).isNotBlank();
        assertThat(savedChallenge.get().getProviderChallengeId()).isEqualTo(OperatorNotificationService.localDevelopmentChallengeIdFor(user.getCellphoneNumber()));
        assertThat(authResponse.token()).isNotBlank();
        assertThat(authResponse.user().email()).isEqualTo(user.getEmail());
    }

    @Test
    void loginRequiresSmsDelivery() {
        notificationService.deliveryResult.set(OtpDeliveryResult.failed("Verification code could not be sent by SMS."));
        AuthService service = serviceWithOtpExpiryMinutes(
                5,
                userRepository(operator()),
                otpChallengeRepository(new AtomicReference<>())
        );

        assertThatThrownBy(() -> service.login(new LoginRequest("ops@skysentinel.test", "secret123")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("could not be sent by SMS");
    }

    @Test
    void resendIsRateLimitedByCooldown() {
        OtpChallenge challenge = new OtpChallenge(
                "challenge-123",
                operator(),
                passwordEncoder.encode("123456"),
                java.time.Instant.now().plusSeconds(300)
        );
        AtomicReference<OtpChallenge> savedChallenge = new AtomicReference<>(challenge);
        AuthService service = new AuthService(
                userRepository(challenge.getUser()),
                otpChallengeRepository(savedChallenge),
                passwordEncoder,
                jwtTokenService,
                notificationService,
                5,
                30,
                3,
                5
        );

        assertThatThrownBy(() -> service.resendOtp(new ResendOtpRequest("challenge-123")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Please wait before requesting another verification code");
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
                    case "findByUserAndUsedAtIsNull" -> List.of();
                    case "save" -> {
                        savedChallenge.set((OtpChallenge) args[0]);
                        yield args[0];
                    }
                    case "saveAll" -> args[0];
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
        private final AtomicReference<String> sentTo = new AtomicReference<>();
        private final AtomicReference<OtpDeliveryResult> deliveryResult = new AtomicReference<>(OtpDeliveryResult.delivered("pin-123"));

        private CapturingNotificationService() {
            super(RestClient.builder(), "api-key", "https://example.infobip.com", "app-123", "message-123");
        }

        @Override
        public OtpDeliveryResult sendOtp(User user) {
            sentTo.set(user.getCellphoneNumber());
            return deliveryResult.get();
        }

        @Override
        public boolean verifyOtp(String providerChallengeId, String code) {
            return "pin-123".equals(providerChallengeId) && "123456".equals(code);
        }
    }

}
