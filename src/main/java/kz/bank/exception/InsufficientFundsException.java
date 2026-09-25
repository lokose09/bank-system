package kz.bank.exception;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Thrown when a withdrawal or transfer is attempted but the deposit
 * does not have enough funds to cover the requested amount.
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(BigDecimal available, BigDecimal requested) {
        super(String.format(
                "Insufficient funds: available=%.2f, requested=%.2f.",
                available.setScale(2, RoundingMode.HALF_UP),
                requested.setScale(2, RoundingMode.HALF_UP)
        ));
    }

    public InsufficientFundsException(String message) {
        super(message);
    }
}
