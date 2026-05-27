package lab1;

import java.time.LocalDateTime;

public abstract class Account {

    private String id ;
    private double balance;
    private String customerId;
    private String accountType;
    private LocalDateTime createdAt;

    public Account( String customerId,String accountType) {
        this.id = java.util.UUID.randomUUID().toString();
        this.balance = 0.0;
        this.accountType = accountType;
        this.customerId = customerId;
        this.createdAt = LocalDateTime.now();
    }

    public abstract boolean withdraw(double amount, String referenceId);
    public abstract boolean deposit (double amount, String referenceId);
    public abstract String processTransaction(Transaction t);


    public  double getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getcustomerId() {
        return customerId;
    }

    public void setcustomerId(String customerId) {
        this.customerId = customerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", balance=" + balance +
                ", customerId='" + customerId + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
