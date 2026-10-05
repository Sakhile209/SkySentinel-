package com.skysentinel.security.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Verification challenge is required")
        String challengeId,

        @NotBlank(message = "OTP code is required")
        String code,

        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String newPassword
) {}
