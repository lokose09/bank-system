package kz.bank.repository;

import kz.bank.model.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory storage for Transaction entities.
 * Transactions are immutable records — they are only ever inserted, never updated.
 * Contains NO business logic — only insert and lookup operations.
 */
public class TransactionRepository {

    private final Map<Long, Transaction> store = new HashMap<>();

    /** Persist a new transaction. */
    public Transaction save(Transaction transaction) {
        store.put(transaction.getId(), transaction);
        return transaction;
    }

    /** Find a transaction by primary key. */
    public Optional<Transaction> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    /** Return all stored transactions (all deposits). */
    public List<Transaction> findAll() {
        return new ArrayList<>(store.values());
    }

    /**
     * Return all transactions for a specific deposit,
     * ordered by timestamp ascending (oldest first).
     */
    public List<Transaction> findByDepositId(Long depositId) {
        return store.values().stream()
                .filter(t -> t.getDepositId().equals(depositId))
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .toList();
    }
}
