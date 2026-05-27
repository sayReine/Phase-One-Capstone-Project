package lab3;

import lab1.Account;
import lab1.Customer;
import lab2.CustomerDAO;
import lab2.AccountDAO;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class CustomerMenu {

    private final Customer    customer;
    private final Scanner     input;
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final AccountDAO  accountDAO  = new AccountDAO();

    public CustomerMenu(Customer customer, Scanner input) {
        this.customer = customer;
        this.input    = input;
    }

    public void show() {
        while (true) {
            System.out.println("\n--- Customer Management ---");
            System.out.println("1. View my profile");
            System.out.println("2. Update my information");
            System.out.println("3. View my accounts");
            System.out.println("0. Back");
            System.out.print("Enter choice: ");

            String choice = input.nextLine().trim();

            switch (choice) {
                case "1" -> viewProfile();
                case "2" -> updateInfo();
                case "3" -> viewAccounts();
                case "0" -> { return; }
                default  -> System.out.println("Invalid option.");
            }
        }
    }

    private void viewProfile() {
        System.out.println("\n--- My Profile ---");
        System.out.println("ID          : " + customer.getId());
        System.out.println("Full Name   : " + customer.getFullName());
        System.out.println("Email       : " + customer.getEmail());
        System.out.println("Phone       : " + customer.getPhone());
        System.out.println("Status      : " + (customer.isLocked() ? "LOCKED" : "ACTIVE"));
        System.out.println("Member Since: " + customer.getCreatedAt().toString().substring(0, 10));
    }

    private void updateInfo() {
        System.out.println("\n--- Update Information ---");
        System.out.println("Leave blank to keep current value.");

        System.out.print("Full name [" + customer.getFullName() + "]: ");
        String fullName = input.nextLine().trim();

        System.out.print("Email [" + customer.getEmail() + "]: ");
        String email = input.nextLine().trim();

        System.out.print("Phone [" + customer.getPhone() + "]: ");
        String phone = input.nextLine().trim();

        if (!fullName.isBlank()) customer.setFullName(fullName);
        if (!email.isBlank())    customer.setEmail(email);
        if (!phone.isBlank())    customer.setPhone(phone);

        try {
            customerDAO.update(customer);
            System.out.println("Profile updated successfully.");
        } catch (SQLException e) {
            System.out.println("Update failed: " + e.getMessage());
        }
    }

    private void viewAccounts() {
        System.out.println("\n--- My Accounts ---");
        try {
            List<Account> accounts = accountDAO.findByCustomerId(customer.getId());
            if (accounts.isEmpty()) {
                System.out.println("No accounts found. Go to Account Management to create one.");
                return;
            }
            System.out.printf("%-38s %-10s %15s%n", "Account ID", "Type", "Balance (RWF)");
            System.out.println("-".repeat(65));
            for (Account a : accounts) {
                System.out.printf("%-38s %-10s %,15.2f%n",
                        a.getId(), a.getAccountType(), a.getBalance());
            }
        } catch (SQLException e) {
            System.out.println("Could not load accounts: " + e.getMessage());
        }
    }
}