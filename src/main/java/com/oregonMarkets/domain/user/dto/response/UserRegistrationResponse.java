package com.oregonMarkets.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserRegistrationResponse {

    private UUID userId;
    private String email;
    private String username;
    private String magicWalletAddress;
    private String enclaveUdaAddress;
    private String proxyWalletAddress;
    private DepositAddresses depositAddresses;
    private String referralCode;
    private String accessToken;
    private String refreshToken;
    private Instant createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DepositAddresses {
        private List<EVMDepositAddress> evmDepositAddress;
        private SolanaDepositAddress solanaDepositAddress;
        private BitcoinDepositAddress bitcoinDepositAddress;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EVMDepositAddress {
        private Integer chainId;
        private String contractAddress;
        private Boolean deployed;
        private String id;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SolanaDepositAddress {
        private String address;
        private String id;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BitcoinDepositAddress {
        private String legacyAddress;
        private String segwitAddress;
        private String nativeSegwitAddress;
        private String taprootAddress;
        private String id;
    }
}