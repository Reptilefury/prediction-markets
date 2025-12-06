package com.oregonMarkets.integration.web3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.regex.Pattern;

@Service
@Slf4j
public class Web3AuthService {
    
    private static final Pattern WALLET_ADDRESS_PATTERN = Pattern.compile("^0x[a-fA-F0-9]{40}$");
    private static final String EXPECTED_MESSAGE_PREFIX = "Sign this message to authenticate with Oregon Markets";
    
    public Mono<Boolean> verifySignature(String walletAddress, String message, String signature) {
        return Mono.fromCallable(() -> {
            if (!WALLET_ADDRESS_PATTERN.matcher(walletAddress).matches()) {
                log.warn("Invalid wallet address format: {}", walletAddress);
                return false;
            }
            
            if (!message.startsWith(EXPECTED_MESSAGE_PREFIX)) {
                log.warn("Invalid message format for wallet: {}", walletAddress);
                return false;
            }
            
            boolean isValid = signature != null && 
                            signature.startsWith("0x") && 
                            signature.length() == 132;
            
            log.info("Signature verification for wallet {}: {}", walletAddress, isValid);
            return isValid;
        })
        .doOnError(error -> log.error("Error verifying signature for wallet {}: {}", 
                                    walletAddress, error.getMessage()));
    }
    
    public String generateAuthMessage(String walletAddress) {
        long timestamp = System.currentTimeMillis();
        return String.format("%s%n%nWallet: %s%nTimestamp: %d", 
                           EXPECTED_MESSAGE_PREFIX, walletAddress, timestamp);
    }
}