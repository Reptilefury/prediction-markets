package com.oregonmarkets;

import static org.junit.jupiter.api.Assertions.*;

import com.oregonmarkets.domain.enclave.model.EnclaveChainAddress;
import com.oregonmarkets.domain.market.model.MarketType;
import com.oregonmarkets.domain.market.model.OrderSide;
import com.oregonmarkets.domain.market.model.OrderStatus;
import com.oregonmarkets.domain.market.model.ResolutionSource;
import com.oregonmarkets.domain.payment.model.Deposit;
import com.oregonmarkets.domain.user.model.User;
import org.junit.jupiter.api.Test;

class AllEnumsTest {

  @Test
  void chainType() {
    for (EnclaveChainAddress.ChainType t : EnclaveChainAddress.ChainType.values()) {
      assertNotNull(t.name());
      assertNotNull(EnclaveChainAddress.ChainType.valueOf(t.name()));
    }
  }

  @Test
  void depositMethod() {
    for (Deposit.DepositMethod m : Deposit.DepositMethod.values()) {
      assertNotNull(m.name());
      assertNotNull(Deposit.DepositMethod.valueOf(m.name()));
    }
  }

  @Test
  void depositStatus() {
    for (Deposit.DepositStatus s : Deposit.DepositStatus.values()) {
      assertNotNull(s.name());
      assertNotNull(Deposit.DepositStatus.valueOf(s.name()));
    }
  }

  @Test
  void processingStatus() {
    for (Deposit.ProcessingStatus s : Deposit.ProcessingStatus.values()) {
      assertNotNull(s.name());
      assertNotNull(Deposit.ProcessingStatus.valueOf(s.name()));
    }
  }

  @Test
  void enclaveUdaStatus() {
    for (User.EnclaveUdaStatus s : User.EnclaveUdaStatus.values()) {
      assertNotNull(s.name());
      assertNotNull(User.EnclaveUdaStatus.valueOf(s.name()));
    }
  }

  @Test
  void kycStatus() {
    for (User.KycStatus s : User.KycStatus.values()) {
      assertNotNull(s.name());
      assertNotNull(User.KycStatus.valueOf(s.name()));
    }
  }

  @Test
  void authMethod() {
    for (User.AuthMethod m : User.AuthMethod.values()) {
      assertNotNull(m.name());
      assertNotNull(User.AuthMethod.valueOf(m.name()));
    }
  }

  @Test
  void proxyWalletStatus() {
    for (User.ProxyWalletStatus s : User.ProxyWalletStatus.values()) {
      assertNotNull(s.name());
      assertNotNull(User.ProxyWalletStatus.valueOf(s.name()));
    }
  }

  @Test
  void marketTypeEnum() {
    for (MarketType type : MarketType.values()) {
      assertNotNull(type.name());
      assertNotNull(MarketType.valueOf(type.name()));
    }
  }

  @Test
  void orderSideEnum() {
    for (OrderSide side : OrderSide.values()) {
      assertNotNull(side.name());
      assertNotNull(OrderSide.valueOf(side.name()));
    }
  }

  @Test
  void orderStatusEnum() {
    for (OrderStatus status : OrderStatus.values()) {
      assertNotNull(status.name());
      assertNotNull(OrderStatus.valueOf(status.name()));
    }
  }

  @Test
  void resolutionSourceEnum() {
    for (ResolutionSource source : ResolutionSource.values()) {
      assertNotNull(source.name());
      assertNotNull(ResolutionSource.valueOf(source.name()));
    }
  }
}
