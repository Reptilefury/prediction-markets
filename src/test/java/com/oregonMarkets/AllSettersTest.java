package com.oregonmarkets;

import static org.junit.jupiter.api.Assertions.*;

import com.oregonmarkets.domain.blockchain.model.BlockchainChain;
import com.oregonmarkets.domain.enclave.model.EnclaveChainAddress;
import com.oregonmarkets.domain.payment.model.Deposit;
import com.oregonmarkets.domain.user.model.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AllSettersTest {

  @Test
  void blockchainChainSetters() {
    BlockchainChain c = new BlockchainChain();
    c.setId(UUID.randomUUID());
    c.setChainName("n");
    c.setChainId(1);
    c.setRpcUrl("u");
    c.setUsdcTokenAddress("a");
    c.setUsdcDecimals(6);
    c.setRequiredConfirmations(12);
    c.setIsActive(true);
    c.setLastScannedBlock(0L);
    c.setCreatedAt(Instant.now());
    assertNotNull(c.getId());
  }

  @Test
  void enclaveChainAddressSetters() {
    EnclaveChainAddress a = new EnclaveChainAddress();
    a.setId(UUID.randomUUID());
    a.setUserId(UUID.randomUUID());
    a.setChainType(EnclaveChainAddress.ChainType.ETHEREUM);
    a.setDepositAddress("a");
    a.setCreatedAt(Instant.now());
    assertNotNull(a.getId());
  }

  @Test
  void depositSetters() {
    Deposit d = new Deposit();
    d.setId(UUID.randomUUID());
    d.setUserId(UUID.randomUUID());
    d.setAmount(BigDecimal.TEN);
    d.setCurrency("U");
    d.setMethod(Deposit.DepositMethod.CRYPTO);
    d.setStatus(Deposit.DepositStatus.PENDING);
    d.setProcessingStatus(Deposit.ProcessingStatus.DETECTED);
    d.setTxHash("h");
    d.setChainId(1);
    d.setToAddress("a");
    d.setTokenAddress("t");
    d.setRawAmount("r");
    d.setBlockNumber(1L);
    d.setConfirmations(0);
    d.setRequiredConfirmations(12);
    d.setCreatedAt(Instant.now());
    d.setUpdatedAt(Instant.now());
    assertNotNull(d.getId());
  }

  @Test
  void userSetters() {
    User u = new User();
    u.setId(UUID.randomUUID());
    u.setEmail("e");
    u.setUsername("u");
    u.setDisplayName("d");
    u.setMagicUserId("m");
    u.setMagicWalletAddress("a");
    u.setMagicIssuer("i");
    u.setWeb3WalletAddress("w");
    u.setAuthMethod(User.AuthMethod.MAGIC);
    u.setWalletVerifiedAt(Instant.now());
    u.setEnclaveUserId("e");
    u.setEnclaveUdaAddress("u");
    u.setEnclaveUdaTag("t");
    u.setEnclaveUdaCreatedAt(Instant.now());
    u.setEnclaveUdaStatus(User.EnclaveUdaStatus.ACTIVE);
    u.setEnclaveDepositAddresses("d");
    u.setCountryCode("US");
    u.setIsActive(true);
    u.setEmailVerified(true);
    u.setEmailVerifiedAt(Instant.now());
    u.setKycStatus(User.KycStatus.APPROVED);
    u.setKycLevel(1);
    u.setDailyDepositLimit(BigDecimal.TEN);
    u.setDailyWithdrawalLimit(BigDecimal.ONE);
    u.setBlnkIdentityId("b");
    u.setBlnkBalanceId("b");
    u.setBlnkCreatedAt(Instant.now());
    u.setProxyWalletAddress("p");
    u.setProxyWalletCreatedAt(Instant.now());
    u.setProxyWalletStatus(User.ProxyWalletStatus.ACTIVE);
    u.setReferralCode("r");
    u.setReferredByUserId(UUID.randomUUID());
    u.setUtmSource("s");
    u.setUtmMedium("m");
    u.setUtmCampaign("c");
    u.setAvatarUrl("a");
    u.setProxyWalletQrCodeUrl("p");
    u.setEnclaveUdaQrCodeUrl("e");
    u.setEvmDepositQrCodes("e");
    u.setSolanaDepositQrCodeUrl("s");
    u.setBitcoinDepositQrCodes("b");
    u.setCreatedAt(Instant.now());
    u.setUpdatedAt(Instant.now());
    assertNotNull(u.getId());
  }
}
