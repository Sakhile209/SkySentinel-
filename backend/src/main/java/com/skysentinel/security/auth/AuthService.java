package com.skysentinel.security.auth;

import com.skysentinel.security.config.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OtpChallengeRepository otpChallengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final OperatorNotificationService notificationService;
    private final SecureRandom secureRandom = new SecureRandom();
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
        return new RegistrationResponse("Registration successful. Sign in to receive your verification code.", UserDto.fromEntity(user));
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
        if (!passwordEncoder.matches(request.code(), challenge.getCodeHash())) {
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
        if (now.isBefore(challenge.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You can request a new verification code after the current code expires");
        }
        if (challenge.getLastSentAt() != null && now.isBefore(challenge.getLastSentAt().plus(resendCooldown))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Please wait before requesting another verification code");
        }

        String code = generateOtpCode();
        challenge.setCodeHash(passwordEncoder.encode(code));
        challenge.setExpiresAt(now.plus(otpExpiration));
        challenge.setAttempts(0);
        challenge.setResendCount(challenge.getResendCount() + 1);
        challenge.setLastSentAt(now);
        challenge = otpChallengeRepository.save(challenge);
        notificationService.sendOtp(challenge.getUser(), code);
        return new OtpChallengeResponse(challenge.getChallengeId(), "A new verification code has been sent.", challenge.getExpiresAt());
    }

    public UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return UserDto.fromEntity(user);
    }

    private OtpChallengeResponse createAndSendOtp(User user) {
        String code = generateOtpCode();
        OtpChallenge challenge = new OtpChallenge(
                UUID.randomUUID().toString(),
                user,
                passwordEncoder.encode(code),
                Instant.now().plus(otpExpiration)
        );
        challenge = otpChallengeRepository.save(challenge);
        notificationService.sendOtp(user, code);
        return new OtpChallengeResponse(challenge.getChallengeId(), "Verification code sent to your registered email and cellphone.", challenge.getExpiresAt());
    }

    private String generateOtpCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase().trim();
    }

    private String normalizeCellphone(String cellphoneNumber) {
        return cellphoneNumber.replaceAll("\\s+", "").trim();
    }
}
