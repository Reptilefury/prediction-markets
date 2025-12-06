package com.oregonMarkets.scheduler;

import static org.mockito.Mockito.*;

import com.oregonMarkets.domain.blockchain.repository.BlockchainChainRepository;
import com.oregonMarkets.domain.blockchain.service.DepositScannerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class DepositMonitoringSchedulerTest {

  @Mock private BlockchainChainRepository chainRepository;

  @Mock private DepositScannerService depositScannerService;

  private DepositMonitoringScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler = new DepositMonitoringScheduler(chainRepository, depositScannerService);
  }

  @Test
  void scanChainsForDeposits_CallsRepositoryAndService() {
    when(chainRepository.findByIsActiveTrue()).thenReturn(Flux.empty());

    scheduler.scanChainsForDeposits();

    verify(chainRepository).findByIsActiveTrue();
  }
}
