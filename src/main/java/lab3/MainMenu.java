package lab3;

import lab1.Customer;
import lab2.CustomerDAO;
import lab2.Connect;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.UUID;

public class MainMenu {

    CustomerDAO customerDAO = new CustomerDAO();
    Scanner input = new Scanner(System.in);

    public void start(){

        try{
            Connect.getConnection();
        } catch (SQLException e) {
            System.out.println("Database connection failed:"+ e.getMessage());
            System.out.println("check your java connect file");
            return;
        }

        while(true) {
            printBanner();
            System.out.println("===== igire pay====");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("0. Exit");

            String choice = input.nextLine().trim();

            switch (choice){
            
                    case "1" -> handleLogin();
                    case "2" -> handleRegister();
                    case "0" -> {
                        System.out.println("Goodbye!");
                        return;
                    }
                    default -> System.out.println("Invalid option.");
            }



        }




    }

    private void handleRegister() {
        System.out.println("\n--- Register ---");
        System.out.print("Full name: ");
        String fullName = input.nextLine().trim();

        System.out.print("Email: ");
        String email = input.nextLine().trim();

        System.out.print("Phone number: ");
        String phone = input.nextLine().trim();

        System.out.print("Create PIN (5 digits): ");
        String pin = input.nextLine().trim();

        System.out.print("Confirm PIN: ");
        String confirmPin = input.nextLine().trim();

        if(fullName.isBlank() || email.isBlank() || phone.isBlank()){
            System.out.println("All fields are required");
            return;
        }

        if (!email.contains("@")){
            System.out.println("invalid email");
            return;
        }

        if( pin.length() !=  5){
            System.out.println("Pin must be 5 digits");
            return;
        }

        if(! pin.equals(confirmPin)){
            System.out.println("PINs MUST MATCH");
            return;
        }


        try{

            // checking if there aren't duplicates that we are sending to the database
            if (customerDAO.phoneExists(phone)){
                System.out.println("Email already registered.");
                return;
            }
            if(customerDAO.emailExists(email)){
                System.out.println("Phone number already registered.");
                return;
            }

            String customerId = UUID.randomUUID().toString();
            String pinHash = hashPin(pin);


            Customer customer = new Customer(customerId, fullName, email, phone, pinHash, LocalDateTime.now(),0,false);

            customerDAO.create(customer);
            System.out.println("Registration successful! You can now login.");

        } catch (SQLException e) {
            System.out.println("Registration error: " + e.getMessage());
        }

    }

    private void handleLogin(){
        System.out.println("\n--- Login ---");
        System.out.println("Enter your phone number:");
        String phone = input.nextLine().trim();

        System.out.println("Enter your PIN:");
        String pin = input.nextLine().trim();

        try{
            Customer customer = customerDAO.findByPhone(phone);
            if(customer == null){
                System.out.println("There is no account with that number");
                return;
            }
            if(customer.isLocked()){
                System.out.println("This is locked.");
                return;
            }

            String pinHash = hashPin(pin);
            if (!pinHash.equals(customer.getPinHash())){
                customer.incrementFailedAttempts();
                if(customer.getFailedLoginAttempts() >=3){
                    customer.lockAccount();
                    System.out.println("Too many failed attempts. Account locked.");
                }else {
                    System.out.println("Wrong pin" +(3-customer.getFailedLoginAttempts())+"attempts remaining");
                }

                customerDAO.update(customer);
                return;

            }
            //successfull login
            customer.resetFailedAttempts();
            customerDAO.update(customer);

            System.out.println("Welcome, " + customer.getFullName() + "!");
            new DashboardMenu(customer, input).show();

        } catch (SQLException e) {
            System.out.println("Login error:"+e.getMessage());
        }




    }

    private String hashPin(String pin) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(pin.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    private void printBanner() {
        // Printed once when the application starts
        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║       IGIREPAY DIGITAL WALLET    ║");
        System.out.println("║       Secure Payments — Rwanda   ║");
        System.out.println("╚══════════════════════════════════╝");
    }
}
