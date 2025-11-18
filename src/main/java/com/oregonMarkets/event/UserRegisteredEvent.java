package com.oregonMarkets.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {
    
    private UUID userId;
    private String email;
    private String magicWalletAddress;
    private String enclaveUdaAddress;
    private String referralCode;
    private UUID referredByUserId;
    private Instant timestamp;
    
    public static UserRegisteredEvent from(UUID userId, String email, String magicWalletAddress, 
                                         String enclaveUdaAddress, String referralCode, 
                                         UUID referredByUserId) {
        return new UserRegisteredEvent(
            userId, email, magicWalletAddress, enclaveUdaAddress, 
            referralCode, referredByUserId, Instant.now()
        );
    }
}