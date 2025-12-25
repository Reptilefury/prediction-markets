package com.oregonmarkets.domain.blockchain.repository;

import com.oregonmarkets.domain.blockchain.model.BlockchainChain;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface BlockchainChainRepository extends ReactiveCrudRepository<BlockchainChain, UUID> {

  Flux<BlockchainChain> findByIsActiveTrue();

  Mono<BlockchainChain> findByChainName(String chainName);

  Mono<BlockchainChain> findByChainId(Integer chainId);
}
