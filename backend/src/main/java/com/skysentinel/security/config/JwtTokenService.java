package com.skysentinel.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class JwtTokenService {

    private final byte[] secretKey;
    private final long expirationMs;

    public JwtTokenService(
            @Value("${JWT_SECRET:skysentinel-super-secret-operational-key-2026-secure}") String secret,
            @Value("${JWT_EXPIRATION_MS:86400000}") long expirationMs) {
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationMs = expirationMs;
    }

    public String generateToken(Long userId, String email, String role, String fullName) {
        long now = System.currentTimeMillis();
        long exp = now + expirationMs;

        String header = base64UrlEncode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payloadJson = String.format(
                "{\"sub\":\"%s\",\"userId\":%d,\"role\":\"%s\",\"name\":\"%s\",\"exp\":%d,\"iat\":%d}",
                escapeJson(email), userId, escapeJson(role), escapeJson(fullName), exp / 1000, now / 1000
        );
        String payload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));
        String content = header + "." + payload;
        String signature = sign(content);
        return content + "." + signature;
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) return false;
        String[] parts = token.split("\\.");
        if (parts.length != 3) return false;

        String content = parts[0] + "." + parts[1];
        String expectedSig = sign(content);
        if (!MessageDigest.isEqual(expectedSig.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
            return false;
        }

        long exp = extractExp(parts[1]);
        return exp > (System.currentTimeMillis() / 1000);
    }

    public String extractEmail(String token) {
        String payloadJson = decodePayload(token);
        return extractJsonString(payloadJson, "sub");
    }

    public String extractRole(String token) {
        String payloadJson = decodePayload(token);
        return extractJsonString(payloadJson, "role");
    }

    public Long extractUserId(String token) {
        String payloadJson = decodePayload(token);
        String idStr = extractJsonField(payloadJson, "userId");
        if (idStr == null) return null;
        try {
            return Long.parseLong(idStr.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String extractName(String token) {
        String payloadJson = decodePayload(token);
        return extractJsonString(payloadJson, "name");
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error signing JWT", e);
        }
    }

    private String decodePayload(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) return "";
        byte[] bytes = Base64.getUrlDecoder().decode(parts[1]);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private long extractExp(String base64Payload) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(base64Payload);
            String json = new String(bytes, StandardCharsets.UTF_8);
            String expStr = extractJsonField(json, "exp");
            return expStr != null ? Long.parseLong(expStr.trim()) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private String extractJsonString(String json, String key) {
        String marker = "\"" + key + "\":\"";
        int idx = json.indexOf(marker);
        if (idx == -1) return null;
        int start = idx + marker.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    private String extractJsonField(String json, String key) {
        String marker = "\"" + key + "\":";
        int idx = json.indexOf(marker);
        if (idx == -1) return null;
        int start = idx + marker.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        if (end == -1) return null;
        return json.substring(start, end).replace("\"", "").trim();
    }

    private String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
