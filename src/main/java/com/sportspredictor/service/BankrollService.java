package com.sportspredictor.service;

import com.sportspredictor.entity.Bankroll;
import com.sportspredictor.entity.BankrollTransaction;
import com.sportspredictor.entity.Bet;
import com.sportspredictor.entity.enums.BetStatus;
import com.sportspredictor.entity.enums.TransactionType;
import com.sportspredictor.repository.BankrollRepository;
import com.sportspredictor.repository.BankrollTransactionRepository;
import com.sportspredictor.repository.BetRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages bankroll lifecycle: status, deposits, withdrawals, and resets. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BankrollService {

    private static final BigDecimal DEFAULT_STARTING_BALANCE = new BigDecimal("1000.00");
    private static final int SCALE = 2;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final BankrollRepository bankrollRepository;
    private final BankrollTransactionRepository transactionRepository;
    private final BetRepository betRepository;

    /** Full bankroll status snapshot. */
    public record BankrollStatusResult(
            String bankrollId,
            String name,
            BigDecimal startingBalance,
            BigDecimal currentBalance,
            BigDecimal totalWagered,
            BigDecimal totalWon,
            BigDecimal totalLost,
            BigDecimal netProfitLoss,
            BigDecimal roiPercent,
            int totalBets,
            int wins,
            int losses,
            int pushes,
            int pending,
            int currentStreak,
            String streakType,
            Instant createdAt,
            String summary) {}

    /** Result of archiving and resetting the bankroll. */
    public record ResetBankrollResult(
            String archivedBankrollId, String newBankrollId, BigDecimal startingBalance, String summary) {}

    /** Result of a deposit operation. */
    public record DepositResult(String transactionId, BigDecimal amount, BigDecimal balanceAfter, String summary) {}

    /** Result of a withdrawal operation. */
    public record WithdrawResult(String transactionId, BigDecimal amount, BigDecimal balanceAfter, String summary) {}

    /** Returns the active (non-archived) bankroll, creating a default one if none exists. */
    public Bankroll getActiveBankroll() {
        List<Bankroll> active = bankrollRepository.findByArchivedAtIsNull();
        if (!active.isEmpty()) {
            return active.getFirst();
        }
        log.info("No active bankroll found — creating default with ${}", DEFAULT_STARTING_BALANCE);
        Bankroll bankroll = Bankroll.builder()
                .name("Default")
                .startingBalance(DEFAULT_STARTING_BALANCE)
                .currentBalance(DEFAULT_STARTING_BALANCE)
                .createdAt(Instant.now())
                .build();
        return bankrollRepository.save(bankroll);
    }

    /** Returns a comprehensive status snapshot of the active bankroll. */
    public BankrollStatusResult getBankrollStatus() {
        Bankroll bankroll = getActiveBankroll();
        List<Bet> bets = betRepository.findByBankrollId(bankroll.getId());

        BigDecimal totalWagered = BigDecimal.ZERO;
        BigDecimal totalWon = BigDecimal.ZERO;
        BigDecimal totalLost = BigDecimal.ZERO;
        int wins = 0;
        int losses = 0;
        int pushes = 0;
        int pending = 0;

        for (Bet bet : bets) {
            totalWagered = totalWagered.add(bet.getStake());
            switch (bet.getStatus()) {
                case WON -> {
                    wins++;
                    totalWon = totalWon.add(bet.getActualPayout() != null ? bet.getActualPayout() : BigDecimal.ZERO);
                }
                case LOST -> {
                    losses++;
                    totalLost = totalLost.add(bet.getStake());
                }
                case PUSHED -> pushes++;
                case PENDING -> pending++;
                default -> {
                    /* CANCELLED, VOID not counted */
                }
            }
        }

        BigDecimal netProfitLoss = bankroll.getCurrentBalance().subtract(bankroll.getStartingBalance());
        BigDecimal roiPercent = totalWagered.compareTo(BigDecimal.ZERO) > 0
                ? netProfitLoss
                        .divide(totalWagered, SCALE + 2, RoundingMode.HALF_UP)
                        .multiply(HUNDRED)
                        .setScale(SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);

        StreakInfo streak = computeStreak(bets);

        String summary = String.format(
                "Bankroll '%s': $%s (started $%s). Record: %d-%d-%d, %d pending. ROI: %s%%",
                bankroll.getName(),
                bankroll.getCurrentBalance().setScale(SCALE, RoundingMode.HALF_UP),
                bankroll.getStartingBalance().setScale(SCALE, RoundingMode.HALF_UP),
                wins,
                losses,
                pushes,
                pending,
                roiPercent);

        return new BankrollStatusResult(
                bankroll.getId(),
                bankroll.getName(),
                bankroll.getStartingBalance(),
                bankroll.getCurrentBalance(),
                totalWagered.setScale(SCALE, RoundingMode.HALF_UP),
                totalWon.setScale(SCALE, RoundingMode.HALF_UP),
                totalLost.setScale(SCALE, RoundingMode.HALF_UP),
                netProfitLoss.setScale(SCALE, RoundingMode.HALF_UP),
                roiPercent,
                bets.size(),
                wins,
                losses,
                pushes,
                pending,
                streak.count(),
                streak.type(),
                bankroll.getCreatedAt(),
                summary);
    }

    /** Archives the current bankroll and creates a fresh one with the given starting balance. */
    public ResetBankrollResult resetBankroll(BigDecimal startingAmount) {
        validatePositiveAmount(startingAmount, "Starting amount");

        Bankroll current = getActiveBankroll();
        current.setArchivedAt(Instant.now());
        bankrollRepository.save(current);

        Bankroll fresh = Bankroll.builder()
                .name("Season "
                        + (bankrollRepository.findByArchivedAtIsNotNull().size() + 1))
                .startingBalance(startingAmount.setScale(SCALE, RoundingMode.HALF_UP))
                .currentBalance(startingAmount.setScale(SCALE, RoundingMode.HALF_UP))
                .createdAt(Instant.now())
                .build();
        bankrollRepository.save(fresh);

        String summary = String.format(
                "Archived bankroll '%s' (final balance $%s). New bankroll started with $%s",
                current.getName(),
                current.getCurrentBalance().setScale(SCALE, RoundingMode.HALF_UP),
                startingAmount.setScale(SCALE, RoundingMode.HALF_UP));

        return new ResetBankrollResult(current.getId(), fresh.getId(), startingAmount, summary);
    }

    /** Deposits funds into the active bankroll. */
    public DepositResult depositFunds(BigDecimal amount) {
        validatePositiveAmount(amount, "Deposit amount");

        Bankroll bankroll = getActiveBankroll();
        BigDecimal newBalance = bankroll.getCurrentBalance().add(amount);
        bankroll.setCurrentBalance(newBalance);
        bankrollRepository.save(bankroll);

        BankrollTransaction txn = BankrollTransaction.builder()
                .bankroll(bankroll)
                .type(TransactionType.DEPOSIT)
                .amount(amount.setScale(SCALE, RoundingMode.HALF_UP))
                .balanceAfter(newBalance.setScale(SCALE, RoundingMode.HALF_UP))
                .createdAt(Instant.now())
                .build();
        transactionRepository.save(txn);

        String summary = String.format(
                "Deposited $%s. New balance: $%s",
                amount.setScale(SCALE, RoundingMode.HALF_UP), newBalance.setScale(SCALE, RoundingMode.HALF_UP));

        return new DepositResult(txn.getId(), amount, newBalance, summary);
    }

    /** Withdraws funds from the active bankroll. */
    public WithdrawResult withdrawFunds(BigDecimal amount) {
        validatePositiveAmount(amount, "Withdrawal amount");

        Bankroll bankroll = getActiveBankroll();
        if (amount.compareTo(bankroll.getCurrentBalance()) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Insufficient funds: requested $%s but balance is $%s",
                    amount.setScale(SCALE, RoundingMode.HALF_UP),
                    bankroll.getCurrentBalance().setScale(SCALE, RoundingMode.HALF_UP)));
        }

        BigDecimal newBalance = bankroll.getCurrentBalance().subtract(amount);
        bankroll.setCurrentBalance(newBalance);
        bankrollRepository.save(bankroll);

        BankrollTransaction txn = BankrollTransaction.builder()
                .bankroll(bankroll)
                .type(TransactionType.WITHDRAWAL)
                .amount(amount.setScale(SCALE, RoundingMode.HALF_UP))
                .balanceAfter(newBalance.setScale(SCALE, RoundingMode.HALF_UP))
                .createdAt(Instant.now())
                .build();
        transactionRepository.save(txn);

        String summary = String.format(
                "Withdrew $%s. New balance: $%s",
                amount.setScale(SCALE, RoundingMode.HALF_UP), newBalance.setScale(SCALE, RoundingMode.HALF_UP));

        return new WithdrawResult(txn.getId(), amount, newBalance, summary);
    }

    private record StreakInfo(int count, String type) {}

    private StreakInfo computeStreak(List<Bet> bets) {
        List<Bet> settled = bets.stream()
                .filter(b -> b.getStatus() == BetStatus.WON || b.getStatus() == BetStatus.LOST)
                .sorted(Comparator.comparing(Bet::getSettledAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        if (settled.isEmpty()) {
            return new StreakInfo(0, "NONE");
        }

        BetStatus streakStatus = settled.getFirst().getStatus();
        int count = 0;
        for (Bet bet : settled) {
            if (bet.getStatus() == streakStatus) {
                count++;
            } else {
                break;
            }
        }

        String type = streakStatus == BetStatus.WON ? "WIN" : "LOSS";
        return new StreakInfo(count, type);
    }

    private static void validatePositiveAmount(BigDecimal amount, String label) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(label + " must be positive, got: " + amount);
        }
    }
}
