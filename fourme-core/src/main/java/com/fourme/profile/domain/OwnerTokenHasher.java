package com.fourme.profile.domain;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OwnerTokenHasher {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final byte[] secret;

    public OwnerTokenHasher(
            @Value("${fourme.owner-token.hmac-secret:local-fourme-owner-token-secret}") String secret
    ) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String hash(String ownerToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret, HMAC_SHA256));
            byte[] digest = mac.doFinal(ownerToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to hash owner token", exception);
        }
    }
}
