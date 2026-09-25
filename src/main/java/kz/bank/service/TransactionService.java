package kz.bank.service;

import kz.bank.exception.NotFoundException;
import kz.bank.exception.ValidationException;
import kz.bank.model.Transaction;
import kz.bank.model.TransactionType;
import kz.bank.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles creation and retrieval of Transaction records.
 * Transaction creation is intentionally package-accessible so that
 * DepositService (the only caller) can record operations atomically.
 */
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final IdGenerator idGenerator;

    public TransactionService(TransactionRepository transactionRepository,
                               IdGenerator idGenerator) {
        this.transactionRepository = transactionRepository;
        this.idGenerator = idGenerator;
    }

    /**
     * Record a financial operation on a deposit.
     * Called by DepositService after every successful balance change.
     *
     * The stored amount carries a sign that reflects the direction of the flow
     * from the perspective of this deposit:
     *   DEPOSIT      →  positive  (money arrived)
     *   TRANSFER_IN  →  positive  (money arrived)
     *   WITHDRAW     →  negative  (money left)
     *   TRANSFER_OUT →  negative  (money left)
     *
     * @param depositId ID of the affected deposit
     * @param amount    the absolute monetary amount (must be > 0)
     * @param type      what kind of operation this was
     * @return the persisted Transaction
     */
    public Transaction record(Long depositId, BigDecimal amount, TransactionType type) {
        BigDecimal signedAmount = isOutgoing(type) ? amount.negate() : amount;

        Transaction tx = new Transaction(
                idGenerator.next(),
                depositId,
                signedAmount,
                LocalDateTime.now(),
                type
        );
        return transactionRepository.save(tx);
    }

    /** Returns true for transaction types that reduce the deposit balance. */
    private boolean isOutgoing(TransactionType type) {
        return type == TransactionType.WITHDRAW || type == TransactionType.TRANSFER_OUT;
    }
    // INTEREST, DEPOSIT, TRANSFER_IN — all incoming (positive sign), handled by default above

    /**
     * Retrieve a single transaction by ID.
     */
    public Transaction getTransactionById(Long id) {
        validateId(id, "Transaction ID");
        return transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction", id));
    }

    /**
     * Return all transactions for a given deposit, oldest first.
     */
    public List<Transaction> getTransactionsByDepositId(Long depositId) {
        validateId(depositId, "Deposit ID");
        return transactionRepository.findByDepositId(depositId);
    }

    /**
     * Return every transaction in the system.
     */
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void validateId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new ValidationException(fieldName + " must be a positive number.");
        }
    }
}
