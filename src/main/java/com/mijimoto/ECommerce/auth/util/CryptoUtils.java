package com.mijimoto.ECommerce.auth.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;

@Component
public class CryptoUtils {

    private final SecureRandom random = new SecureRandom();
    private final String hmacSecret;

    public CryptoUtils(@Value("${app.auth.refresh-token-hmac-secret}") String hmacSecret) {
        this.hmacSecret = hmacSecret;
    }

    // Generate base64-like random string (URL-safe)
    public String generateRefreshTokenRaw(int bytes) {
        byte[] b = new byte[bytes];
        random.nextBytes(b);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    // HMAC-SHA256 hex
    public String hmacSha256Hex(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] h = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(h);
        } catch (Exception e) {
            throw new RuntimeException("Failed hmac", e);
        }
    }
}
