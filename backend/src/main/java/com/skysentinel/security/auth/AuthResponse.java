package com.skysentinel.security.auth;

public record AuthResponse(
        String token,
        UserDto user
) {}
