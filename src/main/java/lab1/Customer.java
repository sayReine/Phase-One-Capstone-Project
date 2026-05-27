package lab1;

import java.time.LocalDateTime;
import java.util.*;

public class Customer {
    private final String id;
    private final LocalDateTime createdAt;
    private String fullName;
    private String email;
    private String phone;
    private String pinHash;

    private int failedLoginAttempts;
    private boolean locked;

    private List<Account> accounts;
    private List<Transaction> transactionHistory;
    private Set<String> processedReferenceIds;
    private Map<String, List<Transaction>> failedTransactionLogs;

    public Customer(String id, String fullName, String email, String phone,
                    String pinHash, LocalDateTime createdAt, int failedLoginAttempts, boolean locked) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.pinHash = pinHash;
        this.createdAt = createdAt;
        this.failedLoginAttempts = failedLoginAttempts;
        this.locked = locked;
        initCollections();
    }

    private void initCollections() {
        this.accounts = new ArrayList<>();
        this.transactionHistory = new ArrayList<>();
        this.processedReferenceIds = new HashSet<>();
        this.failedTransactionLogs = new HashMap<>();
    }

    public void addAccount(Account account) {
        accounts.add(account);
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void addTransaction(Transaction t) {
        transactionHistory.add(t);
        processedReferenceIds.add(t.getReferenceId());
    }

    public boolean isAlreadyProcessed(String referenceId) {
        return processedReferenceIds.contains(referenceId);
    }

    public void logFailedTransaction(String reason, Transaction t) {
        failedTransactionLogs
                .computeIfAbsent(reason, k -> new ArrayList<>())
                .add(t);
    }

    public List<Transaction> getTransactionHistory() {
        return transactionHistory;
    }

    public Map<String, List<Transaction>> getFailedTransactionLogs() {
        return failedTransactionLogs;
    }



    public void incrementFailedAttempts() { this.failedLoginAttempts++; }
    public void resetFailedAttempts()     { this.failedLoginAttempts = 0; }
    public void lockAccount()             { this.locked = true; }
    public boolean isLocked()             { return locked; }
    public int getFailedLoginAttempts()   { return failedLoginAttempts;}



    public String getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

}
