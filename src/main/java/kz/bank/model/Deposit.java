package kz.bank.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a deposit (sub-account) attached to an Account.
 * A single account may hold multiple deposits.
 * Balance is always >= 0 and stored as BigDecimal.
 *
 * New fields:
 *  - rate           : annual interest rate in percent (e.g. 17.0, 17.5, 19.0, 21.0, 22.3)
 *  - termMonths     : deposit term in months (3, 6, 9, 12)
 *  - openDate       : date when the deposit was opened
 *  - endDate        : calculated automatically as openDate + termMonths
 *  - simulatedDate  : virtual "current date" advanced one month per simulation step
 *  - simulatedMonths: how many months have already been simulated
 *  - accruedInterest: total interest accumulated during simulation (resets on early close)
 *  - closed         : true when the deposit has been closed (early or at maturity)
 */
public class Deposit {

    private Long id;
    private Long accountId;
    private String depositNumber;
    private BigDecimal balance;
    private DepositType depositType;

    // ── New fields ──────────────────────────────────────────────────────────
    private BigDecimal rate;           // annual rate, e.g. 17.50
    private int        termMonths;     // 3 / 6 / 9 / 12
    private LocalDate  openDate;       // set at creation
    private LocalDate  endDate;        // openDate + termMonths
    private LocalDate  simulatedDate;  // advances with each simulateMonth() call
    private int        simulatedMonths; // number of months already simulated
    private BigDecimal accruedInterest; // sum of all INTEREST transactions so far
    private boolean    closed;          // deposit is no longer active

    public Deposit(Long id, Long accountId, String depositNumber,
                   BigDecimal balance, DepositType depositType,
                   BigDecimal rate, int termMonths, LocalDate openDate) {
        this.id              = id;
        this.accountId       = accountId;
        this.depositNumber   = depositNumber;
        this.balance         = balance;
        this.depositType     = depositType;

        this.rate            = rate;
        this.termMonths      = termMonths;
        this.openDate        = openDate;
        this.endDate         = openDate.plusMonths(termMonths);   // auto-calculated
        this.simulatedDate   = openDate;
        this.simulatedMonths = 0;
        this.accruedInterest = BigDecimal.ZERO;
        this.closed          = false;
    }

    // ── Existing getters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public Long getAccountId() { return accountId; }

    public String getDepositNumber() { return depositNumber; }

    public BigDecimal getBalance() { return balance; }

    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public DepositType getDepositType() { return depositType; }

    /** Convenience: returns true when withdrawals are permitted on this deposit. */
    public boolean isWithdrawable() {
        return depositType == DepositType.WITHDRAWABLE;
    }

    // ── New getters / setters ───────────────────────────────────────────────

    public BigDecimal getRate() { return rate; }

    public int getTermMonths() { return termMonths; }

    public LocalDate getOpenDate() { return openDate; }

    public LocalDate getEndDate() { return endDate; }

    public LocalDate getSimulatedDate() { return simulatedDate; }

    public void setSimulatedDate(LocalDate simulatedDate) { this.simulatedDate = simulatedDate; }

    public int getSimulatedMonths() { return simulatedMonths; }

    public void setSimulatedMonths(int simulatedMonths) { this.simulatedMonths = simulatedMonths; }

    public BigDecimal getAccruedInterest() { return accruedInterest; }

    public void setAccruedInterest(BigDecimal accruedInterest) { this.accruedInterest = accruedInterest; }

    public boolean isClosed() { return closed; }

    public void setClosed(boolean closed) { this.closed = closed; }

    // ── toString ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format(
                "Deposit{id=%d, number='%s', type=%s, rate=%.2f%%, term=%d mo, " +
                "open=%s, end=%s, simulatedDate=%s, simulatedMonths=%d, " +
                "balance=%s, accrued=%s, closed=%s, accountId=%d}",
                id, depositNumber, depositType, rate, termMonths,
                openDate, endDate, simulatedDate, simulatedMonths,
                balance.toPlainString(), accruedInterest.toPlainString(), closed, accountId);
    }
}
