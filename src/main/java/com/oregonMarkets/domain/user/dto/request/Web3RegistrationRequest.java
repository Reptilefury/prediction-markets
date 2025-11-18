package com.oregonMarkets.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class Web3RegistrationRequest {
    
    @NotBlank(message = "Wallet address is required")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "Invalid wallet address format")
    private String walletAddress;
    
    @NotBlank(message = "Signature is required")
    private String signature;
    
    @NotBlank(message = "Message is required")
    private String message;
    
    @Pattern(regexp = "^[A-Z]{2}$", message = "Invalid country code")
    private String countryCode;
    
    private String referralCode;
    
    private String utmSource;
    private String utmMedium;
    private String utmCampaign;
}