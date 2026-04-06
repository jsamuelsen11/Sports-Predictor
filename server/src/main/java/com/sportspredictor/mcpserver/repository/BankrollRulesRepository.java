package com.sportspredictor.mcpserver.repository;

import com.sportspredictor.mcpserver.entity.BankrollRules;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link BankrollRules} entities. */
public interface BankrollRulesRepository extends JpaRepository<BankrollRules, String> {

    /** Returns the rules for the given bankroll, if any exist. */
    Optional<BankrollRules> findByBankrollId(String bankrollId);
}
