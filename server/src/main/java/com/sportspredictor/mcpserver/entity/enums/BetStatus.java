package com.sportspredictor.mcpserver.entity.enums;

/** Bet settlement status matching the bet.status column. */
public enum BetStatus {
    PENDING,
    WON,
    LOST,
    PUSHED,
    CANCELLED,
    VOID
}
