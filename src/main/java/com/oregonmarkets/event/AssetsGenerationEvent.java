package com.oregonmarkets.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetsGenerationEvent {
  private UUID userId;
  private String email;
  private String proxyWalletAddress;
  private String enclaveUdaAddress;
  private String magicWalletAddress;
  private Map<String, Object> depositAddresses; // From Enclave response
  private Instant timestamp;

  // Defensive copying for mutable fields
  public Map<String, Object> getDepositAddresses() {
    return depositAddresses == null ? null : Map.copyOf(depositAddresses);
  }

  public void setDepositAddresses(Map<String, Object> depositAddresses) {
    this.depositAddresses = depositAddresses == null ? null : Map.copyOf(depositAddresses);
  }
}
