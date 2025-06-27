// --- 4. model/Transaction.java ---
package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

// The Transaction class represents a financial transaction
public class Transaction {
    public enum TransactionType { DEPOSIT, WITHDRAWAL, TRANSFER_OUT, TRANSFER_IN }

    private String id;
    private String accountId; // Account initiating or receiving
    private String counterpartyAccountId; // For transfers
    private BigDecimal amount;
    private TransactionType type;
    private LocalDateTime timestamp;
    private String description;

    // Constructor
    public Transaction(String id, String accountId, String counterpartyAccountId, BigDecimal amount, TransactionType type, LocalDateTime timestamp, String description) {
        this.id = id;
        this.accountId = accountId;
        this.counterpartyAccountId = counterpartyAccountId;
        this.amount = amount;
        this.type = type;
        this.timestamp = timestamp;
        this.description = description;
    }

    // Constructor for new transactions (ID and timestamp are generated)
    public Transaction(String accountId, String counterpartyAccountId, BigDecimal amount, TransactionType type, String description) {
        this(UUID.randomUUID().toString(), accountId, counterpartyAccountId, amount, type, LocalDateTime.now(), description);
    }

    // Getters
    public String getId() { return id; }
    public String getAccountId() { return accountId; }
    public String getCounterpartyAccountId() { return counterpartyAccountId; }
    public BigDecimal getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getDescription() { return description; }

    // Setters (for completeness, though transactions are usually immutable)
    public void setDescription(String description) { this.description = description; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String counterpartyInfo = (counterpartyAccountId != null && !counterpartyAccountId.isEmpty()) ? ", Counterparty: " + counterpartyAccountId : "";
        return "Transaction{" +
               "id='" + id + '\'' +
               ", Account='" + accountId + '\'' +
               counterpartyInfo +
               ", Amount=" + amount +
               ", Type=" + type +
               ", Timestamp=" + timestamp.format(formatter) +
               ", Desc='" + description + '\'' +
               '}';
    }
}