package com.sportspredictor.entity.enums;

/** Bankroll transaction types matching the bankroll_transaction.type column. */
public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    BET_PLACED,
    BET_WON,
    BET_LOST,
    BET_PUSH,
    BET_CANCELLED,
    CASH_OUT
}
