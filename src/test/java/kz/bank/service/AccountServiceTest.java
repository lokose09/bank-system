package kz.bank.service;

import kz.bank.exception.NotFoundException;
import kz.bank.exception.ValidationException;
import kz.bank.model.Account;
import kz.bank.model.Customer;
import kz.bank.repository.AccountRepository;
import kz.bank.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AccountService.
 * Each test starts with a clean repository and a pre-created customer,
 * so tests never depend on each other.
 */
@DisplayName("AccountService")
class AccountServiceTest {

    private CustomerRepository customerRepository;
    private AccountRepository  accountRepository;
    private AccountService     accountService;

    /** ID of a customer that is guaranteed to exist before every test. */
    private Long existingCustomerId;

    @BeforeEach
    void setUp() {
        customerRepository = new CustomerRepository();
        accountRepository  = new AccountRepository();

        // Create one customer directly in the repo so AccountService tests
        // don't depend on CustomerService correctness.
        Customer customer = new Customer(1L, "Artur", "Test", "artur@test.com");
        customerRepository.save(customer);
        existingCustomerId = customer.getId();

        accountService = new AccountService(accountRepository, customerRepository, new IdGenerator());
    }

    // =========================================================================
    // createAccount — happy path
    // =========================================================================

    @Test
    @DisplayName("createAccount: returns account linked to correct customer")
    void createAccount_success() {
        Account account = accountService.createAccount(existingCustomerId);

        assertNotNull(account);
        assertNotNull(account.getId());
        assertTrue(account.getId() > 0);
        assertEquals(existingCustomerId, account.getCustomerId());
    }

    @Test
    @DisplayName("createAccount: account number is auto-generated and not blank")
    void createAccount_accountNumberGenerated() {
        Account account = accountService.createAccount(existingCustomerId);

        assertNotNull(account.getAccountNumber());
        assertFalse(account.getAccountNumber().isBlank());
    }

    @Test
    @DisplayName("createAccount: account number starts with ACC-")
    void createAccount_accountNumberFormat() {
        Account account = accountService.createAccount(existingCustomerId);

        assertTrue(account.getAccountNumber().startsWith("ACC-"),
                "Expected number starting with ACC-, got: " + account.getAccountNumber());
    }

    @Test
    @DisplayName("createAccount: account numbers are unique across multiple accounts")
    void createAccount_accountNumbersAreUnique() {
        // Create a second customer so we can have multiple accounts in the same repo
        customerRepository.save(new Customer(2L, "Bob", "Smith", "bob@test.com"));

        Set<String> numbers = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            Long cid = (i % 2 == 0) ? existingCustomerId : 2L;
            Account acc = accountService.createAccount(cid);
            assertTrue(numbers.add(acc.getAccountNumber()),
                    "Duplicate account number generated: " + acc.getAccountNumber());
        }
    }

    @Test
    @DisplayName("createAccount: IDs are unique and increment")
    void createAccount_idsAreUnique() {
        Account a1 = accountService.createAccount(existingCustomerId);
        Account a2 = accountService.createAccount(existingCustomerId);

        assertNotEquals(a1.getId(), a2.getId());
    }

    // =========================================================================
    // createAccount — validation errors
    // =========================================================================

    @Test
    @DisplayName("createAccount: non-existent customerId throws NotFoundException")
    void createAccount_nonExistentCustomer_throws() {
        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> accountService.createAccount(999L));

        assertTrue(ex.getMessage().contains("999"));
    }

    @Test
    @DisplayName("createAccount: zero customerId throws ValidationException")
    void createAccount_zeroCustomerId_throws() {
        assertThrows(ValidationException.class,
                () -> accountService.createAccount(0L));
    }

    @Test
    @DisplayName("createAccount: negative customerId throws ValidationException")
    void createAccount_negativeCustomerId_throws() {
        assertThrows(ValidationException.class,
                () -> accountService.createAccount(-5L));
    }

    @Test
    @DisplayName("createAccount: null customerId throws ValidationException")
    void createAccount_nullCustomerId_throws() {
        assertThrows(ValidationException.class,
                () -> accountService.createAccount(null));
    }

    // =========================================================================
    // getAccountById
    // =========================================================================

    @Test
    @DisplayName("getAccountById: returns existing account")
    void getAccountById_found() {
        Account created = accountService.createAccount(existingCustomerId);

        Account found = accountService.getAccountById(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals(created.getAccountNumber(), found.getAccountNumber());
    }

    @Test
    @DisplayName("getAccountById: non-existent ID throws NotFoundException")
    void getAccountById_notFound_throws() {
        assertThrows(NotFoundException.class,
                () -> accountService.getAccountById(999L));
    }

    @Test
    @DisplayName("getAccountById: zero ID throws ValidationException")
    void getAccountById_zeroId_throws() {
        assertThrows(ValidationException.class,
                () -> accountService.getAccountById(0L));
    }

    @Test
    @DisplayName("getAccountById: null ID throws ValidationException")
    void getAccountById_nullId_throws() {
        assertThrows(ValidationException.class,
                () -> accountService.getAccountById(null));
    }

    // =========================================================================
    // getAllAccounts / getAccountsByCustomerId
    // =========================================================================

    @Test
    @DisplayName("getAllAccounts: returns empty list when no accounts exist")
    void getAllAccounts_empty() {
        List<Account> all = accountService.getAllAccounts();
        assertNotNull(all);
        assertTrue(all.isEmpty());
    }

    @Test
    @DisplayName("getAllAccounts: returns all created accounts")
    void getAllAccounts_returnsAll() {
        accountService.createAccount(existingCustomerId);
        accountService.createAccount(existingCustomerId);

        assertEquals(2, accountService.getAllAccounts().size());
    }

    @Test
    @DisplayName("getAccountsByCustomerId: returns only accounts of that customer")
    void getAccountsByCustomerId_filtersCorrectly() {
        // Second customer
        customerRepository.save(new Customer(2L, "Bob", "Smith", "bob@test.com"));

        accountService.createAccount(existingCustomerId); // belongs to customer 1
        accountService.createAccount(existingCustomerId); // belongs to customer 1
        accountService.createAccount(2L);                 // belongs to customer 2

        List<Account> customer1Accounts = accountService.getAccountsByCustomerId(existingCustomerId);
        assertEquals(2, customer1Accounts.size());
        customer1Accounts.forEach(a -> assertEquals(existingCustomerId, a.getCustomerId()));

        List<Account> customer2Accounts = accountService.getAccountsByCustomerId(2L);
        assertEquals(1, customer2Accounts.size());
    }
}
