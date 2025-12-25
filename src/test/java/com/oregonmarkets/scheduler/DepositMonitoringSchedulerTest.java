package com.oregonmarkets.scheduler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.oregonmarkets.domain.blockchain.model.BlockchainChain;
import com.oregonmarkets.domain.blockchain.repository.BlockchainChainRepository;
import com.oregonmarkets.domain.blockchain.service.DepositScannerService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class DepositMonitoringSchedulerTest {

  @Mock private BlockchainChainRepository chainRepository;

  @Mock private DepositScannerService scannerService;

  @InjectMocks private DepositMonitoringScheduler scheduler;

  @Test
  void constructor_WithValidDependencies_CreatesInstance() {
    assertNotNull(scheduler);
  }

  @Test
  void scanChainsForDeposits_WithActiveChains_ShouldScanAll() {
    var chain1 = new BlockchainChain();
    chain1.setId(UUID.randomUUID());
    chain1.setChainName("Ethereum");
    chain1.setIsActive(true);

    var chain2 = new BlockchainChain();
    chain2.setId(UUID.randomUUID());
    chain2.setChainName("Polygon");
    chain2.setIsActive(true);

    when(chainRepository.findByIsActiveTrue()).thenReturn(Flux.just(chain1, chain2));
    when(scannerService.scanChainForDeposits(any())).thenReturn(Mono.empty());

    scheduler.scanChainsForDeposits();

    verify(scannerService, times(2)).scanChainForDeposits(any());
  }

  @Test
  void scanChainsForDeposits_WithScannerError_ShouldContinue() {
    var chain = new BlockchainChain();
    chain.setId(UUID.randomUUID());
    chain.setChainName("Ethereum");
    chain.setIsActive(true);

    when(chainRepository.findByIsActiveTrue()).thenReturn(Flux.just(chain));
    when(scannerService.scanChainForDeposits(any()))
        .thenReturn(Mono.error(new RuntimeException("Scanner error")));

    scheduler.scanChainsForDeposits();

    verify(scannerService).scanChainForDeposits(any());
  }

  @Test
  void scanChainsForDeposits_WithNoActiveChains_ShouldComplete() {
    when(chainRepository.findByIsActiveTrue()).thenReturn(Flux.empty());

    scheduler.scanChainsForDeposits();

    verify(scannerService, never()).scanChainForDeposits(any());
  }
}
