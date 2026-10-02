package com.skysentinel.security.auth;

import java.time.Instant;

public record OtpChallengeResponse(
        String challengeId,
        String message,
        Instant expiresAt,
        String developmentOtp
) {}
