package com.oregonMarkets.domain.blockchain.model;

import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("blockchain_chains")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockchainChain {

  @Id private UUID id;

  @Column("chain_name")
  private String chainName;

  @Column("chain_id")
  private Integer chainId;

  @Column("rpc_url")
  private String rpcUrl;

  @Column("usdc_token_address")
  private String usdcTokenAddress;

  @Column("usdc_decimals")
  @Builder.Default
  private Integer usdcDecimals = 6;

  @Column("required_confirmations")
  @Builder.Default
  private Integer requiredConfirmations = 12;

  @Column("is_active")
  @Builder.Default
  private Boolean isActive = true;

  @Column("last_scanned_block")
  @Builder.Default
  private Long lastScannedBlock = 0L;

  @CreatedDate
  @Column("created_at")
  private Instant createdAt;
}
