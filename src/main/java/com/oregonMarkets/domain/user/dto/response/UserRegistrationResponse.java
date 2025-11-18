package com.oregonMarkets.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationResponse {
    
    private UUID userId;
    private String email;
    private String username;
    private String magicWalletAddress;
    private String enclaveUdaAddress;
    private String referralCode;
    private String accessToken;
    private String refreshToken;
    private Instant createdAt;
}