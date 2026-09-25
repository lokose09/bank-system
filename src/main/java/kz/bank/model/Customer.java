package kz.bank.model;

/**
 * Represents a bank customer.
 * Each customer has a unique code (e.g., email or personal ID).
 */
public class Customer {

    private Long id;
    private String firstName;
    private String lastName;
    private String uniqueCode;

    public Customer(Long id, String firstName, String lastName, String uniqueCode) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.uniqueCode = uniqueCode;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUniqueCode() {
        return uniqueCode;
    }

    public void setUniqueCode(String uniqueCode) {
        this.uniqueCode = uniqueCode;
    }

    @Override
    public String toString() {
        return String.format("Customer{id=%d, name='%s %s', uniqueCode='%s'}",
                id, firstName, lastName, uniqueCode);
    }
}
