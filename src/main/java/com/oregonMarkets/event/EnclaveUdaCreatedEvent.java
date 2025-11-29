package com.oregonMarkets.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnclaveUdaCreatedEvent {
    private UUID userId;
    private String magicWalletAddress;
    private String proxyWalletAddress;
    private String enclaveUdaAddress;
    private String email;
    private String magicUserId;  // The Magic User ID (issuer)
    private String didToken;     // The DID token
    private Instant timestamp;
}
