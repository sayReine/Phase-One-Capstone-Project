package lab3;

import lab1.Account;
import lab1.Customer;
import lab1.Transaction;
import lab2.AccountDAO;
import lab2.TransactionDAO;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class TransactionMenu {

    private final Customer        customer;
    private final Scanner         input;
    private final AccountDAO      accountDAO      = new AccountDAO();
    private final TransactionDAO  transactionDAO  = new TransactionDAO();

    public TransactionMenu(Customer customer, Scanner input) {
        this.customer = customer;
        this.input    = input;
    }

    public void show() {
        while (true) {
            System.out.println("\n--- Transaction Management ---");
            System.out.println("1. Deposit money");
            System.out.println("2. Withdraw money");
            System.out.println("3. Transfer money");
            System.out.println("4. View transaction history");
            System.out.println("0. Back");
            System.out.print("Enter choice: ");

            String choice = input.nextLine().trim();

            switch (choice) {
                case "1" -> handleDeposit();
                case "2" -> handleWithdraw();
                case "3" -> handleTransfer();
                case "4" -> handleHistory();
                case "0" -> { return; }
                default  -> System.out.println("Invalid option.");
            }
        }
    }

    private void handleDeposit() {
        System.out.println("\n--- Deposit ---");
        String accountId = selectAccount();
        if (accountId == null) return;

        double amount = readAmount();
        if (amount <= 0) return;

        String referenceId = readReferenceId();

        try {
            transactionDAO.deposit(accountId, amount, referenceId);
            System.out.println("Reference ID: " + referenceId);
        } catch (IllegalStateException e) {
            System.out.println("Deposit rejected: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private void handleWithdraw() {
        System.out.println("\n--- Withdraw ---");
        String accountId = selectAccount();
        if (accountId == null) return;

        try {
            Account account = accountDAO.findById(accountId);
            if (account == null) { System.out.println("Account not found."); return; }
            System.out.println("Current balance: RWF " + String.format("%,.2f", account.getBalance()));
        } catch (SQLException e) {
            System.out.println("Could not load account: " + e.getMessage());
            return;
        }

        double amount = readAmount();
        if (amount <= 0) return;

        String referenceId = readReferenceId();

        try {
            transactionDAO.withdraw(accountId, amount, referenceId);
            System.out.println("Reference ID: " + referenceId);
        } catch (IllegalStateException e) {
            System.out.println("Withdrawal rejected: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private void handleTransfer() {
        System.out.println("\n--- Transfer ---");

        System.out.println("Your accounts:");
        String fromId = selectAccount();
        if (fromId == null) return;

        try {
            Account from = accountDAO.findById(fromId);
            if (from == null) { System.out.println("Account not found."); return; }
            System.out.println("Available balance: RWF " + String.format("%,.2f", from.getBalance()));
        } catch (SQLException e) {
            System.out.println("Could not load account: " + e.getMessage());
            return;
        }

        System.out.print("Destination account ID: ");
        String toId = input.nextLine().trim();

        double amount = readAmount();
        if (amount <= 0) return;

        System.out.println("\nTransfer summary:");
        System.out.println("  From   : " + fromId);
        System.out.println("  To     : " + toId);
        System.out.println("  Amount : RWF " + String.format("%,.2f", amount));
        System.out.print("Confirm? (YES/no): ");
        String confirm = input.nextLine().trim();
        if (!confirm.equalsIgnoreCase("YES")) {
            System.out.println("Transfer cancelled.");
            return;
        }

        String referenceId = readReferenceId();

        try {
            transactionDAO.transfer(fromId, toId, amount, referenceId);
            System.out.println("Reference ID: " + referenceId);
        } catch (IllegalStateException e) {
            System.out.println("Transfer rejected: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private void handleHistory() {
        System.out.println("\n--- Transaction History ---");
        String accountId = selectAccount();
        if (accountId == null) return;

        try {
            List<Transaction> history = transactionDAO.findByAccountId(accountId);
            if (history.isEmpty()) {
                System.out.println("No transactions found.");
                return;
            }
            System.out.println("-".repeat(75));
            System.out.printf("%-38s %-12s %12s%n", "Reference ID", "Type", "Amount (RWF)");
            System.out.println("-".repeat(75));
            for (Transaction t : history) {
                System.out.printf("%-38s %-12s %,12.2f%n",
                        t.getReferenceId(), t.getType(), t.getAmount());
            }
            System.out.println("-".repeat(75));
        } catch (SQLException e) {
            System.out.println("Could not load history: " + e.getMessage());
        }
    }

    private String selectAccount() {
        try {
            List<Account> accounts = accountDAO.findByCustomerId(customer.getId());
            if (accounts.isEmpty()) {
                System.out.println("No accounts found. Create one in Account Management.");
                return null;
            }
            System.out.printf("%-38s %-10s %15s%n", "Account ID", "Type", "Balance (RWF)");
            System.out.println("-".repeat(65));
            for (Account a : accounts) {
                System.out.printf("%-38s %-10s %,15.2f%n",
                        a.getId(), a.getAccountType(), a.getBalance());
            }
            if (accounts.size() == 1) {
                System.out.println("Auto-selected: " + accounts.get(0).getId());
                return accounts.get(0).getId();
            }
            System.out.print("Enter Account ID: ");
            return input.nextLine().trim();
        } catch (SQLException e) {
            System.out.println("Could not load accounts: " + e.getMessage());
            return null;
        }
    }

    private double readAmount() {
        while (true) {
            System.out.print("Amount (RWF): ");
            try {
                double value = Double.parseDouble(input.nextLine().trim());
                if (value <= 0) {
                    System.out.println("Amount must be greater than zero.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Invalid amount. Enter a number e.g. 5000.00");
            }
        }
    }

    private String readReferenceId() {
        while (true) {
            System.out.print("Reference ID: ");
            String ref = input.nextLine().trim();
            if (ref.isEmpty()) {
                System.out.println("Reference ID cannot be empty.");
                continue;
            }
            return ref;
        }
    }
}