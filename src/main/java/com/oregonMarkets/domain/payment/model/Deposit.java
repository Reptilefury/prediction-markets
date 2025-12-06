package com.oregonMarkets.domain.payment.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("deposits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deposit {

  @Id private UUID id;

  @Column("user_id")
  private UUID userId;

  @Column("amount")
  private BigDecimal amount;

  @Column("currency")
  private String currency;

  @Column("method")
  @Builder.Default
  private DepositMethod method = DepositMethod.CRYPTO;

  @Column("status")
  @Builder.Default
  private DepositStatus status = DepositStatus.PENDING;

  @Column("tx_hash")
  private String txHash;

  @Column("chain_id")
  private Integer chainId;

  @Column("from_address")
  private String fromAddress;

  @Column("to_address")
  private String toAddress;

  @Column("confirmations")
  @Builder.Default
  private Integer confirmations = 0;

  @Column("required_confirmations")
  @Builder.Default
  private Integer requiredConfirmations = 12;

  @Column("block_number")
  private Long blockNumber;

  @Column("block_timestamp")
  private Instant blockTimestamp;

  @Column("token_address")
  private String tokenAddress;

  @Column("raw_amount")
  private String rawAmount;

  @Column("processing_status")
  @Builder.Default
  private ProcessingStatus processingStatus = ProcessingStatus.DETECTED;

  @Column("credited_to_magic")
  @Builder.Default
  private Boolean creditedToMagic = false;

  @Column("blnk_mirrored")
  @Builder.Default
  private Boolean blnkMirrored = false;

  @CreatedDate
  @Column("created_at")
  private Instant createdAt;

  @LastModifiedDate
  @Column("updated_at")
  private Instant updatedAt;

  public enum DepositMethod {
    CRYPTO,
    MPESA,
    AIRTEL_MONEY
  }

  public enum DepositStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    FAILED
  }

  public enum ProcessingStatus {
    DETECTED,
    CONFIRMING,
    CONFIRMED,
    PROCESSING,
    CREDITED,
    COMPLETED,
    FAILED
  }
}
