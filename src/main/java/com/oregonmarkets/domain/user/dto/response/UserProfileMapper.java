package com.oregonmarkets.domain.user.dto.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oregonmarkets.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserProfileMapper {

    private final ObjectMapper objectMapper;

    public UserRegistrationResponse toResponse(User user) {
        return UserRegistrationResponse.builder()
                // Basic Profile
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                
                // Magic.link Integration
                .magicWalletAddress(user.getMagicWalletAddress())
                .magicIssuer(user.getMagicIssuer())
                
                // Web3 Wallet Integration
                .web3WalletAddress(user.getWeb3WalletAddress())
                .authMethod(user.getAuthMethod() != null ? user.getAuthMethod().name() : null)
                .walletVerifiedAt(user.getWalletVerifiedAt())
                
                // Enclave UDA Integration
                .enclaveUserId(user.getEnclaveUserId())
                .enclaveUdaAddress(user.getEnclaveUdaAddress())
                .enclaveUdaTag(user.getEnclaveUdaTag())
                .enclaveUdaCreatedAt(user.getEnclaveUdaCreatedAt())
                .enclaveUdaStatus(user.getEnclaveUdaStatus() != null ? user.getEnclaveUdaStatus().name() : null)
                .enclaveDepositAddresses(parseJsonString(user.getEnclaveDepositAddresses()))
                
                // Account Status
                .countryCode(user.getCountryCode())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .emailVerifiedAt(user.getEmailVerifiedAt())
                
                // KYC
                .kycStatus(user.getKycStatus() != null ? user.getKycStatus().name() : null)
                .kycLevel(user.getKycLevel())
                
                // Trading Limits
                .dailyDepositLimit(user.getDailyDepositLimit() != null ? user.getDailyDepositLimit().toString() : null)
                .dailyWithdrawalLimit(user.getDailyWithdrawalLimit() != null ? user.getDailyWithdrawalLimit().toString() : null)
                
                // Blnk Integration
                .blnkIdentityId(user.getBlnkIdentityId())
                .blnkBalanceId(user.getBlnkBalanceId())
                .blnkCreatedAt(user.getBlnkCreatedAt())
                
                // Polymarket Proxy Wallet Integration
                .proxyWalletAddress(user.getProxyWalletAddress())
                .proxyWalletCreatedAt(user.getProxyWalletCreatedAt())
                .proxyWalletStatus(user.getProxyWalletStatus() != null ? user.getProxyWalletStatus().name() : null)
                
                // Biconomy Smart Account Integration
                .biconomySmartAccountAddress(user.getBiconomySmartAccountAddress())
                .biconomyDeployed(user.getBiconomyDeployed())
                .biconomyChainId(user.getBiconomyChainId())
                .biconomyBundlerUrl(user.getBiconomyBundlerUrl())
                .biconomyPaymasterUrl(user.getBiconomyPaymasterUrl())
                .biconomyCreatedAt(user.getBiconomyCreatedAt())
                
                // Referral
                .referralCode(user.getReferralCode())
                .referredByUserId(user.getReferredByUserId())
                .utmSource(user.getUtmSource())
                .utmMedium(user.getUtmMedium())
                .utmCampaign(user.getUtmCampaign())
                
                // Avatar and QR Codes
                .avatarUrl(user.getAvatarUrl())
                .proxyWalletQrCodeUrl(user.getProxyWalletQrCodeUrl())
                .enclaveUdaQrCodeUrl(user.getEnclaveUdaQrCodeUrl())
                .evmDepositQrCodes(parseJsonString(user.getEvmDepositQrCodes()))
                .solanaDepositQrCodeUrl(user.getSolanaDepositQrCodeUrl())
                .bitcoinDepositQrCodes(parseJsonString(user.getBitcoinDepositQrCodes()))
                .build();
    }

    private Object parseJsonString(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        try {
            // First try parsing as JSON
            return objectMapper.readValue(jsonString, Object.class);
        } catch (JsonProcessingException e) {
            // If JSON parsing fails, try parsing Java Map toString format
            try {
                return parseMapString(jsonString);
            } catch (Exception ex) {
                log.warn("Failed to parse string as JSON or Map format: {}", jsonString);
                return jsonString; // Return original string if all parsing fails
            }
        }
    }

    private Object parseMapString(String mapString) {
        if (!mapString.startsWith("{") || !mapString.endsWith("}")) {
            return mapString;
        }
        
        java.util.Map<String, String> result = new java.util.HashMap<>();
        String content = mapString.substring(1, mapString.length() - 1);
        
        if (content.trim().isEmpty()) {
            return result;
        }
        
        String[] pairs = content.split(", ");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                result.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        
        return result;
    }
}
