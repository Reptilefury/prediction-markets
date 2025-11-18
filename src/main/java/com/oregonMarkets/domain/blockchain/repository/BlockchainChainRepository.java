package com.oregonMarkets.domain.blockchain.repository;

import com.oregonMarkets.domain.blockchain.model.BlockchainChain;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface BlockchainChainRepository extends ReactiveCrudRepository<BlockchainChain, UUID> {
    
    Flux<BlockchainChain> findByIsActiveTrue();
    
    Mono<BlockchainChain> findByChainName(String chainName);
    
    Mono<BlockchainChain> findByChainId(Integer chainId);
}