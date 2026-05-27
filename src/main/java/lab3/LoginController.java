// LoginController.java
package lab3;

import com.igirepay.igirepay.MainApp;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lab1.Customer;
import lab2.CustomerDAO;
import lab2.Connect;
import java.sql.SQLException;

public class LoginController {

    @FXML private TextField phoneField;
    @FXML private PasswordField pinField;
    @FXML private Label statusLabel;

    private CustomerDAO customerDAO = new CustomerDAO();

    public LoginController() {
        try {
            Connect.getConnection();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Connection Failed",
                    "Could not connect to database: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogin() {
        statusLabel.setText("");

        String phone = phoneField.getText().trim();
        String pin = pinField.getText().trim();

        if (phone.isEmpty() || pin.isEmpty()) {
            statusLabel.setText("All fields are required.");
            return;
        }

        try {
            Customer customer = customerDAO.findByPhone(phone);

            if (customer == null) {
                statusLabel.setText("Invalid phone number.");
                return;
            }

            if (customer.isLocked()) {
                showAlert(Alert.AlertType.WARNING, "Account Locked",
                        "This account is locked.");
                return;
            }

            String pinHash = hashPin(pin);
            if (!pinHash.equals(customer.getPinHash())) {
                int attemptsRemaining = 3 - customer.getFailedLoginAttempts();
                statusLabel.setText("Wrong PIN. " + attemptsRemaining + " attempts remaining.");
                customer.incrementFailedAttempts();
                customerDAO.update(customer);
                return;
            }

            // Success - go to dashboard
            MainApp.showDashboard();

        } catch (SQLException e) {
            statusLabel.setText("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleRegister() {
        MainApp.showRegister();
    }

    @FXML
    private void handleExit() {
        try {
            if (phoneField != null) {
                javafx.stage.Stage stage = (javafx.stage.Stage) phoneField.getScene().getWindow();
                stage.close();
            }
        } catch (Exception e) {
            // Fallback: do nothing
            System.exit(0);
        }
    }


    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
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