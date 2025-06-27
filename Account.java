// --- 3. model/Account.java ---
package model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

// The Account class represents a bank account
public class Account {
    public enum AccountType { CHECKING, SAVINGS }

    private String id;
    private String userId;
    private AccountType type;
    private BigDecimal balance;

    // Constructor
    public Account(String id, String userId, AccountType type, BigDecimal balance) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.balance = balance;
    }

    // Constructor for creating a new account
    public Account(String userId, AccountType type, BigDecimal initialBalance) {
        this(UUID.randomUUID().toString(), userId, type, initialBalance);
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public AccountType getType() { return type; }
    public BigDecimal getBalance() { return balance; }

    // Setters
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Account{" +
               "id='" + id + '\'' +
               ", userId='" + userId + '\'' +
               ", type=" + type +
               ", balance=" + balance +
               '}';
    }
}