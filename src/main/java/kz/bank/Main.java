package kz.bank;

import kz.bank.repository.AccountRepository;
import kz.bank.repository.CustomerRepository;
import kz.bank.repository.DepositRepository;
import kz.bank.repository.TransactionRepository;
import kz.bank.service.AccountService;
import kz.bank.service.CustomerService;
import kz.bank.service.DepositService;
import kz.bank.service.IdGenerator;
import kz.bank.service.TransactionService;
import kz.bank.ui.ConsoleInput;
import kz.bank.ui.ConsoleMenu;

/**
 * Application entry point.
 *
 * Responsibilities:
 *  1. Instantiate repositories (in-memory storage).
 *  2. Instantiate ID generators (one per entity).
 *  3. Wire services together via constructor injection.
 *  4. Start the console menu.
 *
 * No business logic lives here.
 */
public class Main {

    public static void main(String[] args) {

        // --- Repositories ---------------------------------------------------
        CustomerRepository    customerRepository    = new CustomerRepository();
        AccountRepository     accountRepository     = new AccountRepository();
        DepositRepository     depositRepository     = new DepositRepository();
        TransactionRepository transactionRepository = new TransactionRepository();

        // --- ID generators (separate sequence per entity) -------------------
        IdGenerator customerIdGen    = new IdGenerator();
        IdGenerator accountIdGen     = new IdGenerator();
        IdGenerator depositIdGen     = new IdGenerator();
        IdGenerator transactionIdGen = new IdGenerator();

        // --- Services -------------------------------------------------------
        CustomerService customerService = new CustomerService(
                customerRepository, customerIdGen);

        AccountService accountService = new AccountService(
                accountRepository, customerRepository, accountIdGen);

        TransactionService transactionService = new TransactionService(
                transactionRepository, transactionIdGen);

        DepositService depositService = new DepositService(
                depositRepository, accountRepository, transactionService,
                transactionRepository, depositIdGen);

        // --- Console UI -----------------------------------------------------
        ConsoleInput  consoleInput = new ConsoleInput();
        ConsoleMenu   consoleMenu  = new ConsoleMenu(
                customerService, accountService, depositService,
                transactionService, consoleInput);

        // --- Launch ---------------------------------------------------------
        consoleMenu.run();
    }
}
