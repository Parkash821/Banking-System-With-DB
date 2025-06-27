// --- 5. model/LoanApplication.java ---
package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

// The LoanApplication class represents a loan application
public class LoanApplication {
    public enum LoanStatus { PENDING, APPROVED, REJECTED }

    private String id;
    private String userId;
    private BigDecimal amount;
    private LoanStatus status;
    private LocalDateTime applicationDate;
    private String reason;
    private int priorityScore; // Lower score = higher priority (e.g., based on credit, urgency)

    // Constructor
    public LoanApplication(String id, String userId, BigDecimal amount, LoanStatus status, LocalDateTime applicationDate, String reason, int priorityScore) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.applicationDate = applicationDate;
        this.reason = reason;
        this.priorityScore = priorityScore;
    }

    // Constructor for new applications (ID, date, status are generated)
    public LoanApplication(String userId, BigDecimal amount, String reason, int priorityScore) {
        this(UUID.randomUUID().toString(), userId, amount, LoanStatus.PENDING, LocalDateTime.now(), reason, priorityScore);
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public LoanStatus getStatus() { return status; }
    public LocalDateTime getApplicationDate() { return applicationDate; }
    public String getReason() { return reason; }
    public int getPriorityScore() { return priorityScore; }

    // Setters (for updating status by admin)
    public void setStatus(LoanStatus status) { this.status = status; }
    public void setPriorityScore(int priorityScore) { this.priorityScore = priorityScore; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoanApplication that = (LoanApplication) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "LoanApplication{" +
               "id='" + id + '\'' +
               ", userId='" + userId + '\'' +
               ", amount=" + amount +
               ", status=" + status +
               ", applicationDate=" + applicationDate +
               ", reason='" + reason + '\'' +
               ", priorityScore=" + priorityScore +
               '}';
    }
}