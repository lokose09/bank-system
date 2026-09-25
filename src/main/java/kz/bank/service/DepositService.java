package kz.bank.service;

import kz.bank.exception.InsufficientFundsException;
import kz.bank.exception.InvalidAmountException;
import kz.bank.exception.NotFoundException;
import kz.bank.exception.ValidationException;
import kz.bank.exception.WithdrawalNotAllowedException;
import kz.bank.model.Deposit;
import kz.bank.model.DepositType;
import kz.bank.model.TransactionType;
import kz.bank.repository.AccountRepository;
import kz.bank.repository.DepositRepository;
import kz.bank.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Core business logic: deposit creation and all financial operations.
 *
 * New operations added:
 *  - createDeposit  accepts rate, termMonths, openDate
 *  - simulateMonth  advances the virtual date by 1 month and accrues monthly interest
 *  - earlyClose     zeroes out all accrued interest and marks the deposit as closed
 *
 * Rules:
 *  - Amount must be > 0 for every financial operation.
 *  - SAVINGS deposits do not allow withdrawals or outgoing transfers.
 *  - Balance never goes below zero.
 *  - Every successful balance change produces a Transaction record.
 *  - transferMoney is atomic: all validations run before any balance changes.
 *  - Interest is calculated as:  balance * (rate / 100) / 12  (simple monthly interest).
 *  - Each monthly interest accrual creates its own INTEREST transaction with a unique ID.
 *  - On early close all accrued interest is reversed (subtracted from balance) and the
 *    deposit is marked closed. Burn amount = accruedInterest accumulated so far.
 */
public class DepositService {

    private final DepositRepository     depositRepository;
    private final AccountRepository     accountRepository;
    private final TransactionService    transactionService;
    private final TransactionRepository transactionRepository;
    private final IdGenerator           idGenerator;

    public DepositService(DepositRepository depositRepository,
                          AccountRepository accountRepository,
                          TransactionService transactionService,
                          TransactionRepository transactionRepository,
                          IdGenerator idGenerator) {
        this.depositRepository     = depositRepository;
        this.accountRepository     = accountRepository;
        this.transactionService    = transactionService;
        this.transactionRepository = transactionRepository;
        this.idGenerator           = idGenerator;
    }

    // =========================================================================
    // CREATE
    // =========================================================================

    /**
     * Create a new deposit with an interest rate and a fixed term.
     *
     * @param accountId   ID of an existing account
     * @param depositType WITHDRAWABLE or SAVINGS
     * @param initial     optional initial balance (null / ZERO = start empty)
     * @param rate        annual interest rate in percent, e.g. 17.0, 17.5, 19.0, 21.0, 22.3
     * @param termMonths  deposit term: 3, 6, 9, or 12 months
     * @param openDate    date the deposit is opened (endDate is calculated automatically)
     * @return the newly created Deposit
     */
    public Deposit createDeposit(Long accountId, DepositType depositType,
                                 BigDecimal initial, BigDecimal rate,
                                 int termMonths, LocalDate openDate) {
        validateId(accountId, "Account ID");

        if (depositType == null) {
            throw new ValidationException("Deposit type must not be null.");
        }
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Interest rate must be a positive number.");
        }
        if (termMonths != 3 && termMonths != 6 && termMonths != 9 && termMonths != 12) {
            throw new ValidationException("Term must be 3, 6, 9, or 12 months.");
        }
        if (openDate == null) {
            throw new ValidationException("Open date must not be null.");
        }

        accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account", accountId));

        if (initial != null && initial.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAmountException(
                    "Initial balance must not be negative, but got: " + initial.toPlainString());
        }
        BigDecimal startBalance = (initial == null) ? BigDecimal.ZERO : initial;

        String depositNumber = generateUniqueDepositNumber();

        Deposit deposit = new Deposit(
                idGenerator.next(),
                accountId,
                depositNumber,
                startBalance,
                depositType,
                rate,
                termMonths,
                openDate
        );
        return depositRepository.save(deposit);
    }

    // =========================================================================
    // READ
    // =========================================================================

    public Deposit getDepositById(Long id) {
        validateId(id, "Deposit ID");
        return depositRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Deposit", id));
    }

    public List<Deposit> getAllDeposits() {
        return depositRepository.findAll();
    }

    public List<Deposit> getDepositsByAccountId(Long accountId) {
        validateId(accountId, "Account ID");
        return depositRepository.findByAccountId(accountId);
    }

    // =========================================================================
    // SIMULATION
    // =========================================================================

    /**
     * Simulate one calendar month for the given deposit.
     *
     * What happens:
     *  1. Deposit must not be closed.
     *  2. simulatedMonths must be < termMonths (can't simulate past the end date).
     *  3. Monthly interest = balance * (rate / 100) / 12, rounded to 2 dp.
     *  4. Interest is added to the balance.
     *  5. An INTEREST transaction is created with a unique auto-assigned ID.
     *  6. simulatedDate advances by 1 month.
     *  7. simulatedMonths increments by 1.
     *  8. accruedInterest accumulates.
     *  9. If simulatedMonths == termMonths after increment the deposit has reached maturity
     *     (it stays open — the client can then choose to withdraw normally).
     *
     * @param depositId deposit to advance
     * @return the Deposit after the interest has been applied
     */
    public Deposit simulateMonth(Long depositId) {
        validateId(depositId, "Deposit ID");

        Deposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new NotFoundException("Deposit", depositId));

        if (deposit.isClosed()) {
            throw new ValidationException(
                    "Deposit " + deposit.getDepositNumber() + " is already closed.");
        }
        if (deposit.getSimulatedMonths() >= deposit.getTermMonths()) {
            throw new ValidationException(
                    "Deposit " + deposit.getDepositNumber() + " has already completed its full term ("
                    + deposit.getTermMonths() + " months). No further simulation is possible.");
        }

        // Monthly interest = principal * (annualRate / 100) / 12
        BigDecimal monthlyInterest = deposit.getBalance()
                .multiply(deposit.getRate())
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        // Apply interest to balance
        deposit.setBalance(deposit.getBalance().add(monthlyInterest));

        // Track cumulative accrued interest
        deposit.setAccruedInterest(deposit.getAccruedInterest().add(monthlyInterest));

        // Advance virtual date and counter
        deposit.setSimulatedDate(deposit.getSimulatedDate().plusMonths(1));
        deposit.setSimulatedMonths(deposit.getSimulatedMonths() + 1);

        depositRepository.save(deposit);

        // Record as a distinct INTEREST transaction with its own unique ID
        transactionService.record(depositId, monthlyInterest, TransactionType.INTEREST);

        return deposit;
    }

    /**
     * Close the deposit early.
     *
     * Rules:
     *  - The deposit must not already be closed.
     *  - All interest accrued during simulation is forfeited (burned):
     *    accruedInterest is subtracted from balance and set to ZERO.
     *  - The deposit is marked as closed = true.
     *  - The principal (balance minus accrued interest) remains untouched and
     *    can be withdrawn normally after closing if the type allows it.
     *
     * @param depositId deposit to close early
     * @return the Deposit after penalty has been applied
     */
    public Deposit earlyClose(Long depositId) {
        validateId(depositId, "Deposit ID");

        Deposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new NotFoundException("Deposit", depositId));

        if (deposit.isClosed()) {
            throw new ValidationException(
                    "Deposit " + deposit.getDepositNumber() + " is already closed.");
        }

        BigDecimal accrued = deposit.getAccruedInterest();

        if (accrued.compareTo(BigDecimal.ZERO) > 0) {
            // Burn all accrued interest: subtract from balance
            deposit.setBalance(deposit.getBalance().subtract(accrued));
            if (deposit.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                // Safety net: can't go negative (shouldn't happen in practice)
                deposit.setBalance(BigDecimal.ZERO);
            }
        }

        // Reset accrued interest to zero
        deposit.setAccruedInterest(BigDecimal.ZERO);
        deposit.setClosed(true);

        depositRepository.save(deposit);
        return deposit;
    }

    // =========================================================================
    // FINANCIAL OPERATIONS
    // =========================================================================

    /**
     * Add funds to a deposit.
     * Allowed for both WITHDRAWABLE and SAVINGS deposits.
     * Not allowed on closed deposits.
     */
    public Deposit depositMoney(Long depositId, BigDecimal amount) {
        validateId(depositId, "Deposit ID");
        validatePositiveAmount(amount);

        Deposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new NotFoundException("Deposit", depositId));

        if (deposit.isClosed()) {
            throw new ValidationException(
                    "Cannot deposit to a closed deposit: " + deposit.getDepositNumber());
        }

        deposit.setBalance(deposit.getBalance().add(amount));
        depositRepository.save(deposit);

        transactionService.record(depositId, amount, TransactionType.DEPOSIT);
        return deposit;
    }

    /**
     * Withdraw funds from a deposit.
     * Only allowed for WITHDRAWABLE deposits and not on closed deposits.
     */
    public Deposit withdrawMoney(Long depositId, BigDecimal amount) {
        validateId(depositId, "Deposit ID");
        validatePositiveAmount(amount);

        Deposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new NotFoundException("Deposit", depositId));

        if (deposit.isClosed()) {
            throw new ValidationException(
                    "Cannot withdraw from a closed deposit: " + deposit.getDepositNumber());
        }

        checkWithdrawalAllowed(deposit);
        checkSufficientFunds(deposit, amount);

        deposit.setBalance(deposit.getBalance().subtract(amount));
        depositRepository.save(deposit);

        transactionService.record(depositId, amount, TransactionType.WITHDRAW);
        return deposit;
    }

    /**
     * Transfer funds between two deposits.
     * Source must be WITHDRAWABLE and not closed.
     * Two transactions are recorded: TRANSFER_OUT and TRANSFER_IN.
     */
    public void transferMoney(Long fromDepositId, Long toDepositId, BigDecimal amount) {
        validateId(fromDepositId, "Source deposit ID");
        validateId(toDepositId, "Target deposit ID");
        validatePositiveAmount(amount);

        if (fromDepositId.equals(toDepositId)) {
            throw new ValidationException("Source and target deposits must be different.");
        }

        Deposit from = depositRepository.findById(fromDepositId)
                .orElseThrow(() -> new NotFoundException("Source deposit", fromDepositId));
        Deposit to = depositRepository.findById(toDepositId)
                .orElseThrow(() -> new NotFoundException("Target deposit", toDepositId));

        if (from.isClosed()) {
            throw new ValidationException(
                    "Cannot transfer from a closed deposit: " + from.getDepositNumber());
        }

        checkWithdrawalAllowed(from);
        checkSufficientFunds(from, amount);

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        depositRepository.save(from);
        depositRepository.save(to);

        transactionService.record(fromDepositId, amount, TransactionType.TRANSFER_OUT);
        transactionService.record(toDepositId, amount, TransactionType.TRANSFER_IN);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void checkWithdrawalAllowed(Deposit deposit) {
        if (!deposit.isWithdrawable()) {
            throw new WithdrawalNotAllowedException(deposit.getDepositNumber());
        }
    }

    private void checkSufficientFunds(Deposit deposit, BigDecimal amount) {
        if (deposit.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(deposit.getBalance(), amount);
        }
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException(amount);
        }
    }

    private void validateId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new ValidationException(fieldName + " must be a positive number.");
        }
    }

    /**
     * Generates a formatted deposit number like DEP-7C3B-F190.
     * Retries until unique (collisions are astronomically unlikely).
     */
    private String generateUniqueDepositNumber() {
        String number;
        do {
            String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
            number = "DEP-" + uuid.substring(0, 4) + "-" + uuid.substring(4, 8);
        } while (depositRepository.existsByDepositNumber(number));
        return number;
    }
}
