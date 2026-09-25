package kz.bank.exception;

/**
 * Thrown when a withdrawal or outgoing transfer is attempted on a deposit
 * whose type does not permit withdrawals (e.g. DepositType.SAVINGS).
 */
public class WithdrawalNotAllowedException extends RuntimeException {

    public WithdrawalNotAllowedException(String depositNumber) {
        super("Withdrawal is not allowed for deposit '" + depositNumber +
              "'. This is a SAVINGS deposit. Only top-ups are permitted.");
    }
}
