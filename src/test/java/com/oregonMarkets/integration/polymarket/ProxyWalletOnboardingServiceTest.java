package com.oregonMarkets.integration.polymarket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.web3j.protocol.Web3j;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProxyWalletOnboardingServiceTest {

    @Mock
    private Web3j web3j;

    private ProxyWalletOnboardingService service;

    @BeforeEach
    void setUp() {
        service = new ProxyWalletOnboardingService(web3j);
        ReflectionTestUtils.setField(service, "proxyFactoryAddress", "0xaB45c5A4B0c941a2F231C04C3f49182e1A254052");
    }

    @Test
    void createUserProxyWallet_Success() {
        String userEOA = "0x742d35Cc6634C0532925a3b8D400E4C0532925a3";

        StepVerifier.create(service.createUserProxyWallet(userEOA))
            .expectNextMatches(address -> address.startsWith("0x") && address.length() == 42)
            .verifyComplete();
    }

    @Test
    void createUserProxyWallet_NullAddress() {
        StepVerifier.create(service.createUserProxyWallet(null))
            .expectError(IllegalArgumentException.class)
            .verify();
    }

    @Test
    void createUserProxyWallet_EmptyAddress() {
        StepVerifier.create(service.createUserProxyWallet(""))
            .expectError(IllegalArgumentException.class)
            .verify();
    }

    @Test
    void isValidProxyWalletAddress_Valid() {
        String validAddress = "0x742d35Cc6634C0532925a3b8D400E4C0532925a3";

        StepVerifier.create(service.isValidProxyWalletAddress(validAddress))
            .expectNext(true)
            .verifyComplete();
    }

    @Test
    void isValidProxyWalletAddress_Invalid() {
        StepVerifier.create(service.isValidProxyWalletAddress("invalid"))
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    void isValidProxyWalletAddress_Null() {
        StepVerifier.create(service.isValidProxyWalletAddress(null))
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    void getProxyWalletCreationInfo() {
        String info = service.getProxyWalletCreationInfo();
        
        assertTrue(info.contains("Proxy wallet"));
        assertTrue(info.contains("automatically created"));
    }
}