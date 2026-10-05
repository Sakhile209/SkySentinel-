package com.skysentinel.security.auth;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OperatorNotificationService {
    private static final Logger log = LoggerFactory.getLogger(OperatorNotificationService.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String baseUrl;
    private final String applicationId;
    private final String messageId;

    public OperatorNotificationService(
            RestClient.Builder restClientBuilder,
            @Value("${skysentinel.infobip.api-key:}") String apiKey,
            @Value("${skysentinel.infobip.base-url:}") String baseUrl,
            @Value("${skysentinel.infobip.two-factor.application-id:}") String applicationId,
            @Value("${skysentinel.infobip.two-factor.message-id:}") String messageId) {
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.applicationId = applicationId;
        this.messageId = messageId;
    }

    public void sendRegistrationConfirmation(User user) {
        log.info("SkySentinel operator account registered for {}", user.getEmail());
    }

    public OtpDeliveryResult sendOtp(User user) {
        if (isLocalDevelopmentMode()) {
            log.info("Infobip 2FA not configured; using local-development OTP fallback for {}", user.getCellphoneNumber());
            return OtpDeliveryResult.delivered("local-dev-pin");
        }

        String configurationProblem = configurationProblem();
        if (configurationProblem != null) {
            log.warn(configurationProblem);
            return OtpDeliveryResult.failed(configurationProblem);
        }

        Map<String, String> body = new LinkedHashMap<>();
        body.put("applicationId", applicationId);
        body.put("messageId", messageId);
        body.put("to", user.getCellphoneNumber());

        try {
            JsonNode response = restClient.post()
                    .uri(normalizedBaseUrl() + "/2fa/2/pin")
                    .header(HttpHeaders.AUTHORIZATION, "App " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String pinId = response == null || response.path("pinId").isMissingNode()
                    ? null
                    : response.path("pinId").asText();
            if (pinId == null || pinId.isBlank()) {
                log.warn("Infobip 2FA SMS delivery response did not include a pinId for {}", user.getCellphoneNumber());
                return OtpDeliveryResult.failed("Verification code could not be sent by SMS. Check Infobip 2FA response configuration and try again.");
            }
            return OtpDeliveryResult.delivered(pinId);
        } catch (RestClientResponseException ex) {
            log.warn("Infobip 2FA SMS delivery failed for {} with status {}", user.getCellphoneNumber(), ex.getStatusCode().value());
            return OtpDeliveryResult.failed(infobipSendFailureMessage(ex.getStatusCode().value()));
        } catch (RuntimeException ex) {
            log.warn("Infobip 2FA SMS delivery failed for {}", user.getCellphoneNumber(), ex);
            return OtpDeliveryResult.failed("Verification code could not be sent by SMS. Check Infobip connectivity and try again.");
        }
    }

    public boolean verifyOtp(String providerChallengeId, String code) {
        if ("local-dev-pin".equals(providerChallengeId) && "123456".equals(code)) {
            return true;
        }

        String configurationProblem = configurationProblem();
        if (configurationProblem != null) {
            log.warn(configurationProblem);
            return false;
        }

        try {
            JsonNode response = restClient.post()
                    .uri(normalizedBaseUrl() + "/2fa/2/pin/{pinId}/verify", providerChallengeId)
                    .header(HttpHeaders.AUTHORIZATION, "App " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Map.of("pin", code))
                    .retrieve()
                    .body(JsonNode.class);
            return response != null && response.path("verified").asBoolean(false);
        } catch (RestClientResponseException ex) {
            log.warn("Infobip 2FA OTP verification failed with status {}", ex.getStatusCode().value());
            return false;
        } catch (RuntimeException ex) {
            log.warn("Infobip 2FA OTP verification failed", ex);
            return false;
        }
    }

    private boolean isLocalDevelopmentMode() {
        return !hasText(apiKey) && !hasText(baseUrl) && !hasText(applicationId) && !hasText(messageId);
    }

    private String configurationProblem() {
        if (!hasText(apiKey)) {
            return "Infobip 2FA is not configured. Set INFOBIP_API_KEY in .env and restart the backend.";
        }
        if (!hasText(baseUrl)) {
            return "Infobip 2FA is not configured. Set INFOBIP_BASE_URL in .env and restart the backend.";
        }
        if (!hasText(applicationId)) {
            return "Infobip 2FA is not configured. Set INFOBIP_2FA_APPLICATION_ID in .env and restart the backend.";
        }
        if (!hasText(messageId)) {
            return "Infobip 2FA is not configured. Set INFOBIP_2FA_MESSAGE_ID in .env and restart the backend.";
        }
        return null;
    }

    private String infobipSendFailureMessage(int statusCode) {
        return switch (statusCode) {
            case 401, 403 -> "Infobip rejected the SMS request. Check INFOBIP_API_KEY permissions and account access.";
            case 404 -> "Infobip 2FA application or message template was not found. Check INFOBIP_2FA_APPLICATION_ID and INFOBIP_2FA_MESSAGE_ID.";
            case 429 -> "Infobip SMS rate limit reached. Wait before requesting another OTP or check your Infobip quota and throttling settings.";
            default -> "Verification code could not be sent by SMS. Check Infobip credentials, application, message template, and phone number.";
        };
    }

    private String normalizedBaseUrl() {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
