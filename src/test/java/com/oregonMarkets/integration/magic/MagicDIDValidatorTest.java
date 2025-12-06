package com.oregonMarkets.integration.magic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oregonMarkets.common.exception.MagicAuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;

import java.util.Base64;

class MagicDIDValidatorTest {

    private MagicDIDValidator validator;

    @BeforeEach
    void setUp() {
        validator = new MagicDIDValidator(new ObjectMapper());
        ReflectionTestUtils.setField(validator, "magicApiKey", "test-api-key");
    }

    @Test
    void validateDIDToken_NullToken() {
        StepVerifier.create(validator.validateDIDToken(null))
            .expectError(MagicAuthException.class)
            .verify();
    }

    @Test
    void validateDIDToken_EmptyToken() {
        StepVerifier.create(validator.validateDIDToken(""))
            .expectError(MagicAuthException.class)
            .verify();
    }

    @Test
    void validateDIDToken_InvalidFormat() {
        String invalidToken = Base64.getEncoder().encodeToString("invalid-json".getBytes());
        
        StepVerifier.create(validator.validateDIDToken(invalidToken))
            .expectError(MagicAuthException.class)
            .verify();
    }

    @Test
    void validateDIDToken_InvalidArrayFormat() {
        String invalidArray = Base64.getEncoder().encodeToString("[\"only-one-element\"]".getBytes());
        
        StepVerifier.create(validator.validateDIDToken(invalidArray))
            .expectError(MagicAuthException.class)
            .verify();
    }

    @Test
    void validateDIDToken_ExpiredToken() {
        long expiredTime = System.currentTimeMillis() / 1000L - 3600; // 1 hour ago
        String claim = String.format("{\"iss\":\"did:ethr:0x123\",\"sub\":\"user123\",\"ext\":%d}", expiredTime);
        String tokenArray = String.format("[\"%s\",\"%s\"]", "proof", claim);
        String token = Base64.getEncoder().encodeToString(tokenArray.getBytes());
        
        StepVerifier.create(validator.validateDIDToken(token))
            .expectError(MagicAuthException.class)
            .verify();
    }

    @Test
    void validateDIDToken_NotYetValid() {
        long futureTime = System.currentTimeMillis() / 1000L + 3600; // 1 hour from now
        String claim = String.format("{\"iss\":\"did:ethr:0x123\",\"sub\":\"user123\",\"nbf\":%d}", futureTime);
        String tokenArray = String.format("[\"%s\",\"%s\"]", "proof", claim);
        String token = Base64.getEncoder().encodeToString(tokenArray.getBytes());
        
        StepVerifier.create(validator.validateDIDToken(token))
            .expectError(MagicAuthException.class)
            .verify();
    }

    @Test
    void validateDIDToken_MissingSubject() {
        String claim = "{\"iss\":\"did:ethr:0x123\"}"; // Missing sub
        String tokenArray = String.format("[\"%s\",\"%s\"]", "proof", claim);
        String token = Base64.getEncoder().encodeToString(tokenArray.getBytes());
        
        StepVerifier.create(validator.validateDIDToken(token))
            .expectError(MagicAuthException.class)
            .verify();
    }

    @Test
    void validateDIDToken_MissingIssuer() {
        String claim = "{\"sub\":\"user123\"}"; // Missing iss
        String tokenArray = String.format("[\"%s\",\"%s\"]", "proof", claim);
        String token = Base64.getEncoder().encodeToString(tokenArray.getBytes());
        
        StepVerifier.create(validator.validateDIDToken(token))
            .expectError(MagicAuthException.class)
            .verify();
    }

    @Test
    void validateDIDToken_InvalidIssuerFormat() {
        String claim = "{\"iss\":\"invalid-issuer\",\"sub\":\"user123\"}";
        String tokenArray = String.format("[\"%s\",\"%s\"]", "proof", claim);
        String token = Base64.getEncoder().encodeToString(tokenArray.getBytes());
        
        StepVerifier.create(validator.validateDIDToken(token))
            .expectError(MagicAuthException.class)
            .verify();
    }

    @Test
    void validateDIDToken_InvalidSignatureLength() {
        String claim = "{\"iss\":\"did:ethr:0x123\",\"sub\":\"user123\"}";
        String shortProof = "0x123"; // Too short signature
        String tokenArray = String.format("[\"%s\",\"%s\"]", shortProof, claim);
        String token = Base64.getEncoder().encodeToString(tokenArray.getBytes());
        
        StepVerifier.create(validator.validateDIDToken(token))
            .expectError(MagicAuthException.class)
            .verify();
    }
}