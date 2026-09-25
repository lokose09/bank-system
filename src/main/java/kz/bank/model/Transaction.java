package kz.bank.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Immutable record of a single financial operation on a deposit.
 * Created automatically whenever a balance changes.
 */
public class Transaction {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Long id;
    private Long depositId;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private TransactionType type;

    public Transaction(Long id, Long depositId, BigDecimal amount,
                       LocalDateTime timestamp, TransactionType type) {
        this.id = id;
        this.depositId = depositId;
        this.amount = amount;
        this.timestamp = timestamp;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public Long getDepositId() {
        return depositId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public TransactionType getType() {
        return type;
    }

    @Override
    public String toString() {
        // amount is already signed: positive = money in, negative = money out
        String sign = amount.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return String.format("Transaction{id=%d, depositId=%d, type=%-12s, amount=%s%s, time=%s}",
                id,
                depositId,
                type,
                sign,
                amount.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                timestamp.format(FORMATTER));
    }
}
