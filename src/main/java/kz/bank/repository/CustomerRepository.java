package kz.bank.repository;

import kz.bank.model.Customer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory storage for Customer entities.
 * Mimics the contract of a future database-backed repository.
 * Contains NO business logic — only CRUD and lookup operations.
 */
public class CustomerRepository {

    private final Map<Long, Customer> store = new HashMap<>();

    /** Persist a customer (insert or update). */
    public Customer save(Customer customer) {
        store.put(customer.getId(), customer);
        return customer;
    }

    /** Find a customer by primary key. */
    public Optional<Customer> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    /** Return all stored customers. */
    public List<Customer> findAll() {
        return new ArrayList<>(store.values());
    }

    /** Find a customer by their unique code (e.g. email / personal ID). */
    public Optional<Customer> findByUniqueCode(String uniqueCode) {
        return store.values().stream()
                .filter(c -> c.getUniqueCode().equalsIgnoreCase(uniqueCode))
                .findFirst();
    }

    /** Return true when the given unique code is already taken. */
    public boolean existsByUniqueCode(String uniqueCode) {
        return store.values().stream()
                .anyMatch(c -> c.getUniqueCode().equalsIgnoreCase(uniqueCode));
    }
}
