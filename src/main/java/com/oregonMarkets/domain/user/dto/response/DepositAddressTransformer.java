package com.oregonMarkets.domain.user.dto.response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DepositAddressTransformer {

    public static UserRegistrationResponse.DepositAddresses transform(Map<String, Object> rawDepositAddresses) {
        if (rawDepositAddresses == null) {
            return null;
        }

        List<UserRegistrationResponse.EVMDepositAddress> evmAddresses = transformEVMAddresses(
                (List<Map<String, Object>>) rawDepositAddresses.get("evm_deposit_address")
        );

        UserRegistrationResponse.SolanaDepositAddress solanaAddress = transformSolanaAddress(
                (Map<String, Object>) rawDepositAddresses.get("solana_deposit_address")
        );

        UserRegistrationResponse.BitcoinDepositAddress bitcoinAddress = transformBitcoinAddress(
                (Map<String, Object>) rawDepositAddresses.get("bitcoin_deposit_address")
        );

        return UserRegistrationResponse.DepositAddresses.builder()
                .evmDepositAddress(evmAddresses)
                .solanaDepositAddress(solanaAddress)
                .bitcoinDepositAddress(bitcoinAddress)
                .build();
    }

    private static List<UserRegistrationResponse.EVMDepositAddress> transformEVMAddresses(
            List<Map<String, Object>> rawList) {
        if (rawList == null) {
            return null;
        }

        List<UserRegistrationResponse.EVMDepositAddress> evmAddresses = new ArrayList<>();
        for (Map<String, Object> item : rawList) {
            evmAddresses.add(UserRegistrationResponse.EVMDepositAddress.builder()
                    .chainId(((Number) item.get("chainId")).intValue())
                    .contractAddress((String) item.get("contractAddress"))
                    .deployed((Boolean) item.get("deployed"))
                    .id((String) item.get("_id"))
                    .build());
        }
        return evmAddresses;
    }

    private static UserRegistrationResponse.SolanaDepositAddress transformSolanaAddress(
            Map<String, Object> rawAddress) {
        if (rawAddress == null) {
            return null;
        }

        return UserRegistrationResponse.SolanaDepositAddress.builder()
                .address((String) rawAddress.get("address"))
                .id((String) rawAddress.get("_id"))
                .build();
    }

    private static UserRegistrationResponse.BitcoinDepositAddress transformBitcoinAddress(
            Map<String, Object> rawAddress) {
        if (rawAddress == null) {
            return null;
        }

        return UserRegistrationResponse.BitcoinDepositAddress.builder()
                .legacyAddress((String) rawAddress.get("legacy_address"))
                .segwitAddress((String) rawAddress.get("segwit_address"))
                .nativeSegwitAddress((String) rawAddress.get("native_segwit_address"))
                .taprootAddress((String) rawAddress.get("taproot_address"))
                .id((String) rawAddress.get("_id"))
                .build();
    }
}
