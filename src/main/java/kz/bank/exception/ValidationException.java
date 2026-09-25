package kz.bank.exception;

/**
 * Thrown when input data fails business validation rules
 * (e.g. blank name, duplicate unique code, same source and target deposit).
 * Keeps validation errors distinct from not-found or financial errors.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
