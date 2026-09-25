package kz.bank.repository;

import kz.bank.model.Account;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory storage for Account entities.
 * Contains NO business logic — only CRUD and lookup operations.
 */
public class AccountRepository {

    private final Map<Long, Account> store = new HashMap<>();

    /** Persist an account (insert or update). */
    public Account save(Account account) {
        store.put(account.getId(), account);
        return account;
    }

    /** Find an account by primary key. */
    public Optional<Account> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    /** Return all stored accounts. */
    public List<Account> findAll() {
        return new ArrayList<>(store.values());
    }

    /** Find an account by its unique account number. */
    public Optional<Account> findByAccountNumber(String accountNumber) {
        return store.values().stream()
                .filter(a -> a.getAccountNumber().equals(accountNumber))
                .findFirst();
    }

    /** Return all accounts that belong to the given customer. */
    public List<Account> findByCustomerId(Long customerId) {
        return store.values().stream()
                .filter(a -> a.getCustomerId().equals(customerId))
                .toList();
    }

    /** Return true when the given account number is already taken. */
    public boolean existsByAccountNumber(String accountNumber) {
        return store.values().stream()
                .anyMatch(a -> a.getAccountNumber().equals(accountNumber));
    }
}
