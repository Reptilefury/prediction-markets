package com.oregonMarkets.integration.magic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oregonMarkets.common.exception.MagicAuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Magic DID token validator using pure Java JWT validation
 * Validates Magic DID tokens without external HTTP calls or GraalVM
 *
 * Magic tokens are JWT tokens signed with Ed25519
 * They are issued by Magic.link and contain user claims
 */
@Component
@Slf4j
public class MagicDIDValidator {

    @Value("${app.magic.api-key}")
    private String magicApiKey;

    private final ObjectMapper objectMapper;

    public MagicDIDValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Validate a Magic DID token asynchronously
     * Magic tokens are in custom format: [proof, claim] where:
     *   - proof: Ethereum personal_sign signature (hex string)
     *   - claim: JSON payload as string
     *
     * Validates:
     * 1. Token format [proof, claim]
     * 2. Base64 decoding
     * 3. Claim expiration (ext) and not-before (nbf)
     * 4. Required claims (iss, sub)
     * 5. ECDSA signature recovery and verification
     * 6. Signature must match the issuer (did:ethr:0x...)
     *
     * Runs in a separate scheduler to avoid blocking the event loop
     */
    public Mono<MagicUserInfo> validateDIDToken(String didToken) {
        return Mono.fromCallable(() -> {
            log.debug("Validating Magic DID token");

            if (didToken == null || didToken.isEmpty()) {
                throw new MagicAuthException("DID token is null or empty");
            }

            log.debug("Token length: {}, First 50 chars: {}",
                didToken.length(),
                didToken.substring(0, Math.min(50, didToken.length())));

            try {
                ObjectMapper mapper = new ObjectMapper();

                // 1) Decode base64 token first (Magic tokens are base64-encoded)
                String decodedToken;
                try {
                    byte[] decodedBytes = Base64.getDecoder().decode(didToken);
                    decodedToken = new String(decodedBytes, StandardCharsets.UTF_8);
                    log.debug("Base64 decoded token length: {}, First 100 chars: {}",
                        decodedToken.length(),
                        decodedToken.substring(0, Math.min(100, decodedToken.length())));
                } catch (IllegalArgumentException e) {
                    // Token might not be base64 encoded, use as-is
                    log.debug("Token is not base64 encoded, using as-is");
                    decodedToken = didToken;
                }

                // 2) Parse Magic token format: [proof, claim]
                JsonNode tokenArray = mapper.readTree(decodedToken);

                if (!tokenArray.isArray() || tokenArray.size() < 2) {
                    throw new MagicAuthException("Invalid token format: expected array [proof, claim]");
                }

                String proof = tokenArray.get(0).asText();
                String claimJson = tokenArray.get(1).asText();

                log.debug("Token proof length: {}, Claim present: {}", proof.length(), !claimJson.isEmpty());

                // 3) Parse the claim JSON
                JsonNode claim = mapper.readTree(claimJson);

                // 4) Validate claim temporal constraints
                long nowSec = System.currentTimeMillis() / 1000L;

                if (claim.has("nbf")) {
                    long notBefore = claim.get("nbf").asLong();
                    if (nowSec < notBefore) {
                        throw new MagicAuthException("DID token is not yet valid (nbf: " + notBefore + ", now: " + nowSec + ")");
                    }
                }

                if (claim.has("ext")) {
                    long expirationTime = claim.get("ext").asLong();
                    if (nowSec > expirationTime) {
                        throw new MagicAuthException("DID token has expired (ext: " + expirationTime + ", now: " + nowSec + ")");
                    }
                }

                // 5) Extract and validate required claims
                String issuer = claim.has("iss") ? claim.get("iss").asText() : null;
                String userId = claim.has("sub") ? claim.get("sub").asText() : null;
                String publicAddress = claim.has("add") ? claim.get("add").asText() : null;

                if (userId == null || userId.isEmpty()) {
                    throw new MagicAuthException("Missing 'sub' (user ID) claim in token");
                }

                if (issuer == null || issuer.isEmpty()) {
                    throw new MagicAuthException("Missing 'iss' (issuer) claim in token");
                }

                if (!issuer.startsWith("did:ethr:")) {
                    throw new MagicAuthException("Invalid issuer format: must start with 'did:ethr:', got: " + issuer);
                }

                // 6) Recover Ethereum address from ECDSA signature
                String recoveredAddress = recoverAddressFromSignature(proof, claimJson);

                // 7) Verify recovered address matches issuer
                String expectedAddress = issuer.substring("did:ethr:".length()).toLowerCase();
                String recoveredAddressLower = recoveredAddress.toLowerCase();

                if (!recoveredAddressLower.equals(expectedAddress)) {
                    throw new MagicAuthException(
                        "Signature verification failed: recovered address does not match issuer. " +
                        "Expected: " + expectedAddress + ", Got: " + recoveredAddressLower
                    );
                }

                log.info("Magic DID token validated successfully for user: {} (issuer: {}, address: {})",
                    userId, issuer, recoveredAddress);

                MagicUserInfo userInfo = new MagicUserInfo();
                userInfo.setIssuer(issuer);
                userInfo.setEmail(null); // Magic tokens don't include email
                userInfo.setPublicAddress(recoveredAddress);  // Use recovered address, not token claim
                userInfo.setPhone(null);
                userInfo.setUserId(userId);

                return userInfo;

            } catch (IOException e) {
                log.error("Failed to parse DID token", e);
                throw new MagicAuthException("Invalid token format", e);
            } catch (MagicAuthException e) {
                throw e;
            } catch (Exception e) {
                log.error("DID token validation error", e);
                throw new MagicAuthException("Token validation failed", e);
            }
        })
        // Run on a separate scheduler to prevent blocking the reactor thread
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(error -> log.error("Magic DID token validation failed", error));
    }

    /**
     * Recover Ethereum address from personal_sign signature
     * The signature is expected to be a hex string from Ethereum personal_sign
     *
     * @param proof signature hex string (with or without 0x prefix)
     * @param message the original message that was signed (claim JSON)
     * @return recovered Ethereum address in 0x format (lowercase)
     */
    private String recoverAddressFromSignature(String proof, String message) {
        try {
            // Normalize hex string (add 0x if missing)
            String hexProof = proof.startsWith("0x") ? proof : "0x" + proof;

            // Convert hex to bytes
            byte[] sigBytes = Numeric.hexStringToByteArray(hexProof);

            if (sigBytes.length != 65) {
                throw new MagicAuthException("Invalid signature length: expected 65 bytes, got " + sigBytes.length);
            }

            // Extract v, r, s from signature bytes
            // v is the last byte, r is first 32 bytes, s is next 32 bytes
            byte v = sigBytes[64];
            if (v < 27) {
                v = (byte) (v + 27); // Normalize v to 27/28 if necessary
            }

            byte[] r = new byte[32];
            byte[] s = new byte[32];
            System.arraycopy(sigBytes, 0, r, 0, 32);
            System.arraycopy(sigBytes, 32, s, 0, 32);

            Sign.SignatureData signatureData = new Sign.SignatureData(v, r, s);

            // Recover public key using Ethereum personal_sign (EIP-191)
            // The message is the claim JSON string
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

            log.debug("Recovering address from signature. Message length: {}, Signature: {}",
                messageBytes.length, hexProof.substring(0, Math.min(20, hexProof.length())) + "...");

            BigInteger publicKey = Sign.signedPrefixedMessageToKey(messageBytes, signatureData);
            String recoveredAddress = "0x" + Keys.getAddress(publicKey);

            log.debug("Successfully recovered address: {}", recoveredAddress);
            return recoveredAddress;

        } catch (Exception e) {
            log.error("Failed to recover address from signature", e);
            throw new MagicAuthException("Invalid token signature", e);
        }
    }

    /**
     * DTO for Magic user info extracted from DID token
     */
    public static class MagicUserInfo {
        private String issuer;
        private String email;
        private String publicAddress;
        private String phone;
        private String userId;

        public MagicUserInfo() {}

        public MagicUserInfo(String issuer, String email, String publicAddress, String phone, String userId) {
            this.issuer = issuer;
            this.email = email;
            this.publicAddress = publicAddress;
            this.phone = phone;
            this.userId = userId;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPublicAddress() {
            return publicAddress;
        }

        public void setPublicAddress(String publicAddress) {
            this.publicAddress = publicAddress;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        @Override
        public String toString() {
            return "MagicUserInfo{" +
                    "issuer='" + issuer + '\'' +
                    ", email='" + email + '\'' +
                    ", publicAddress='" + publicAddress + '\'' +
                    ", phone='" + phone + '\'' +
                    ", userId='" + userId + '\'' +
                    '}';
        }
    }
}
