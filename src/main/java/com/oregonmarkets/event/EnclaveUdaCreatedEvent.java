package com.oregonmarkets.event;

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
public class EnclaveUdaCreatedEvent {
  private UUID userId;
  private String magicWalletAddress;
  private String proxyWalletAddress;
  private String enclaveUdaAddress;
  private String email;
  private String magicUserId; // The Magic User ID (issuer)
  private String didToken; // The DID token
  private Map<String, Object> depositAddresses; // Enclave deposit addresses for QR codes
  private Instant timestamp;

  // Defensive copying for mutable fields
  public Map<String, Object> getDepositAddresses() {
    return depositAddresses == null ? null : Map.copyOf(depositAddresses);
  }

  public void setDepositAddresses(Map<String, Object> depositAddresses) {
    this.depositAddresses = depositAddresses == null ? null : Map.copyOf(depositAddresses);
  }
}
