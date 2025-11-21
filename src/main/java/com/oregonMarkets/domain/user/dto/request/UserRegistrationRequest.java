package com.oregonMarkets.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserRegistrationRequest {
    
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;
    
    @Pattern(regexp = "^[A-Z]{2}$", message = "Invalid country code")
    private String countryCode;
    
    private String referralCode;
    
    private String utmSource;
    private String utmMedium;
    private String utmCampaign;
}