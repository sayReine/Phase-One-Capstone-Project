package lab3;

import com.igirepay.igirepay.MainApp;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lab1.Customer;
import lab2.CustomerDAO;
import lab2.Connect;

import java.sql.SQLException;

public class LoginController {

    @FXML private TextField     phoneField;
    @FXML private PasswordField pinField;
    @FXML private Label         statusLabel;

    private final CustomerDAO customerDAO = new CustomerDAO();

    public LoginController() {
        try {
            Connect.getConnection();
        } catch (SQLException e) {
            // Will surface as an error when user tries to login
        }
    }

    @FXML
    private void handleLogin() {
        statusLabel.setText("");

        String phone = phoneField.getText().trim();
        String pin   = pinField.getText().trim();

        if (phone.isEmpty() || pin.isEmpty()) {
            statusLabel.setText("Phone number and PIN are required.");
            return;
        }

        try {
            Customer customer = customerDAO.findByPhone(phone);

            if (customer == null) {
                statusLabel.setText("No account found with that phone number.");
                return;
            }

            if (customer.isLocked()) {
                statusLabel.setText("Account is locked. Contact support.");
                return;
            }

            String pinHash = hashPin(pin);
            if (!pinHash.equals(customer.getPinHash())) {
                customer.incrementFailedAttempts();
                if (customer.getFailedLoginAttempts() >= 3) {
                    customer.lockAccount();
                    statusLabel.setText("Account locked after too many failed attempts.");
                } else {
                    int remaining = 3 - customer.getFailedLoginAttempts();
                    statusLabel.setText("Wrong PIN. " + remaining + " attempt(s) remaining.");
                }
                customerDAO.update(customer);
                return;
            }

            // Success — reset counter and open dashboard
            customer.resetFailedAttempts();
            customerDAO.update(customer);

            // Pass the logged-in customer to the dashboard
            MainApp.showDashboard(customer);

        } catch (SQLException e) {
            statusLabel.setText("Database error: " + e.getMessage());
        }
    }

    @FXML private void handleRegister() { MainApp.showRegister(); }

    @FXML
    private void handleExit() {
        try {
            javafx.stage.Stage stage = (javafx.stage.Stage) phoneField.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            System.exit(0);
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
}