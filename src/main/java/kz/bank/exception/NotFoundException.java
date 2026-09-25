package kz.bank.exception;

/**
 * Thrown when a requested entity (Customer, Account, Deposit, Transaction)
 * cannot be found by the given identifier.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String entityName, Long id) {
        super(entityName + " with id=" + id + " not found.");
    }
}
