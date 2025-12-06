package com.oregonMarkets.domain.blockchain.service;

import com.oregonMarkets.domain.blockchain.model.BlockchainChain;
import com.oregonMarkets.domain.blockchain.repository.BlockchainChainRepository;
import com.oregonMarkets.domain.enclave.repository.EnclaveChainAddressRepository;
import com.oregonMarkets.domain.payment.repository.DepositRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepositScannerServiceTest {

    @Mock
    private BlockchainChainRepository chainRepository;
    
    @Mock
    private EnclaveChainAddressRepository addressRepository;
    
    @Mock
    private DepositRepository depositRepository;

    private DepositScannerService depositScannerService;

    @BeforeEach
    void setUp() {
        depositScannerService = new DepositScannerService(
            chainRepository, 
            addressRepository, 
            depositRepository
        );
    }

    @Test
    void scanChainForDeposits_ChainNotFound_CompletesEmpty() {
        UUID chainId = UUID.randomUUID();
        when(chainRepository.findById(chainId)).thenReturn(Mono.empty());

        StepVerifier.create(depositScannerService.scanChainForDeposits(chainId))
            .verifyComplete();
    }

    @Test
    void scanChainForDeposits_ChainInactive_CompletesEmpty() {
        UUID chainId = UUID.randomUUID();
        BlockchainChain inactiveChain = BlockchainChain.builder()
            .id(chainId)
            .chainName("ethereum")
            .isActive(false)
            .build();

        when(chainRepository.findById(chainId)).thenReturn(Mono.just(inactiveChain));

        StepVerifier.create(depositScannerService.scanChainForDeposits(chainId))
            .verifyComplete();
    }

    @Test
    void scanChainForDeposits_ActiveChain_ProcessesSuccessfully() {
        UUID chainId = UUID.randomUUID();
        BlockchainChain activeChain = BlockchainChain.builder()
            .id(chainId)
            .chainName("ethereum")
            .chainId(1)
            .rpcUrl("https://mainnet.infura.io/v3/test")
            .usdcTokenAddress("0xA0b86a33E6441c8C06DD2c2b4b2B3c4B2B3c4B2B")
            .usdcDecimals(6)
            .requiredConfirmations(12)
            .lastScannedBlock(18000000L)
            .isActive(true)
            .build();

        when(chainRepository.findById(chainId)).thenReturn(Mono.just(activeChain));

        StepVerifier.create(depositScannerService.scanChainForDeposits(chainId))
            .expectError()
            .verify();
    }

    @Test
    void scanChainForDeposits_NullChainId_CompletesEmpty() {
        UUID nullId = null;
        when(chainRepository.findById(nullId)).thenReturn(Mono.empty());

        StepVerifier.create(depositScannerService.scanChainForDeposits(nullId))
            .verifyComplete();
    }
}