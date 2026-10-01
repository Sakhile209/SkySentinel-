package com.skysentinel.security.auth;

public record RegistrationResponse(
        String message,
        UserDto user
) {}
