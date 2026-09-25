package kz.bank.model;

/**
 * Represents a bank account belonging to a customer.
 * One customer can have multiple accounts.
 * Each account has a unique, auto-generated account number.
 */
public class Account {

    private Long id;
    private String accountNumber;
    private Long customerId;

    public Account(Long id, String accountNumber, Long customerId) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.customerId = customerId;
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public Long getCustomerId() {
        return customerId;
    }

    @Override
    public String toString() {
        return String.format("Account{id=%d, accountNumber='%s', customerId=%d}",
                id, accountNumber, customerId);
    }
}
