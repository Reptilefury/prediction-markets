package com.oregonMarkets.domain.enclave.model;

import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("enclave_chain_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnclaveChainAddress {

  @Id private UUID id;

  @Column("user_id")
  private UUID userId;

  @Column("chain_type")
  private ChainType chainType;

  @Column("chain_id")
  private Integer chainId;

  @Column("network")
  private String network;

  @Column("deposit_address")
  private String depositAddress;

  @Column("address_tag")
  private String addressTag;

  @Column("is_primary")
  @Builder.Default
  private Boolean isPrimary = false;

  @Column("is_active")
  @Builder.Default
  private Boolean isActive = true;

  @CreatedDate
  @Column("created_at")
  private Instant createdAt;

  public enum ChainType {
    ETHEREUM,
    POLYGON,
    ARBITRUM,
    OPTIMISM,
    BASE
  }
}
