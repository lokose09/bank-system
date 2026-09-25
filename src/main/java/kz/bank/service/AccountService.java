package kz.bank.service;

import kz.bank.exception.NotFoundException;
import kz.bank.exception.ValidationException;
import kz.bank.model.Account;
import kz.bank.repository.AccountRepository;
import kz.bank.repository.CustomerRepository;

import java.util.List;
import java.util.UUID;

/**
 * Business logic for Account management.
 * Generates unique account numbers and verifies that the owning customer exists.
 */
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final IdGenerator idGenerator;

    public AccountService(AccountRepository accountRepository,
                          CustomerRepository customerRepository,
                          IdGenerator idGenerator) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.idGenerator = idGenerator;
    }

    /**
     * Create a new account for the given customer.
     * The account number is generated automatically and is guaranteed unique.
     *
     * @param customerId ID of an existing customer
     * @return the newly created Account
     */
    public Account createAccount(Long customerId) {
        validateId(customerId, "Customer ID");

        // Verify the customer actually exists
        customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer", customerId));

        String accountNumber = generateUniqueAccountNumber();

        Account account = new Account(idGenerator.next(), accountNumber, customerId);
        return accountRepository.save(account);
    }

    /**
     * Retrieve an account by ID or throw NotFoundException.
     */
    public Account getAccountById(Long id) {
        validateId(id, "Account ID");
        return accountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Account", id));
    }

    /**
     * Return all accounts in the system.
     */
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    /**
     * Return all accounts belonging to a specific customer.
     */
    public List<Account> getAccountsByCustomerId(Long customerId) {
        validateId(customerId, "Customer ID");
        return accountRepository.findByCustomerId(customerId);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Generates a formatted account number like ACC-3F2A-19B0.
     * Retries until a collision-free number is found (extremely rare in practice).
     */
    private String generateUniqueAccountNumber() {
        String number;
        do {
            String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
            number = "ACC-" + uuid.substring(0, 4) + "-" + uuid.substring(4, 8);
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }

    private void validateId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new ValidationException(fieldName + " must be a positive number.");
        }
    }
}
