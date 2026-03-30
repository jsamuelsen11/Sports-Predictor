package com.sportspredictor.repository;

import com.sportspredictor.entity.BankrollTransaction;
import com.sportspredictor.entity.enums.TransactionType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link BankrollTransaction} entities. */
public interface BankrollTransactionRepository extends JpaRepository<BankrollTransaction, String> {

    /** Returns all transactions for the given bankroll. */
    List<BankrollTransaction> findByBankrollId(String bankrollId);

    /** Returns all transactions of the given type. */
    List<BankrollTransaction> findByType(TransactionType type);

    /** Returns transactions for a bankroll filtered by type. */
    List<BankrollTransaction> findByBankrollIdAndType(String bankrollId, TransactionType type);

    /** Returns transactions created within the given time range (inclusive). */
    List<BankrollTransaction> findByCreatedAtBetween(Instant start, Instant end);

    /** Returns transactions linked to the given bet. */
    List<BankrollTransaction> findByReferenceBetId(String referenceBetId);

    /** Returns transactions for a bankroll within a time range. */
    List<BankrollTransaction> findByBankrollIdAndCreatedAtBetween(String bankrollId, Instant start, Instant end);
}
