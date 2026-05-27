package lab3;

import lab1.Account;
import lab1.Customer;
import lab1.WalletAccount;
import lab1.SavingsAccount;
import lab2.AccountDAO;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class AccountMenu {

    private final Customer   customer;
    private final Scanner    input;
    private final AccountDAO accountDAO = new AccountDAO();

    public AccountMenu(Customer customer, Scanner input) {
        this.customer = customer;
        this.input    = input;
    }

    public void show() {
        while (true) {
            System.out.println("\n--- Account Management ---");
            System.out.println("1. Create Wallet account");
            System.out.println("2. Create Savings account");
            System.out.println("3. View account balance");
            System.out.println("4. Delete inactive account");
            System.out.println("0. Back");
            System.out.print("Enter choice: ");

            String choice = input.nextLine().trim();

            switch (choice) {
                case "1" -> createWallet();
                case "2" -> createSavings();
                case "3" -> viewBalance();
                case "4" -> deleteAccount();
                case "0" -> { return; }
                default  -> System.out.println("Invalid option.");
            }
        }
    }

    private void createWallet() {
        try {
            WalletAccount wallet = new WalletAccount(customer.getId());
            accountDAO.create(wallet);
            System.out.println("Wallet account created: " + wallet.getId());
            System.out.println("Balance: RWF 0.00");
        } catch (SQLException e) {
            System.out.println("Could not create wallet: " + e.getMessage());
        }
    }

    private void createSavings() {
        try {
            SavingsAccount savings = new SavingsAccount(customer.getId());
            accountDAO.create(savings);
            System.out.println("Savings account created: " + savings.getId());
            System.out.println("Balance: RWF 0.00");
            System.out.println("Withdrawal limit: RWF 10,500.00 | Fee applied on each withdrawal.");
        } catch (SQLException e) {
            System.out.println("Could not create savings account: " + e.getMessage());
        }
    }

    private void viewBalance() {
        try {
            List<Account> accounts = accountDAO.findByCustomerId(customer.getId());
            if (accounts.isEmpty()) {
                System.out.println("No accounts found.");
                return;
            }
            System.out.println("\n--- Account Balances ---");
            System.out.printf("%-38s %-10s %15s%n", "Account ID", "Type", "Balance (RWF)");
            System.out.println("-".repeat(65));
            double total = 0;
            for (Account a : accounts) {
                System.out.printf("%-38s %-10s %,15.2f%n",
                        a.getId(), a.getAccountType(), a.getBalance());
                total += a.getBalance();
            }
            System.out.println("-".repeat(65));
            System.out.printf("%-49s %,15.2f%n", "TOTAL", total);
        } catch (SQLException e) {
            System.out.println("Could not load balances: " + e.getMessage());
        }
    }

    private void deleteAccount() {
        try {
            List<Account> accounts = accountDAO.findByCustomerId(customer.getId());
            if (accounts.isEmpty()) {
                System.out.println("No accounts found.");
                return;
            }
            System.out.printf("%-38s %-10s %15s%n", "Account ID", "Type", "Balance (RWF)");
            System.out.println("-".repeat(65));
            for (Account a : accounts) {
                System.out.printf("%-38s %-10s %,15.2f%n",
                        a.getId(), a.getAccountType(), a.getBalance());
            }

            System.out.print("\nEnter Account ID to delete: ");
            String accountId = input.nextLine().trim();

            System.out.print("Type YES to confirm: ");
            String confirm = input.nextLine().trim();
            if (!confirm.equalsIgnoreCase("YES")) {
                System.out.println("Deletion cancelled.");
                return;
            }

            accountDAO.delete(accountId);

        } catch (IllegalStateException e) {
            System.out.println("Cannot delete: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }
}