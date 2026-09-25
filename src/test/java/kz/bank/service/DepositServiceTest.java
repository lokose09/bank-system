package kz.bank.service;

import kz.bank.exception.InsufficientFundsException;
import kz.bank.exception.InvalidAmountException;
import kz.bank.exception.NotFoundException;
import kz.bank.exception.ValidationException;
import kz.bank.exception.WithdrawalNotAllowedException;
import kz.bank.model.Account;
import kz.bank.model.Deposit;
import kz.bank.model.DepositType;
import kz.bank.model.Transaction;
import kz.bank.model.TransactionType;
import kz.bank.repository.AccountRepository;
import kz.bank.repository.CustomerRepository;
import kz.bank.repository.DepositRepository;
import kz.bank.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DepositService.
 *
 * Structure:
 *  - CreateDeposit
 *  - DepositMoney
 *  - WithdrawMoney
 *  - TransferMoney
 *  - Transactions (amounts, signs, counts)
 */
@DisplayName("DepositService")
class DepositServiceTest {

    private DepositService     depositService;
    private TransactionService transactionService;
    private TransactionRepository transactionRepository;

    /** IDs seeded directly into repo so tests are independent of other services. */
    private Long accountId;          // WITHDRAWABLE deposit owner
    private Long savingsAccountId;   // SAVINGS deposit owner

    @BeforeEach
    void setUp() {
        AccountRepository     accountRepository     = new AccountRepository();
        DepositRepository     depositRepository     = new DepositRepository();
        transactionRepository = new TransactionRepository();

        transactionService = new TransactionService(transactionRepository, new IdGenerator());

        depositService = new DepositService(
                depositRepository,
                accountRepository,
                transactionService,
                new IdGenerator()
        );

        // Seed two accounts directly so tests don't rely on AccountService
        Account acc1 = new Account(1L, "ACC-TEST-0001", 1L);
        Account acc2 = new Account(2L, "ACC-TEST-0002", 2L);
        accountRepository.save(acc1);
        accountRepository.save(acc2);
        accountId        = acc1.getId();
        savingsAccountId = acc2.getId();
    }

    // =========================================================================
    // Helper: create a deposit without repeating boilerplate
    // =========================================================================

    private Deposit withdrawable() {
        return depositService.createDeposit(accountId, DepositType.WITHDRAWABLE, null);
    }

    private Deposit withdrawable(BigDecimal initial) {
        return depositService.createDeposit(accountId, DepositType.WITHDRAWABLE, initial);
    }

    private Deposit savings() {
        return depositService.createDeposit(savingsAccountId, DepositType.SAVINGS, null);
    }

    // =========================================================================
    // CREATE DEPOSIT
    // =========================================================================

    @Nested
    @DisplayName("createDeposit")
    class CreateDepositTests {

        @Test
        @DisplayName("creates deposit with ZERO balance when initial is null")
        void nullInitial_startsAtZero() {
            Deposit d = depositService.createDeposit(accountId, DepositType.WITHDRAWABLE, null);
            assertEquals(0, BigDecimal.ZERO.compareTo(d.getBalance()));
        }

        @Test
        @DisplayName("creates deposit with ZERO balance when initial is BigDecimal.ZERO")
        void zeroInitial_startsAtZero() {
            Deposit d = withdrawable(BigDecimal.ZERO);
            assertEquals(0, BigDecimal.ZERO.compareTo(d.getBalance()));
        }

        @Test
        @DisplayName("creates deposit with supplied positive initial balance")
        void positiveInitial_usedAsBalance() {
            Deposit d = withdrawable(new BigDecimal("5000"));
            assertEquals(0, new BigDecimal("5000").compareTo(d.getBalance()));
        }

        @Test
        @DisplayName("negative initial balance throws InvalidAmountException")
        void negativeInitial_throws() {
            assertThrows(InvalidAmountException.class,
                    () -> depositService.createDeposit(accountId, DepositType.WITHDRAWABLE,
                            new BigDecimal("-1")));
        }

        @Test
        @DisplayName("negative initial balance -5000 throws InvalidAmountException")
        void negativeInitialLarge_throws() {
            assertThrows(InvalidAmountException.class,
                    () -> depositService.createDeposit(accountId, DepositType.WITHDRAWABLE,
                            new BigDecimal("-5000")));
        }

        @Test
        @DisplayName("deposit number is auto-generated and not blank")
        void depositNumberIsGenerated() {
            Deposit d = withdrawable();
            assertNotNull(d.getDepositNumber());
            assertFalse(d.getDepositNumber().isBlank());
        }

        @Test
        @DisplayName("deposit number starts with DEP-")
        void depositNumberFormat() {
            Deposit d = withdrawable();
            assertTrue(d.getDepositNumber().startsWith("DEP-"),
                    "Expected DEP- prefix, got: " + d.getDepositNumber());
        }

        @Test
        @DisplayName("deposit numbers are unique across multiple deposits")
        void depositNumbersAreUnique() {
            java.util.Set<String> numbers = new java.util.HashSet<>();
            for (int i = 0; i < 10; i++) {
                Deposit d = depositService.createDeposit(accountId, DepositType.WITHDRAWABLE, null);
                assertTrue(numbers.add(d.getDepositNumber()),
                        "Duplicate deposit number: " + d.getDepositNumber());
            }
        }

        @Test
        @DisplayName("deposit type WITHDRAWABLE is stored correctly")
        void depositTypeWithdrawable() {
            Deposit d = withdrawable();
            assertEquals(DepositType.WITHDRAWABLE, d.getDepositType());
            assertTrue(d.isWithdrawable());
        }

        @Test
        @DisplayName("deposit type SAVINGS is stored correctly")
        void depositTypeSavings() {
            Deposit d = savings();
            assertEquals(DepositType.SAVINGS, d.getDepositType());
            assertFalse(d.isWithdrawable());
        }

        @Test
        @DisplayName("non-existent accountId throws NotFoundException")
        void nonExistentAccount_throws() {
            assertThrows(NotFoundException.class,
                    () -> depositService.createDeposit(999L, DepositType.WITHDRAWABLE, null));
        }

        @Test
        @DisplayName("null depositType throws ValidationException")
        void nullDepositType_throws() {
            assertThrows(ValidationException.class,
                    () -> depositService.createDeposit(accountId, null, null));
        }
    }

    // =========================================================================
    // DEPOSIT MONEY
    // =========================================================================

    @Nested
    @DisplayName("depositMoney")
    class DepositMoneyTests {

        @Test
        @DisplayName("balance increases by the deposited amount")
        void success_balanceIncreases() {
            Deposit d = withdrawable();
            depositService.depositMoney(d.getId(), new BigDecimal("1000"));
            Deposit updated = depositService.getDepositById(d.getId());
            assertEquals(0, new BigDecimal("1000").compareTo(updated.getBalance()));
        }

        @Test
        @DisplayName("multiple deposits accumulate correctly")
        void multipleDeposits_accumulate() {
            Deposit d = withdrawable();
            depositService.depositMoney(d.getId(), new BigDecimal("500"));
            depositService.depositMoney(d.getId(), new BigDecimal("300"));
            depositService.depositMoney(d.getId(), new BigDecimal("200"));
            Deposit updated = depositService.getDepositById(d.getId());
            assertEquals(0, new BigDecimal("1000").compareTo(updated.getBalance()));
        }

        @Test
        @DisplayName("allowed on SAVINGS deposit")
        void savings_depositAllowed() {
            Deposit d = savings();
            assertDoesNotThrow(() -> depositService.depositMoney(d.getId(), new BigDecimal("100")));
        }

        @Test
        @DisplayName("zero amount throws InvalidAmountException")
        void zeroAmount_throws() {
            Deposit d = withdrawable();
            assertThrows(InvalidAmountException.class,
                    () -> depositService.depositMoney(d.getId(), BigDecimal.ZERO));
        }

        @Test
        @DisplayName("negative amount throws InvalidAmountException")
        void negativeAmount_throws() {
            Deposit d = withdrawable();
            assertThrows(InvalidAmountException.class,
                    () -> depositService.depositMoney(d.getId(), new BigDecimal("-100")));
        }

        @Test
        @DisplayName("null amount throws InvalidAmountException")
        void nullAmount_throws() {
            Deposit d = withdrawable();
            assertThrows(InvalidAmountException.class,
                    () -> depositService.depositMoney(d.getId(), null));
        }

        @Test
        @DisplayName("non-existent depositId throws NotFoundException")
        void nonExistentDeposit_throws() {
            assertThrows(NotFoundException.class,
                    () -> depositService.depositMoney(999L, new BigDecimal("100")));
        }

        @Test
        @DisplayName("creates a DEPOSIT transaction record")
        void createsTransaction() {
            Deposit d = withdrawable();
            depositService.depositMoney(d.getId(), new BigDecimal("1000"));
            List<Transaction> txs = transactionService.getTransactionsByDepositId(d.getId());
            assertEquals(1, txs.size());
            assertEquals(TransactionType.DEPOSIT, txs.get(0).getType());
        }
    }

    // =========================================================================
    // WITHDRAW MONEY
    // =========================================================================

    @Nested
    @DisplayName("withdrawMoney")
    class WithdrawMoneyTests {

        @Test
        @DisplayName("balance decreases by the withdrawn amount")
        void success_balanceDecreases() {
            Deposit d = withdrawable(new BigDecimal("1000"));
            depositService.withdrawMoney(d.getId(), new BigDecimal("400"));
            Deposit updated = depositService.getDepositById(d.getId());
            assertEquals(0, new BigDecimal("600").compareTo(updated.getBalance()));
        }

        @Test
        @DisplayName("withdraw entire balance leaves zero")
        void withdrawAll_leavesZero() {
            Deposit d = withdrawable(new BigDecimal("500"));
            depositService.withdrawMoney(d.getId(), new BigDecimal("500"));
            Deposit updated = depositService.getDepositById(d.getId());
            assertEquals(0, BigDecimal.ZERO.compareTo(updated.getBalance()));
        }

        @Test
        @DisplayName("balance never goes negative after withdraw")
        void balanceNeverNegative() {
            Deposit d = withdrawable(new BigDecimal("100"));
            assertThrows(InsufficientFundsException.class,
                    () -> depositService.withdrawMoney(d.getId(), new BigDecimal("101")));
            // Balance must still be 100
            assertEquals(0, new BigDecimal("100").compareTo(
                    depositService.getDepositById(d.getId()).getBalance()));
        }

        @Test
        @DisplayName("insufficient funds throws InsufficientFundsException")
        void insufficientFunds_throws() {
            Deposit d = withdrawable(new BigDecimal("50"));
            assertThrows(InsufficientFundsException.class,
                    () -> depositService.withdrawMoney(d.getId(), new BigDecimal("100")));
        }

        @Test
        @DisplayName("withdraw from SAVINGS throws WithdrawalNotAllowedException")
        void savings_withdrawForbidden() {
            Deposit d = savings();
            depositService.depositMoney(d.getId(), new BigDecimal("500"));
            assertThrows(WithdrawalNotAllowedException.class,
                    () -> depositService.withdrawMoney(d.getId(), new BigDecimal("100")));
        }

        @Test
        @DisplayName("withdraw from SAVINGS leaves balance unchanged")
        void savings_balanceUnchangedAfterFailedWithdraw() {
            Deposit d = savings();
            depositService.depositMoney(d.getId(), new BigDecimal("500"));
            assertThrows(WithdrawalNotAllowedException.class,
                    () -> depositService.withdrawMoney(d.getId(), new BigDecimal("100")));
            assertEquals(0, new BigDecimal("500").compareTo(
                    depositService.getDepositById(d.getId()).getBalance()));
        }

        @Test
        @DisplayName("zero amount throws InvalidAmountException")
        void zeroAmount_throws() {
            Deposit d = withdrawable(new BigDecimal("100"));
            assertThrows(InvalidAmountException.class,
                    () -> depositService.withdrawMoney(d.getId(), BigDecimal.ZERO));
        }

        @Test
        @DisplayName("negative amount throws InvalidAmountException")
        void negativeAmount_throws() {
            Deposit d = withdrawable(new BigDecimal("100"));
            assertThrows(InvalidAmountException.class,
                    () -> depositService.withdrawMoney(d.getId(), new BigDecimal("-50")));
        }

        @Test
        @DisplayName("null amount throws InvalidAmountException")
        void nullAmount_throws() {
            Deposit d = withdrawable(new BigDecimal("100"));
            assertThrows(InvalidAmountException.class,
                    () -> depositService.withdrawMoney(d.getId(), null));
        }

        @Test
        @DisplayName("non-existent depositId throws NotFoundException")
        void nonExistentDeposit_throws() {
            assertThrows(NotFoundException.class,
                    () -> depositService.withdrawMoney(999L, new BigDecimal("100")));
        }

        @Test
        @DisplayName("creates a WITHDRAW transaction record")
        void createsTransaction() {
            Deposit d = withdrawable(new BigDecimal("1000"));
            depositService.withdrawMoney(d.getId(), new BigDecimal("200"));
            List<Transaction> txs = transactionService.getTransactionsByDepositId(d.getId());
            assertEquals(1, txs.size());
            assertEquals(TransactionType.WITHDRAW, txs.get(0).getType());
        }
    }

    // =========================================================================
    // TRANSFER MONEY
    // =========================================================================

    @Nested
    @DisplayName("transferMoney")
    class TransferMoneyTests {

        @Test
        @DisplayName("sender balance decreases by transferred amount")
        void success_senderBalanceDecreases() {
            Deposit from = withdrawable(new BigDecimal("1000"));
            Deposit to   = savings();
            depositService.transferMoney(from.getId(), to.getId(), new BigDecimal("300"));
            assertEquals(0, new BigDecimal("700").compareTo(
                    depositService.getDepositById(from.getId()).getBalance()));
        }

        @Test
        @DisplayName("receiver balance increases by transferred amount")
        void success_receiverBalanceIncreases() {
            Deposit from = withdrawable(new BigDecimal("1000"));
            Deposit to   = savings();
            depositService.transferMoney(from.getId(), to.getId(), new BigDecimal("300"));
            assertEquals(0, new BigDecimal("300").compareTo(
                    depositService.getDepositById(to.getId()).getBalance()));
        }

        @Test
        @DisplayName("full demo scenario: +100000, -20000, transfer 30000 → from=50000, to=30000")
        void demoScenario() {
            Deposit from = withdrawable();
            Deposit to   = savings();

            depositService.depositMoney(from.getId(), new BigDecimal("100000"));
            depositService.withdrawMoney(from.getId(), new BigDecimal("20000"));
            depositService.transferMoney(from.getId(), to.getId(), new BigDecimal("30000"));

            assertEquals(0, new BigDecimal("50000").compareTo(
                    depositService.getDepositById(from.getId()).getBalance()));
            assertEquals(0, new BigDecimal("30000").compareTo(
                    depositService.getDepositById(to.getId()).getBalance()));
        }

        @Test
        @DisplayName("creates TRANSFER_OUT transaction for sender")
        void createsTransferOut() {
            Deposit from = withdrawable(new BigDecimal("500"));
            Deposit to   = savings();
            depositService.transferMoney(from.getId(), to.getId(), new BigDecimal("200"));

            List<Transaction> txs = transactionService.getTransactionsByDepositId(from.getId());
            assertEquals(1, txs.size());
            assertEquals(TransactionType.TRANSFER_OUT, txs.get(0).getType());
        }

        @Test
        @DisplayName("creates TRANSFER_IN transaction for receiver")
        void createsTransferIn() {
            Deposit from = withdrawable(new BigDecimal("500"));
            Deposit to   = savings();
            depositService.transferMoney(from.getId(), to.getId(), new BigDecimal("200"));

            List<Transaction> txs = transactionService.getTransactionsByDepositId(to.getId());
            assertEquals(1, txs.size());
            assertEquals(TransactionType.TRANSFER_IN, txs.get(0).getType());
        }

        @Test
        @DisplayName("transfer to self throws ValidationException without changing balance")
        void transferToSelf_throws() {
            Deposit d = withdrawable(new BigDecimal("1000"));
            assertThrows(ValidationException.class,
                    () -> depositService.transferMoney(d.getId(), d.getId(), new BigDecimal("100")));
            // Balance must be unchanged
            assertEquals(0, new BigDecimal("1000").compareTo(
                    depositService.getDepositById(d.getId()).getBalance()));
        }

        @Test
        @DisplayName("insufficient funds: no balance changes on either side")
        void insufficientFunds_noBothBalancesUnchanged() {
            Deposit from = withdrawable(new BigDecimal("100"));
            Deposit to   = savings();
            assertThrows(InsufficientFundsException.class,
                    () -> depositService.transferMoney(from.getId(), to.getId(),
                            new BigDecimal("999")));
            assertEquals(0, new BigDecimal("100").compareTo(
                    depositService.getDepositById(from.getId()).getBalance()));
            assertEquals(0, BigDecimal.ZERO.compareTo(
                    depositService.getDepositById(to.getId()).getBalance()));
        }

        @Test
        @DisplayName("insufficient funds: no transactions created")
        void insufficientFunds_noTransactions() {
            Deposit from = withdrawable(new BigDecimal("100"));
            Deposit to   = savings();
            assertThrows(InsufficientFundsException.class,
                    () -> depositService.transferMoney(from.getId(), to.getId(),
                            new BigDecimal("999")));
            assertTrue(transactionService.getTransactionsByDepositId(from.getId()).isEmpty());
            assertTrue(transactionService.getTransactionsByDepositId(to.getId()).isEmpty());
        }

        @Test
        @DisplayName("source is SAVINGS: throws WithdrawalNotAllowedException, no balance changes")
        void sourceSavings_throws() {
            Deposit from = savings();
            depositService.depositMoney(from.getId(), new BigDecimal("500"));
            Deposit to = withdrawable();
            assertThrows(WithdrawalNotAllowedException.class,
                    () -> depositService.transferMoney(from.getId(), to.getId(),
                            new BigDecimal("100")));
            // Balances unchanged
            assertEquals(0, new BigDecimal("500").compareTo(
                    depositService.getDepositById(from.getId()).getBalance()));
            assertEquals(0, BigDecimal.ZERO.compareTo(
                    depositService.getDepositById(to.getId()).getBalance()));
        }

        @Test
        @DisplayName("zero amount throws InvalidAmountException")
        void zeroAmount_throws() {
            Deposit from = withdrawable(new BigDecimal("500"));
            Deposit to   = savings();
            assertThrows(InvalidAmountException.class,
                    () -> depositService.transferMoney(from.getId(), to.getId(), BigDecimal.ZERO));
        }

        @Test
        @DisplayName("negative amount throws InvalidAmountException")
        void negativeAmount_throws() {
            Deposit from = withdrawable(new BigDecimal("500"));
            Deposit to   = savings();
            assertThrows(InvalidAmountException.class,
                    () -> depositService.transferMoney(from.getId(), to.getId(),
                            new BigDecimal("-100")));
        }

        @Test
        @DisplayName("null amount throws InvalidAmountException")
        void nullAmount_throws() {
            Deposit from = withdrawable(new BigDecimal("500"));
            Deposit to   = savings();
            assertThrows(InvalidAmountException.class,
                    () -> depositService.transferMoney(from.getId(), to.getId(), null));
        }

        @Test
        @DisplayName("non-existent source deposit throws NotFoundException")
        void nonExistentSource_throws() {
            Deposit to = savings();
            assertThrows(NotFoundException.class,
                    () -> depositService.transferMoney(999L, to.getId(), new BigDecimal("100")));
        }

        @Test
        @DisplayName("non-existent target deposit throws NotFoundException")
        void nonExistentTarget_throws() {
            Deposit from = withdrawable(new BigDecimal("500"));
            assertThrows(NotFoundException.class,
                    () -> depositService.transferMoney(from.getId(), 999L, new BigDecimal("100")));
        }
    }

    // =========================================================================
    // TRANSACTION RECORDS — amount, sign, timestamp, depositId
    // =========================================================================

    @Nested
    @DisplayName("Transaction records")
    class TransactionRecordTests {

        @Test
        @DisplayName("DEPOSIT transaction has positive amount")
        void deposit_positiveAmount() {
            Deposit d = withdrawable();
            depositService.depositMoney(d.getId(), new BigDecimal("1000"));

            Transaction tx = transactionService.getTransactionsByDepositId(d.getId()).get(0);
            assertEquals(TransactionType.DEPOSIT, tx.getType());
            assertTrue(tx.getAmount().compareTo(BigDecimal.ZERO) > 0,
                    "DEPOSIT amount should be positive, was: " + tx.getAmount());
        }

        @Test
        @DisplayName("WITHDRAW transaction has negative amount")
        void withdraw_negativeAmount() {
            Deposit d = withdrawable(new BigDecimal("1000"));
            depositService.withdrawMoney(d.getId(), new BigDecimal("400"));

            Transaction tx = transactionService.getTransactionsByDepositId(d.getId()).get(0);
            assertEquals(TransactionType.WITHDRAW, tx.getType());
            assertTrue(tx.getAmount().compareTo(BigDecimal.ZERO) < 0,
                    "WITHDRAW amount should be negative, was: " + tx.getAmount());
        }

        @Test
        @DisplayName("TRANSFER_OUT transaction has negative amount")
        void transferOut_negativeAmount() {
            Deposit from = withdrawable(new BigDecimal("500"));
            Deposit to   = savings();
            depositService.transferMoney(from.getId(), to.getId(), new BigDecimal("200"));

            Transaction tx = transactionService.getTransactionsByDepositId(from.getId()).get(0);
            assertEquals(TransactionType.TRANSFER_OUT, tx.getType());
            assertTrue(tx.getAmount().compareTo(BigDecimal.ZERO) < 0,
                    "TRANSFER_OUT amount should be negative, was: " + tx.getAmount());
        }

        @Test
        @DisplayName("TRANSFER_IN transaction has positive amount")
        void transferIn_positiveAmount() {
            Deposit from = withdrawable(new BigDecimal("500"));
            Deposit to   = savings();
            depositService.transferMoney(from.getId(), to.getId(), new BigDecimal("200"));

            Transaction tx = transactionService.getTransactionsByDepositId(to.getId()).get(0);
            assertEquals(TransactionType.TRANSFER_IN, tx.getType());
            assertTrue(tx.getAmount().compareTo(BigDecimal.ZERO) > 0,
                    "TRANSFER_IN amount should be positive, was: " + tx.getAmount());
        }

        @Test
        @DisplayName("transaction amount absolute value equals the operated amount")
        void transactionAmount_matchesOperation() {
            BigDecimal amount = new BigDecimal("350");
            Deposit d = withdrawable(new BigDecimal("1000"));
            depositService.withdrawMoney(d.getId(), amount);

            Transaction tx = transactionService.getTransactionsByDepositId(d.getId()).get(0);
            assertEquals(0, amount.compareTo(tx.getAmount().abs()));
        }

        @Test
        @DisplayName("transaction timestamp is not null")
        void transaction_timestampNotNull() {
            Deposit d = withdrawable();
            depositService.depositMoney(d.getId(), new BigDecimal("100"));

            Transaction tx = transactionService.getTransactionsByDepositId(d.getId()).get(0);
            assertNotNull(tx.getTimestamp());
        }

        @Test
        @DisplayName("transaction depositId matches the deposit it was recorded for")
        void transaction_depositIdIsCorrect() {
            Deposit d = withdrawable();
            depositService.depositMoney(d.getId(), new BigDecimal("100"));

            Transaction tx = transactionService.getTransactionsByDepositId(d.getId()).get(0);
            assertEquals(d.getId(), tx.getDepositId());
        }

        @Test
        @DisplayName("after deposit+withdraw+transfer: exactly 3 transactions for sender, 1 for receiver")
        void fullScenario_transactionCounts() {
            Deposit from = withdrawable();
            Deposit to   = savings();

            depositService.depositMoney(from.getId(), new BigDecimal("100000"));  // tx1 for from
            depositService.withdrawMoney(from.getId(), new BigDecimal("20000"));  // tx2 for from
            depositService.transferMoney(from.getId(), to.getId(),
                    new BigDecimal("30000"));                                       // tx3 for from, tx1 for to

            List<Transaction> fromTxs = transactionService.getTransactionsByDepositId(from.getId());
            List<Transaction> toTxs   = transactionService.getTransactionsByDepositId(to.getId());

            assertEquals(3, fromTxs.size());
            assertEquals(1, toTxs.size());

            assertEquals(TransactionType.DEPOSIT,      fromTxs.get(0).getType());
            assertEquals(TransactionType.WITHDRAW,     fromTxs.get(1).getType());
            assertEquals(TransactionType.TRANSFER_OUT, fromTxs.get(2).getType());
            assertEquals(TransactionType.TRANSFER_IN,  toTxs.get(0).getType());
        }
    }
}
