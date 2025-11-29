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
public class BlnkBalanceCreatedEvent {
    private UUID userId;
    private String magicWalletAddress;
    private String proxyWalletAddress;
    private String enclaveUdaAddress;
    private String blnkIdentityId;
    private String blnkBalanceId;
    private String email;
    private String magicUserId;  // The decoded Magic User ID (e.g., CQ0U5PbimduW29o9eRLqysxJPSAhPT__DiXK6D2RPd0)
    private String didToken;     // The DID token for password
    private Instant timestamp;
}
