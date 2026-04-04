package com.sportspredictor.mcpserver.entity;

import com.sportspredictor.mcpserver.entity.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A ledger entry recording a deposit, withdrawal, or bet-related balance change. */
@Entity
@Table(name = "bankroll_transaction")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BankrollTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bankroll_id", nullable = false, updatable = false)
    private Bankroll bankroll;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false)
    private BigDecimal balanceAfter;

    @Column(name = "reference_bet_id")
    private String referenceBetId;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TEXT")
    private Instant createdAt;
}
