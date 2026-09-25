package kz.bank.model;

/**
 * Defines the type of a deposit and its withdrawal behaviour.
 *
 * WITHDRAWABLE – money can be deposited AND withdrawn freely.
 * SAVINGS      – money can be deposited, but withdrawals are NOT allowed.
 */
public enum DepositType {

    /** Standard deposit: supports both top-ups and withdrawals. */
    WITHDRAWABLE,

    /** Savings deposit: supports top-ups only; withdrawals are forbidden. */
    SAVINGS
}
