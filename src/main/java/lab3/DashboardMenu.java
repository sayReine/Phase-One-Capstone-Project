package lab3;

import lab1.Customer;

import java.util.Scanner;

public class DashboardMenu {

    private final Customer customer;
    private final Scanner  input;

    public DashboardMenu(Customer customer, Scanner input) {
        this.customer = customer;
        this.input    = input;
    }

    public void show() {
        while (true) {
            System.out.println("\n========== Dashboard — " + customer.getFullName() + " ==========");
            System.out.println("1. Customer Management");
            System.out.println("2. Account Management");
            System.out.println("3. Transaction Management");
            System.out.println("0. Logout");
            System.out.print("Enter choice: ");

            String choice = input.nextLine().trim();

            switch (choice) {
                case "1" -> new CustomerMenu(customer, input).show();
                case "2" -> new AccountMenu(customer, input).show();
                case "3" -> new TransactionMenu(customer, input).show();
                case "0" -> {
                    System.out.println("Logged out.");
                    return;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }
}