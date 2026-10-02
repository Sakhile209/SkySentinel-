package com.skysentinel.security.auth;

public record OtpDeliveryResult(boolean emailSent, boolean smsSent) {
    public boolean fullyDelivered() {
        return emailSent && smsSent;
    }

    public boolean partiallyDelivered() {
        return emailSent || smsSent;
    }
}
