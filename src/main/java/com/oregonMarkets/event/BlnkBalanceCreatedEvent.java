package com.oregonMarkets.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlnkBalanceCreatedEvent {
  private UUID userId;
  private String magicWalletAddress;
  private String proxyWalletAddress;
  private String enclaveUdaAddress;
  private String blnkIdentityId;
  private String blnkBalanceId;
  private String email;
  private String
      magicUserId; // The decoded Magic User ID (e.g., CQ0U5PbimduW29o9eRLqysxJPSAhPT__DiXK6D2RPd0)
  private String didToken; // The DID token for password
  private Map<String, Object> depositAddresses; // Enclave deposit addresses for QR code generation
  private Instant timestamp;

  // Defensive copying for mutable fields
  public Map<String, Object> getDepositAddresses() {
    return depositAddresses == null ? null : Map.copyOf(depositAddresses);
  }

  public void setDepositAddresses(Map<String, Object> depositAddresses) {
    this.depositAddresses = depositAddresses == null ? null : Map.copyOf(depositAddresses);
  }
}
