package kz.bank.service;

import kz.bank.exception.NotFoundException;
import kz.bank.exception.ValidationException;
import kz.bank.model.Customer;
import kz.bank.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CustomerService.
 * Each test gets a fresh repository + service — no shared state between tests.
 */
@DisplayName("CustomerService")
class CustomerServiceTest {

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        // Fresh instances for every test — full isolation
        customerService = new CustomerService(new CustomerRepository(), new IdGenerator());
    }

    // =========================================================================
    // createCustomer — happy path
    // =========================================================================

    @Test
    @DisplayName("createCustomer: returns customer with correct data")
    void createCustomer_success() {
        Customer c = customerService.createCustomer("Artur", "Test", "artur@example.com");

        assertNotNull(c);
        assertNotNull(c.getId());
        assertTrue(c.getId() > 0);
        assertEquals("Artur", c.getFirstName());
        assertEquals("Test", c.getLastName());
        assertEquals("artur@example.com", c.getUniqueCode());
    }

    @Test
    @DisplayName("createCustomer: trims leading/trailing whitespace from inputs")
    void createCustomer_trimsWhitespace() {
        Customer c = customerService.createCustomer("  Alice  ", "  Smith  ", "  alice@test.com  ");

        assertEquals("Alice", c.getFirstName());
        assertEquals("Smith", c.getLastName());
        assertEquals("alice@test.com", c.getUniqueCode());
    }

    @Test
    @DisplayName("createCustomer: generates unique IDs for multiple customers")
    void createCustomer_idsAreUnique() {
        Customer c1 = customerService.createCustomer("One", "A", "one@test.com");
        Customer c2 = customerService.createCustomer("Two", "B", "two@test.com");
        Customer c3 = customerService.createCustomer("Three", "C", "three@test.com");

        assertNotEquals(c1.getId(), c2.getId());
        assertNotEquals(c2.getId(), c3.getId());
        assertNotEquals(c1.getId(), c3.getId());
    }

    // =========================================================================
    // createCustomer — validation: blank fields
    // =========================================================================

    @Test
    @DisplayName("createCustomer: null firstName throws ValidationException")
    void createCustomer_nullFirstName_throws() {
        assertThrows(ValidationException.class,
                () -> customerService.createCustomer(null, "Test", "u@test.com"));
    }

    @Test
    @DisplayName("createCustomer: blank firstName throws ValidationException")
    void createCustomer_blankFirstName_throws() {
        assertThrows(ValidationException.class,
                () -> customerService.createCustomer("   ", "Test", "u@test.com"));
    }

    @Test
    @DisplayName("createCustomer: empty firstName throws ValidationException")
    void createCustomer_emptyFirstName_throws() {
        assertThrows(ValidationException.class,
                () -> customerService.createCustomer("", "Test", "u@test.com"));
    }

    @Test
    @DisplayName("createCustomer: null lastName throws ValidationException")
    void createCustomer_nullLastName_throws() {
        assertThrows(ValidationException.class,
                () -> customerService.createCustomer("Artur", null, "u@test.com"));
    }

    @Test
    @DisplayName("createCustomer: blank lastName throws ValidationException")
    void createCustomer_blankLastName_throws() {
        assertThrows(ValidationException.class,
                () -> customerService.createCustomer("Artur", "  ", "u@test.com"));
    }

    @Test
    @DisplayName("createCustomer: null uniqueCode throws ValidationException")
    void createCustomer_nullUniqueCode_throws() {
        assertThrows(ValidationException.class,
                () -> customerService.createCustomer("Artur", "Test", null));
    }

    @Test
    @DisplayName("createCustomer: blank uniqueCode throws ValidationException")
    void createCustomer_blankUniqueCode_throws() {
        assertThrows(ValidationException.class,
                () -> customerService.createCustomer("Artur", "Test", "   "));
    }

    // =========================================================================
    // createCustomer — uniqueCode uniqueness
    // =========================================================================

    @Test
    @DisplayName("createCustomer: duplicate uniqueCode throws ValidationException")
    void createCustomer_duplicateUniqueCode_throws() {
        customerService.createCustomer("Artur", "Test", "artur@example.com");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> customerService.createCustomer("Bob", "Smith", "artur@example.com"));

        assertTrue(ex.getMessage().contains("artur@example.com"));
    }

    @Test
    @DisplayName("createCustomer: duplicate uniqueCode case-insensitive throws ValidationException")
    void createCustomer_duplicateUniqueCodeCaseInsensitive_throws() {
        customerService.createCustomer("Artur", "Test", "artur@example.com");

        assertThrows(ValidationException.class,
                () -> customerService.createCustomer("Bob", "Smith", "ARTUR@EXAMPLE.COM"));
    }

    @Test
    @DisplayName("createCustomer: different uniqueCodes are both accepted")
    void createCustomer_differentUniqueCodes_bothAccepted() {
        Customer c1 = customerService.createCustomer("A", "One", "a@test.com");
        Customer c2 = customerService.createCustomer("B", "Two", "b@test.com");

        assertNotNull(c1);
        assertNotNull(c2);
    }

    // =========================================================================
    // getCustomerById
    // =========================================================================

    @Test
    @DisplayName("getCustomerById: returns existing customer")
    void getCustomerById_found() {
        Customer created = customerService.createCustomer("Artur", "Test", "u@test.com");

        Customer found = customerService.getCustomerById(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals("Artur", found.getFirstName());
    }

    @Test
    @DisplayName("getCustomerById: non-existent ID throws NotFoundException")
    void getCustomerById_notFound_throws() {
        assertThrows(NotFoundException.class,
                () -> customerService.getCustomerById(999L));
    }

    @Test
    @DisplayName("getCustomerById: zero ID throws ValidationException")
    void getCustomerById_zeroId_throws() {
        assertThrows(ValidationException.class,
                () -> customerService.getCustomerById(0L));
    }

    @Test
    @DisplayName("getCustomerById: negative ID throws ValidationException")
    void getCustomerById_negativeId_throws() {
        assertThrows(ValidationException.class,
                () -> customerService.getCustomerById(-1L));
    }

    @Test
    @DisplayName("getCustomerById: null ID throws ValidationException")
    void getCustomerById_nullId_throws() {
        assertThrows(ValidationException.class,
                () -> customerService.getCustomerById(null));
    }

    // =========================================================================
    // getAllCustomers
    // =========================================================================

    @Test
    @DisplayName("getAllCustomers: returns empty list when no customers exist")
    void getAllCustomers_empty() {
        List<Customer> all = customerService.getAllCustomers();
        assertNotNull(all);
        assertTrue(all.isEmpty());
    }

    @Test
    @DisplayName("getAllCustomers: returns all created customers")
    void getAllCustomers_returnsAll() {
        customerService.createCustomer("A", "One", "a@test.com");
        customerService.createCustomer("B", "Two", "b@test.com");
        customerService.createCustomer("C", "Three", "c@test.com");

        List<Customer> all = customerService.getAllCustomers();
        assertEquals(3, all.size());
    }
}
