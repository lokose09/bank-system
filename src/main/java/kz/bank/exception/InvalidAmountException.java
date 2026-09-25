package kz.bank.exception;

import java.math.BigDecimal;

/**
 * Thrown when a monetary amount fails basic validation:
 * null, zero, or negative values are all invalid for financial operations.
 */
public class InvalidAmountException extends RuntimeException {

    public InvalidAmountException(String message) {
        super(message);
    }

    public InvalidAmountException(BigDecimal amount) {
        super("Amount must be greater than zero, but got: " +
              (amount == null ? "null" : amount.toPlainString()));
    }
}
