package kz.bank.service;

import kz.bank.exception.NotFoundException;
import kz.bank.exception.ValidationException;
import kz.bank.model.Customer;
import kz.bank.repository.CustomerRepository;

import java.util.List;

/**
 * Business logic for Customer management.
 * Validates input, enforces uniqueness rules, delegates persistence to the repository.
 */
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final IdGenerator idGenerator;

    public CustomerService(CustomerRepository customerRepository, IdGenerator idGenerator) {
        this.customerRepository = customerRepository;
        this.idGenerator = idGenerator;
    }

    /**
     * Create and persist a new customer.
     *
     * @param firstName  must not be blank
     * @param lastName   must not be blank
     * @param uniqueCode must not be blank and must not already be taken
     * @return the newly created Customer
     */
    public Customer createCustomer(String firstName, String lastName, String uniqueCode) {
        validateNotBlank(firstName, "First name");
        validateNotBlank(lastName, "Last name");
        validateNotBlank(uniqueCode, "Unique code");

        if (customerRepository.existsByUniqueCode(uniqueCode)) {
            throw new ValidationException(
                    "A customer with unique code '" + uniqueCode + "' already exists.");
        }

        Customer customer = new Customer(idGenerator.next(), firstName.trim(),
                lastName.trim(), uniqueCode.trim());
        return customerRepository.save(customer);
    }

    /**
     * Retrieve a customer by ID or throw NotFoundException.
     */
    public Customer getCustomerById(Long id) {
        validateId(id, "Customer ID");
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer", id));
    }

    /**
     * Return all customers in the system.
     */
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void validateNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " must not be blank.");
        }
    }

    private void validateId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new ValidationException(fieldName + " must be a positive number.");
        }
    }
}
