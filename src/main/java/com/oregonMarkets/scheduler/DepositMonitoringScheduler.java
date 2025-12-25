package com.oregonmarkets.scheduler;

import com.oregonmarkets.domain.blockchain.repository.BlockchainChainRepository;
import com.oregonmarkets.domain.blockchain.service.DepositScannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class DepositMonitoringScheduler {

  private final BlockchainChainRepository chainRepository;
  private final DepositScannerService scannerService;

  // @Scheduled(fixedDelay = 30000, initialDelay = 10000)
  public void scanChainsForDeposits() {
    log.debug("Starting deposit monitoring scan");

    chainRepository
        .findByIsActiveTrue()
        .flatMap(
            chain ->
                scannerService
                    .scanChainForDeposits(chain.getId())
                    .onErrorResume(
                        error -> {
                          log.error(
                              "Error scanning chain {}: {}",
                              chain.getChainName(),
                              error.getMessage());
                          return reactor.core.publisher.Mono.empty();
                        }))
        .subscribe();
  }
}
