package com.skysentinel.security.auth;

public record OtpDeliveryResult(boolean smsSent, String providerChallengeId, String failureMessage) {
    public static OtpDeliveryResult delivered(String providerChallengeId) {
        return new OtpDeliveryResult(true, providerChallengeId, null);
    }

    public static OtpDeliveryResult failed(String failureMessage) {
        return new OtpDeliveryResult(false, null, failureMessage);
    }
}
