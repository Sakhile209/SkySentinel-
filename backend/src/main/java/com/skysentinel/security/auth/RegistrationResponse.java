package com.skysentinel.security.auth;

import java.time.Instant;

public record RegistrationResponse(
        String message,
        UserDto user,
        String challengeId,
        Instant expiresAt
) {}
