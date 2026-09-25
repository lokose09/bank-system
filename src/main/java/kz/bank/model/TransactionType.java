package kz.bank.model;

/**
 * Describes the direction/purpose of a financial transaction.
 *
 * DEPOSIT      – funds were added to a deposit.
 * WITHDRAW     – funds were taken from a deposit.
 * TRANSFER_OUT – funds left this deposit as part of a transfer.
 * TRANSFER_IN  – funds arrived at this deposit as part of a transfer.
 * INTEREST     – monthly interest reward accrued during deposit simulation.
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAW,
    TRANSFER_OUT,
    TRANSFER_IN,
    INTEREST
}
