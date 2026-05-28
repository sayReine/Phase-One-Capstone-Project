// RegisterController.java
package lab3;

import com.igirepay.igirepay.MainApp;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lab1.Customer;
import lab1.SavingsAccount;
import lab1.WalletAccount;
import lab2.AccountDAO;
import lab2.CustomerDAO;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

public class RegisterController {

    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField pinField;
    @FXML private PasswordField confirmPinField;
    @FXML private Label statusLabel;

    private CustomerDAO customerDAO = new CustomerDAO();
    private AccountDAO  accountDAO  = new AccountDAO();

    @FXML
    private void handleRegister() {
        statusLabel.setText("");

        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String pin = pinField.getText().trim();
        String confirmPin = confirmPinField.getText().trim();

        if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || pin.isEmpty()) {
            statusLabel.setText("All fields are required.");
            return;
        }

        if (!email.contains("@")) {
            statusLabel.setText("Invalid email format.");
            return;
        }

        if (pin.length() != 5 || !pin.matches("\\d+")) {
            statusLabel.setText("PIN must be exactly 5 digits.");
            return;
        }

        if (!pin.equals(confirmPin)) {
            statusLabel.setText("PINs do not match.");
            return;
        }

        try {
            if (customerDAO.phoneExists(phone)) {
                statusLabel.setText("Phone number already registered.");
                return;
            }
            if (customerDAO.emailExists(email)) {
                statusLabel.setText("Email already registered.");
                return;
            }

            String customerId = UUID.randomUUID().toString();
            String pinHash = hashPin(pin);

            Customer customer = new Customer(
                    customerId,
                    fullName,
                    email,
                    phone,
                    pinHash,
                    LocalDateTime.now(),
                    0,
                    false
            );

            customerDAO.create(customer);

            // Auto-create one wallet and one savings account for the new customer
            accountDAO.create(new WalletAccount(customerId));
            accountDAO.create(new SavingsAccount(customerId));

            showAlert(Alert.AlertType.INFORMATION, "Registration Successful",
                    "Your account has been created. Please login.");
            MainApp.showLogin();

        } catch (SQLException e) {
            statusLabel.setText("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        MainApp.showLogin();
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