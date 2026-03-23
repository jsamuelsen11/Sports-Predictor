package com.sportspredictor.entity;

import com.sportspredictor.entity.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A ledger entry recording a deposit, withdrawal, or bet-related balance change. */
@Entity
@Table(name = "bankroll_transaction")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BankrollTransaction {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bankroll_id", nullable = false, updatable = false)
    private Bankroll bankroll;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;

    @Column(name = "amount", nullable = false)
    private double amount;

    @Column(name = "balance_after", nullable = false)
    private double balanceAfter;

    @Column(name = "reference_bet_id")
    private String referenceBetId;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TEXT")
    private Instant createdAt;

    @PrePersist
    void generateId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
