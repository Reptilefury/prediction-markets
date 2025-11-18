package com.oregonMarkets.domain.payment.repository;

import com.oregonMarkets.domain.payment.model.Deposit;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface DepositRepository extends ReactiveCrudRepository<Deposit, UUID> {
    
    Flux<Deposit> findByUserId(UUID userId);
    
    Flux<Deposit> findByUserIdAndStatus(UUID userId, Deposit.DepositStatus status);
    
    Mono<Deposit> findByTxHash(String txHash);
    
    Flux<Deposit> findByStatus(Deposit.DepositStatus status);
    
    Flux<Deposit> findByProcessingStatus(Deposit.ProcessingStatus status);
}