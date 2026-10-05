package com.skysentinel.security.auth;

import com.skysentinel.security.config.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OtpChallengeRepository otpChallengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final OperatorNotificationService notificationService;
    private static final Pattern SOUTH_AFRICAN_CELLPHONE = Pattern.compile("^\\+27[6-8]\\d{8}$");
    private static final String OTP_SENT_MESSAGE = "A 6-digit verification code has been sent to your registered phone number.";
    private final Duration otpExpiration;
    private final Duration resendCooldown;
    private final int maxResends;
    private final int maxAttempts;

    public AuthService(
            UserRepository userRepository,
            OtpChallengeRepository otpChallengeRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            OperatorNotificationService notificationService,
            @Value("${skysentinel.auth.otp-expiration-minutes:5}") long otpExpirationMinutes,
            @Value("${skysentinel.auth.otp-resend-cooldown-seconds:30}") long resendCooldownSeconds,
            @Value("${skysentinel.auth.otp-max-resends:3}") int maxResends,
            @Value("${skysentinel.auth.otp-max-attempts:5}") int maxAttempts) {
        this.userRepository = userRepository;
        this.otpChallengeRepository = otpChallengeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.notificationService = notificationService;
        this.otpExpiration = Duration.ofMinutes(otpExpirationMinutes);
        this.resendCooldown = Duration.ofSeconds(resendCooldownSeconds);
        this.maxResends = maxResends;
        this.maxAttempts = maxAttempts;
    }

    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        String cellphone = normalizeCellphone(request.cellphoneNumber());
        String badge = request.badgeNumber().trim();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
        if (userRepository.existsByCellphoneNumber(cellphone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cellphone number is already registered");
        }

        String role = request.role();
        if (role == null || role.isBlank()) {
            role = "CONTROL_ROOM_OPERATOR";
        }

        User user = new User(
                email,
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                cellphone,
                role.toUpperCase(),
                badge
        );

        user = userRepository.save(user);
        notificationService.sendRegistrationConfirmation(user);
        OtpChallengeResponse challenge = createAndSendOtp(user);
        return new RegistrationResponse(
                challenge.message(),
                UserDto.fromEntity(user),
                challenge.challengeId(),
                challenge.expiresAt()
        );
    }

    @Transactional
    public OtpChallengeResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return createAndSendOtp(user);
    }

    @Transactional
    public OtpChallengeResponse forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        String cellphone = normalizeCellphone(request.cellphoneNumber());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "We could not find an account with that email and cellphone number."));

        if (!user.getCellphoneNumber().equals(cellphone)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The email and cellphone number do not match this account.");
        }

        return createAndSendOtp(user);
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        OtpChallenge challenge = otpChallengeRepository.findByChallengeId(request.challengeId().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid verification challenge"));

        if (challenge.getUsedAt() != null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Verification code has already been used");
        }
        if (Instant.now().isAfter(challenge.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Verification code has expired");
        }
        if (challenge.getAttempts() >= maxAttempts) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many verification attempts");
        }

        challenge.setAttempts(challenge.getAttempts() + 1);
        if (!isOtpValid(challenge, request.code())) {
            otpChallengeRepository.save(challenge);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid verification code");
        }

        challenge.setUsedAt(Instant.now());
        otpChallengeRepository.save(challenge);

        User user = challenge.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        return "Password reset successfully.";
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        OtpChallenge challenge = otpChallengeRepository.findByChallengeId(request.challengeId().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid verification challenge"));

        if (challenge.getUsedAt() != null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Verification code has already been used");
        }
        if (Instant.now().isAfter(challenge.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Verification code has expired");
        }
        if (challenge.getAttempts() >= maxAttempts) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many verification attempts");
        }

        challenge.setAttempts(challenge.getAttempts() + 1);
        if (!isOtpValid(challenge, request.code())) {
            otpChallengeRepository.save(challenge);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid verification code");
        }

        challenge.setUsedAt(Instant.now());
        otpChallengeRepository.save(challenge);
        User user = challenge.getUser();
        String token = jwtTokenService.generateToken(user.getId(), user.getEmail(), user.getRole(), user.getFullName());
        return new AuthResponse(token, UserDto.fromEntity(user));
    }

    @Transactional
    public OtpChallengeResponse resendOtp(ResendOtpRequest request) {
        OtpChallenge challenge = otpChallengeRepository.findByChallengeId(request.challengeId().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Verification challenge not found"));

        if (challenge.getUsedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Verification challenge has already been used");
        }
        if (challenge.getResendCount() >= maxResends) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Verification code resend limit reached");
        }
        Instant now = Instant.now();
        if (challenge.getLastSentAt() != null && now.isBefore(challenge.getLastSentAt().plus(resendCooldown))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Please wait before requesting another verification code");
        }

        OtpDeliveryResult delivery = notificationService.sendOtp(challenge.getUser());
        ensureOtpDelivered(delivery);
        challenge.setCodeHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        challenge.setProviderChallengeId(delivery.providerChallengeId());
        challenge.setExpiresAt(now.plus(otpExpiration));
        challenge.setAttempts(0);
        challenge.setResendCount(challenge.getResendCount() + 1);
        challenge.setLastSentAt(now);
        challenge = otpChallengeRepository.save(challenge);
        return new OtpChallengeResponse(
                challenge.getChallengeId(),
                OTP_SENT_MESSAGE,
                challenge.getExpiresAt()
        );
    }

    public UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return UserDto.fromEntity(user);
    }

    private OtpChallengeResponse createAndSendOtp(User user) {
        OtpDeliveryResult delivery = notificationService.sendOtp(user);
        ensureOtpDelivered(delivery);
        invalidateUnusedChallenges(user);
        OtpChallenge challenge = new OtpChallenge(
                UUID.randomUUID().toString(),
                user,
                passwordEncoder.encode(UUID.randomUUID().toString()),
                Instant.now().plus(otpExpiration)
        );
        challenge.setProviderChallengeId(delivery.providerChallengeId());
        challenge = otpChallengeRepository.save(challenge);
        return new OtpChallengeResponse(
                challenge.getChallengeId(),
                OTP_SENT_MESSAGE,
                challenge.getExpiresAt()
        );
    }

    private void ensureOtpDelivered(OtpDeliveryResult delivery) {
        if (!delivery.smsSent()) {
            String message = delivery.failureMessage() == null || delivery.failureMessage().isBlank()
                    ? "Verification code could not be sent by SMS. Check Infobip 2FA settings and try again."
                    : delivery.failureMessage();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message);
        }
    }

    private boolean isOtpValid(OtpChallenge challenge, String code) {
        if (challenge.getProviderChallengeId() != null && !challenge.getProviderChallengeId().isBlank()) {
            return notificationService.verifyOtp(challenge.getProviderChallengeId(), code);
        }
        return passwordEncoder.matches(code, challenge.getCodeHash());
    }

    private void invalidateUnusedChallenges(User user) {
        List<OtpChallenge> activeChallenges = otpChallengeRepository.findByUserAndUsedAtIsNull(user);
        Instant now = Instant.now();
        for (OtpChallenge activeChallenge : activeChallenges) {
            activeChallenge.setUsedAt(now);
        }
        if (!activeChallenges.isEmpty()) {
            otpChallengeRepository.saveAll(activeChallenges);
        }
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase().trim();
    }

    private String normalizeCellphone(String cellphoneNumber) {
        String digits = cellphoneNumber.replaceAll("[^0-9+]", "").trim();
        if (digits.startsWith("00")) {
            digits = "+" + digits.substring(2);
        }
        if (digits.startsWith("0")) {
            digits = "+27" + digits.substring(1);
        }
        if (!digits.startsWith("+") && digits.startsWith("27")) {
            digits = "+" + digits;
        }
        if (!SOUTH_AFRICAN_CELLPHONE.matcher(digits).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid South African cellphone number, for example +27821234567 or 0821234567");
        }
        return digits;
    }
}
