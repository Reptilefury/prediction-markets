package com.oregonMarkets.domain.enclave.repository;

import com.oregonMarkets.domain.enclave.model.EnclaveChainAddress;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface EnclaveChainAddressRepository
    extends ReactiveCrudRepository<EnclaveChainAddress, UUID> {

  Flux<EnclaveChainAddress> findByUserId(UUID userId);

  Flux<EnclaveChainAddress> findByUserIdAndIsActive(UUID userId, Boolean isActive);

  Mono<EnclaveChainAddress> findByUserIdAndIsPrimary(UUID userId, Boolean isPrimary);

  Mono<EnclaveChainAddress> findByDepositAddress(String depositAddress);

  Flux<EnclaveChainAddress> findByChainType(EnclaveChainAddress.ChainType chainType);
}
