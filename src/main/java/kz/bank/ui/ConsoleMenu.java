package kz.bank.ui;

import kz.bank.model.Account;
import kz.bank.model.Customer;
import kz.bank.model.Deposit;
import kz.bank.model.DepositType;
import kz.bank.model.Transaction;
import kz.bank.service.AccountService;
import kz.bank.service.CustomerService;
import kz.bank.service.DepositService;
import kz.bank.service.TransactionService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Main console menu loop.
 * Handles all user interaction: displays menus, reads input via ConsoleInput,
 * delegates work to services, and prints results.
 * Contains NO business logic — only presentation and delegation.
 */
public class ConsoleMenu {

    private static final String SEPARATOR = "=".repeat(60);
    private static final String THIN_LINE  = "-".repeat(60);

    private final CustomerService    customerService;
    private final AccountService     accountService;
    private final DepositService     depositService;
    private final TransactionService transactionService;
    private final ConsoleInput       input;

    public ConsoleMenu(CustomerService customerService,
                       AccountService accountService,
                       DepositService depositService,
                       TransactionService transactionService,
                       ConsoleInput input) {
        this.customerService    = customerService;
        this.accountService     = accountService;
        this.depositService     = depositService;
        this.transactionService = transactionService;
        this.input              = input;
    }

    // =========================================================================
    // Main loop
    // =========================================================================

    public void run() {
        System.out.println();
        System.out.println(SEPARATOR);
        System.out.println("            WELCOME TO BANK SYSTEM");
        System.out.println(SEPARATOR);

        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = input.readIntInRange("Enter choice: ", 0, 13);
            System.out.println();

            try {
                switch (choice) {
                    case 1  -> handleCreateCustomer();
                    case 2  -> handleCreateAccount();
                    case 3  -> handleCreateDeposit();
                    case 4  -> handleDepositMoney();
                    case 5  -> handleWithdrawMoney();
                    case 6  -> handleTransferMoney();
                    case 7  -> handleSimulateDeposit();
                    case 8  -> handleEarlyCloseDeposit();
                    case 9  -> handleShowCustomers();
                    case 10 -> handleShowAccounts();
                    case 11 -> handleShowDeposits();
                    case 12 -> handleShowTransactions();
                    case 13 -> handleShowAllTransactions();
                    case 0  -> running = false;
                }
            } catch (Exception e) {
                System.out.println();
                System.out.println("  [ERROR] " + e.getMessage());
            }

            if (running) {
                System.out.println();
            }
        }

        System.out.println();
        System.out.println(SEPARATOR);
        System.out.println("  Thank you for using Bank System. Goodbye!");
        System.out.println(SEPARATOR);
        input.close();
    }

    // =========================================================================
    // Menu display
    // =========================================================================

    private void printMainMenu() {
        System.out.println();
        System.out.println(SEPARATOR);
        System.out.println("                   MAIN MENU");
        System.out.println(SEPARATOR);
        System.out.println("  CUSTOMERS");
        System.out.println("    1.  Create customer");
        System.out.println("    9.  Show all customers");
        System.out.println();
        System.out.println("  ACCOUNTS");
        System.out.println("    2.  Create account");
        System.out.println("   10.  Show all accounts");
        System.out.println();
        System.out.println("  DEPOSITS");
        System.out.println("    3.  Create deposit");
        System.out.println("   11.  Show all deposits");
        System.out.println();
        System.out.println("  FINANCIAL OPERATIONS");
        System.out.println("    4.  Deposit money");
        System.out.println("    5.  Withdraw money");
        System.out.println("    6.  Transfer money");
        System.out.println();
        System.out.println("  DEPOSIT SIMULATION");
        System.out.println("    7.  Simulate next month  (accrue interest)");
        System.out.println("    8.  Early close deposit  (forfeit accrued interest)");
        System.out.println();
        System.out.println("  TRANSACTIONS");
        System.out.println("   12.  Show transactions by deposit ID");
        System.out.println("   13.  Show ALL transactions");
        System.out.println();
        System.out.println("    0.  Exit");
        System.out.println(SEPARATOR);
    }

    // =========================================================================
    // Customer handlers
    // =========================================================================

    private void handleCreateCustomer() {
        System.out.println(THIN_LINE);
        System.out.println("  CREATE CUSTOMER");
        System.out.println(THIN_LINE);
        String firstName  = input.readString("  First name  : ");
        String lastName   = input.readString("  Last name   : ");
        String uniqueCode = input.readString("  Unique code : ");

        Customer customer = customerService.createCustomer(firstName, lastName, uniqueCode);
        System.out.println();
        System.out.println("  [OK] Customer created:");
        printCustomer(customer);
    }

    private void handleShowCustomers() {
        System.out.println(THIN_LINE);
        System.out.println("  ALL CUSTOMERS");
        System.out.println(THIN_LINE);
        List<Customer> customers = customerService.getAllCustomers();
        if (customers.isEmpty()) {
            System.out.println("  (no customers yet)");
        } else {
            customers.forEach(this::printCustomer);
        }
    }

    // =========================================================================
    // Account handlers
    // =========================================================================

    private void handleCreateAccount() {
        System.out.println(THIN_LINE);
        System.out.println("  CREATE ACCOUNT");
        System.out.println(THIN_LINE);
        long customerId = input.readPositiveLong("  Customer ID: ");

        Account account = accountService.createAccount(customerId);
        System.out.println();
        System.out.println("  [OK] Account created:");
        printAccount(account);
    }

    private void handleShowAccounts() {
        System.out.println(THIN_LINE);
        System.out.println("  ALL ACCOUNTS");
        System.out.println(THIN_LINE);
        List<Account> accounts = accountService.getAllAccounts();
        if (accounts.isEmpty()) {
            System.out.println("  (no accounts yet)");
        } else {
            accounts.forEach(this::printAccount);
        }
    }

    // =========================================================================
    // Deposit handlers
    // =========================================================================

    private void handleCreateDeposit() {
        System.out.println(THIN_LINE);
        System.out.println("  CREATE DEPOSIT");
        System.out.println(THIN_LINE);

        long accountId = input.readPositiveLong("  Account ID       : ");
        int  typeChoice = input.readDepositTypeChoice();
        DepositType depositType = (typeChoice == 1) ? DepositType.WITHDRAWABLE : DepositType.SAVINGS;

        BigDecimal rate       = input.readRate();
        int        termMonths = input.readTermMonths();

        // Open date defaults to today; endDate is auto-calculated inside Deposit
        LocalDate openDate = LocalDate.now();

        Deposit deposit = depositService.createDeposit(
                accountId, depositType, BigDecimal.ZERO, rate, termMonths, openDate);

        System.out.println();
        System.out.println("  [OK] Deposit created:");
        printDeposit(deposit);
    }

    private void handleShowDeposits() {
        System.out.println(THIN_LINE);
        System.out.println("  ALL DEPOSITS");
        System.out.println(THIN_LINE);
        List<Deposit> deposits = depositService.getAllDeposits();
        if (deposits.isEmpty()) {
            System.out.println("  (no deposits yet)");
        } else {
            deposits.forEach(this::printDeposit);
        }
    }

    // =========================================================================
    // Financial operation handlers
    // =========================================================================

    private void handleDepositMoney() {
        System.out.println(THIN_LINE);
        System.out.println("  DEPOSIT MONEY");
        System.out.println(THIN_LINE);
        long       depositId = input.readPositiveLong("  Deposit ID : ");
        BigDecimal amount    = input.readPositiveAmount("  Amount     : ");

        Deposit updated = depositService.depositMoney(depositId, amount);
        System.out.println();
        System.out.printf("  [OK] Deposited %s. New balance: %s%n",
                formatMoney(amount), formatMoney(updated.getBalance()));
    }

    private void handleWithdrawMoney() {
        System.out.println(THIN_LINE);
        System.out.println("  WITHDRAW MONEY");
        System.out.println(THIN_LINE);
        long       depositId = input.readPositiveLong("  Deposit ID : ");
        BigDecimal amount    = input.readPositiveAmount("  Amount     : ");

        Deposit updated = depositService.withdrawMoney(depositId, amount);
        System.out.println();
        System.out.printf("  [OK] Withdrawn %s. New balance: %s%n",
                formatMoney(amount), formatMoney(updated.getBalance()));
    }

    private void handleTransferMoney() {
        System.out.println(THIN_LINE);
        System.out.println("  TRANSFER MONEY");
        System.out.println(THIN_LINE);
        long       fromId  = input.readPositiveLong("  From deposit ID : ");
        long       toId    = input.readPositiveLong("  To deposit ID   : ");
        BigDecimal amount  = input.readPositiveAmount("  Amount          : ");

        depositService.transferMoney(fromId, toId, amount);

        Deposit from = depositService.getDepositById(fromId);
        Deposit to   = depositService.getDepositById(toId);
        System.out.println();
        System.out.printf("  [OK] Transferred %s.%n", formatMoney(amount));
        System.out.printf("       From deposit #%d -> new balance: %s%n",
                fromId, formatMoney(from.getBalance()));
        System.out.printf("       To   deposit #%d -> new balance: %s%n",
                toId, formatMoney(to.getBalance()));
    }

    // =========================================================================
    // Simulation handlers
    // =========================================================================

    /**
     * Simulate the next month on a deposit: accrues monthly interest,
     * records an INTEREST transaction, advances the virtual date.
     */
    private void handleSimulateDeposit() {
        System.out.println(THIN_LINE);
        System.out.println("  SIMULATE NEXT MONTH");
        System.out.println(THIN_LINE);
        long depositId = input.readPositiveLong("  Deposit ID : ");

        Deposit updated = depositService.simulateMonth(depositId);

        // Re-fetch last INTEREST transaction for display
        List<Transaction> txList = transactionService.getTransactionsByDepositId(depositId);
        BigDecimal lastInterest = txList.stream()
                .filter(t -> t.getType() == kz.bank.model.TransactionType.INTEREST)
                .reduce((first, second) -> second)   // last element
                .map(Transaction::getAmount)
                .orElse(BigDecimal.ZERO);

        System.out.println();
        System.out.printf("  [OK] Month %d/%d simulated.%n",
                updated.getSimulatedMonths(), updated.getTermMonths());
        System.out.printf("       Simulated date  : %s%n", updated.getSimulatedDate());
        System.out.printf("       Interest accrued: +%s%n", formatMoney(lastInterest));
        System.out.printf("       Total accrued   : %s%n", formatMoney(updated.getAccruedInterest()));
        System.out.printf("       New balance     : %s%n", formatMoney(updated.getBalance()));

        if (updated.getSimulatedMonths() == updated.getTermMonths()) {
            System.out.println();
            System.out.println("  *** Deposit has reached full maturity! ***");
            System.out.println("      You may now withdraw the funds normally.");
        }
    }

    /**
     * Early-close a deposit: forfeits all accrued interest (it burns),
     * marks the deposit as closed. Principal stays in balance.
     */
    private void handleEarlyCloseDeposit() {
        System.out.println(THIN_LINE);
        System.out.println("  EARLY CLOSE DEPOSIT");
        System.out.println(THIN_LINE);
        long depositId = input.readPositiveLong("  Deposit ID : ");

        // Show current state before closing
        Deposit before = depositService.getDepositById(depositId);
        System.out.println();
        System.out.printf("  Current state  : %d/%d months simulated%n",
                before.getSimulatedMonths(), before.getTermMonths());
        System.out.printf("  Accrued reward : %s  (will be FORFEITED)%n",
                formatMoney(before.getAccruedInterest()));
        System.out.printf("  Balance before : %s%n", formatMoney(before.getBalance()));

        // Confirm
        System.out.println();
        System.out.println("  WARNING: All accrued interest will be burned!");
        String confirm = input.readString("  Type YES to confirm, anything else to cancel: ");
        if (!confirm.equalsIgnoreCase("YES")) {
            System.out.println("  [CANCELLED] Early close was not performed.");
            return;
        }

        Deposit closed = depositService.earlyClose(depositId);
        System.out.println();
        System.out.println("  [OK] Deposit closed early.");
        System.out.printf("       Interest burned: %s%n",
                formatMoney(before.getAccruedInterest()));
        System.out.printf("       Remaining balance: %s%n", formatMoney(closed.getBalance()));
        System.out.println("       Status: CLOSED");
    }

    // =========================================================================
    // Transaction handlers
    // =========================================================================

    private void handleShowTransactions() {
        System.out.println(THIN_LINE);
        System.out.println("  TRANSACTIONS BY DEPOSIT");
        System.out.println(THIN_LINE);
        long depositId = input.readPositiveLong("  Deposit ID: ");

        Deposit deposit = depositService.getDepositById(depositId);
        List<Transaction> txList = transactionService.getTransactionsByDepositId(depositId);

        System.out.println();
        System.out.printf("  Deposit: %s  [%s]  Balance: %s  Rate: %.2f%%  Term: %d mo%n",
                deposit.getDepositNumber(),
                deposit.getDepositType(),
                formatMoney(deposit.getBalance()),
                deposit.getRate(),
                deposit.getTermMonths());
        System.out.println(THIN_LINE);

        if (txList.isEmpty()) {
            System.out.println("  (no transactions for this deposit)");
        } else {
            txList.forEach(tx -> System.out.println("  " + tx));
        }
    }

    private void handleShowAllTransactions() {
        System.out.println(THIN_LINE);
        System.out.println("  ALL TRANSACTIONS");
        System.out.println(THIN_LINE);
        List<Transaction> txList = transactionService.getAllTransactions();
        if (txList.isEmpty()) {
            System.out.println("  (no transactions yet)");
        } else {
            txList.stream()
                    .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                    .forEach(tx -> System.out.println("  " + tx));
        }
    }

    // =========================================================================
    // Pretty-print helpers
    // =========================================================================

    private void printCustomer(Customer c) {
        System.out.printf("  ID: %-4d | Name: %s %s | Code: %s%n",
                c.getId(), c.getFirstName(), c.getLastName(), c.getUniqueCode());
    }

    private void printAccount(Account a) {
        System.out.printf("  ID: %-4d | Number: %-14s | Customer ID: %d%n",
                a.getId(), a.getAccountNumber(), a.getCustomerId());
    }

    private void printDeposit(Deposit d) {
        System.out.printf(
                "  ID: %-4d | %-13s | %-12s | Rate: %5.2f%% | Term: %2d mo | " +
                "Open: %s | End: %s | Simulated: %d/%d mo | " +
                "Balance: %s | Accrued: %s | %s%n",
                d.getId(),
                d.getDepositNumber(),
                d.getDepositType(),
                d.getRate(),
                d.getTermMonths(),
                d.getOpenDate(),
                d.getEndDate(),
                d.getSimulatedMonths(),
                d.getTermMonths(),
                formatMoney(d.getBalance()),
                formatMoney(d.getAccruedInterest()),
                d.isClosed() ? "CLOSED" : "ACTIVE");
    }

    private String formatMoney(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
