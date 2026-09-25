package kz.bank.repository;

import kz.bank.model.Deposit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory storage for Deposit entities.
 * Contains NO business logic — only CRUD and lookup operations.
 */
public class DepositRepository {

    private final Map<Long, Deposit> store = new HashMap<>();

    /** Persist a deposit (insert or update). */
    public Deposit save(Deposit deposit) {
        store.put(deposit.getId(), deposit);
        return deposit;
    }

    /** Find a deposit by primary key. */
    public Optional<Deposit> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    /** Return all stored deposits. */
    public List<Deposit> findAll() {
        return new ArrayList<>(store.values());
    }

    /** Find a deposit by its unique deposit number. */
    public Optional<Deposit> findByDepositNumber(String depositNumber) {
        return store.values().stream()
                .filter(d -> d.getDepositNumber().equals(depositNumber))
                .findFirst();
    }

    /** Return all deposits that belong to the given account. */
    public List<Deposit> findByAccountId(Long accountId) {
        return store.values().stream()
                .filter(d -> d.getAccountId().equals(accountId))
                .toList();
    }

    /** Return true when the given deposit number is already taken. */
    public boolean existsByDepositNumber(String depositNumber) {
        return store.values().stream()
                .anyMatch(d -> d.getDepositNumber().equals(depositNumber));
    }
}
