package lab1;

import java.time.LocalDateTime;

public class Transaction {
    private String id;
    private String accountId;
    private String referenceId;
    private double amount;
    private String type;
    private LocalDateTime timestamp;

    // Used when creating a NEW transaction
    public Transaction(String accountId, String referenceId, double amount, String type) {
        this.id          = java.util.UUID.randomUUID().toString();
        this.accountId   = accountId;
        this.referenceId = referenceId;
        this.amount      = amount;
        this.type        = type;
        this.timestamp   = LocalDateTime.now();
    }

    // ✅ Full constructor — used when REBUILDING from DB (was missing, caused history to show nothing)
    public Transaction(String id, String accountId, String referenceId,
                       double amount, String type, LocalDateTime timestamp) {
        this.id          = id;
        this.accountId   = accountId;
        this.referenceId = referenceId;
        this.amount      = amount;
        this.type        = type;
        this.timestamp   = timestamp;
    }

    public String getId()               { return id; }
    public String getAccountId()        { return accountId; }
    public String getReferenceId()      { return referenceId; }
    public double getAmount()           { return amount; }
    public String getType()             { return type; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public void setId(String id)                   { this.id = id; }
    public void setAccountId(String accountId)     { this.accountId = accountId; }
    public void setReferenceId(String ref)         { this.referenceId = ref; }
    public void setAmount(double amount)           { this.amount = amount; }
    public void setType(String type)               { this.type = type; }
    public void setTimestamp(LocalDateTime ts)     { this.timestamp = ts; }

    @Override
    public String toString() {
        return "Transaction{id='" + id + "', accountId='" + accountId +
                "', ref='" + referenceId + "', amount=" + amount +
                ", type='" + type + "', time=" + timestamp + '}';
    }
}