package com.oregonMarkets.domain.user.repository;

import com.oregonMarkets.domain.user.model.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends ReactiveCrudRepository<User, UUID> {
    
    Mono<User> findByEmail(String email);
    
    Mono<User> findByMagicUserId(String magicUserId);
    
    Mono<User> findByMagicWalletAddress(String magicWalletAddress);
    
    Mono<User> findByReferralCode(String referralCode);
    
    @Query("SELECT COUNT(*) > 0 FROM users WHERE email = :email")
    Mono<Boolean> existsByEmail(String email);
    
    Mono<Boolean> existsByMagicUserId(String magicUserId);
    
    Mono<Boolean> existsByMagicWalletAddress(String magicWalletAddress);
    
    @Query("SELECT * FROM users WHERE email = :email AND is_active = true")
    Mono<User> findActiveByEmail(String email);
    
    Mono<User> findByWeb3WalletAddress(String walletAddress);
    
    Mono<Boolean> existsByWeb3WalletAddress(String walletAddress);
    
    @Query("SELECT * FROM users WHERE web3_wallet_address = :walletAddress AND is_active = true")
    Mono<User> findActiveByWeb3WalletAddress(String walletAddress);
}