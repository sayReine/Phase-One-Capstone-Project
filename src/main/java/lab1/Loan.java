package lab1;

import java.time.LocalDateTime;

public class Loan {
    private String id;
    private String customerId;
    private double amount;
    private String status; // PENDING, APPROVED, REJECTED
    private LocalDateTime requestedAt;

    public Loan(String customerId, double amount) {
        this.id          = java.util.UUID.randomUUID().toString();
        this.customerId  = customerId;
        this.amount      = amount;
        this.status      = "PENDING";
        this.requestedAt = LocalDateTime.now();
    }

    public Loan(String id, String customerId, double amount, String status, LocalDateTime requestedAt) {
        this.id          = id;
        this.customerId  = customerId;
        this.amount      = amount;
        this.status      = status;
        this.requestedAt = requestedAt;
    }

    public String getId()               { return id; }
    public String getCustomerId()       { return customerId; }
    public double getAmount()           { return amount; }
    public String getStatus()           { return status; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setStatus(String status) { this.status = status; }
}
