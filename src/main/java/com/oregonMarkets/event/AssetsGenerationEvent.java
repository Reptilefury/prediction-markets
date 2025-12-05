package com.oregonMarkets.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetsGenerationEvent {
    private UUID userId;
    private String email;
    private String proxyWalletAddress;
    private String enclaveUdaAddress;
    private String magicWalletAddress;
    private Map<String, Object> depositAddresses; // From Enclave response
    private Instant timestamp;
}