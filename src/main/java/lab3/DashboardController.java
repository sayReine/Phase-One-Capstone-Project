// DashboardController.java - Complete Implementation
package lab3;

import com.igirepay.igirepay.MainApp;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import lab1.WalletAccount;
import lab1.SavingsAccount;

import javafx.scene.text.Text;
import javafx.stage.Stage;
import lab1.*;
import lab2.AccountDAO;
import lab2.CustomerDAO;
import lab2.TransactionDAO;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class DashboardController {

    // Current logged-in customer
    private Customer currentCustomer;
    private final CustomerDAO customerDAO = new CustomerDAO();

    private AccountDAO accountDAO = new AccountDAO();
    private TransactionDAO transactionDAO = new TransactionDAO();

    @FXML private Text welcomeText;
    @FXML private Label statusLabel;

    // Constructor - Initialize with current customer
    public DashboardController() {
        try {
            // In real app, this comes from login
            // Using demo customer for demonstration
            currentCustomer = customerDAO.findByPhone("1234567890");

            if (currentCustomer == null) {
                currentCustomer = new Customer(
                        "demo-id",
                        "Demo User",
                        "demo@igirepay.rw",
                        "1234567890",
                        "demo-hash",
                        LocalDateTime.now(),
                        0,
                        false
                );
            }
            updateWelcomeText();
        } catch (SQLException e) {
            showError("Database Error", "Could not load user: " + e.getMessage());
        }
    }

    private void updateWelcomeText() {
        if (currentCustomer != null) {
            welcomeText.setText("Welcome, " + currentCustomer.getFullName());
        }
    }

    // ============================
    // EXERCISE 3.2: Exception Handling
    // ============================

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean validateAmount(String amountStr, boolean allowZero) {
        try {
            double amount = Double.parseDouble(amountStr);
            if (!allowZero && amount <= 0) {
                showError("Invalid Amount", "Amount must be greater than zero.");
                return false;
            }
            if (amount < 0) {
                showError("Invalid Amount", "Amount cannot be negative.");
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            showError("Invalid Amount", "Please enter a valid number.");
            return false;
        }
    }

    private String selectAccount() {
        try {
            List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            if (accounts.isEmpty()) {
                showInfo("No Accounts", "Please create an account first.");
                return null;
            }

            if (accounts.size() == 1) {
                return accounts.get(0).getId();
            }

            // Dialog to select account
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Select Account");
            dialog.setHeaderText("Choose an account:");

            ComboBox<String> combo = new ComboBox<>();
            for (Account a : accounts) {
                combo.getItems().add(a.getId() + " (" + a.getAccountType() + " - RWF " + a.getBalance() + ")");
            }
            combo.getSelectionModel().selectFirst();

            dialog.getDialogPane().setContent(combo);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialog.showAndWait();
            return accounts.get(combo.getSelectionModel().getSelectedIndex()).getId();

        } catch (SQLException e) {
            showError("Database Error", "Could not load accounts: " + e.getMessage());
            return null;
        }
    }

    // ============================
    // ACCOUNT MANAGEMENT
    // ============================

    @FXML
    private void createWallet() {
        try {
            WalletAccount wallet = new WalletAccount(currentCustomer.getId());
            accountDAO.create(wallet);
            statusLabel.setText("Wallet created successfully!");
            showInfo("Success", "Wallet Account Created\nID: " + wallet.getId() + "\nBalance: RWF 0.00");
        } catch (SQLException e) {
            showError("Database Error", "Could not create wallet: " + e.getMessage());
        }
    }

    @FXML
    private void createSavings() {
        try {
            SavingsAccount savings = new SavingsAccount(currentCustomer.getId());
            accountDAO.create(savings);
            statusLabel.setText("Savings account created!");
            showInfo("Success", "Savings Account Created\nID: " + savings.getId() +
                    "\nBalance: RWF 0.00\nWithdrawal Fee: RWF 500.00");
        } catch (SQLException e) {
            showError("Database Error", "Could not create savings: " + e.getMessage());
        }
    }

    @FXML
    private void viewBalance() {
        try {
            List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            if (accounts.isEmpty()) {
                showInfo("Accounts", "No accounts found.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            double total = 0;
            for (Account a : accounts) {
                sb.append(a.getAccountType()).append(": RWF ")
                        .append(String.format("%,.2f", a.getBalance())).append("\n");
                total += a.getBalance();
            }
            sb.append("\nTOTAL: RWF ").append(String.format("%,.2f", total));

            showInfo("Account Balances", sb.toString());
            statusLabel.setText("Viewed balances. Total: RWF " + String.format("%,.2f", total));
        } catch (SQLException e) {
            showError("Database Error", "Could not load balances: " + e.getMessage());
        }
    }

    // ============================
    // TRANSACTION MANAGEMENT
    // ============================

    @FXML
    private void deposit() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Deposit Money");
        dialog.setHeaderText("Enter amount to deposit:");

        dialog.showAndWait().ifPresent(amountStr -> {
            if (!validateAmount(amountStr, false)) return;
            double amount = Double.parseDouble(amountStr);

            String accountId = selectAccount();
            if (accountId == null) return;

            try {
                String refId = "DEP-" + System.currentTimeMillis();
                transactionDAO.deposit(accountId, amount, refId);
                statusLabel.setText("Deposited: RWF " + amount);
                showInfo("Success", "Deposited RWF " + String.format("%,.2f", amount) +
                        "\nReference: " + refId);
            } catch (SQLException e) {
                showError("Database Error", "Deposit failed: " + e.getMessage());
            } catch (IllegalStateException e) {
                showError("Transaction Error", e.getMessage());
            }
        });
    }

    @FXML
    private void withdraw() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Withdraw Money");
        dialog.setHeaderText("Enter amount to withdraw:");

        dialog.showAndWait().ifPresent(amountStr -> {
            if (!validateAmount(amountStr, false)) return;
            double amount = Double.parseDouble(amountStr);

            String accountId = selectAccount();
            if (accountId == null) return;

            try {
                // Check balance for insufficient funds
                Account account = accountDAO.findById(accountId);
                if (account.getBalance() < amount) {
                    showError("Insufficient Balance", "Your balance is RWF " +
                            String.format("%,.2f", account.getBalance()));
                    return;
                }

                String refId = "WTH-" + System.currentTimeMillis();
                transactionDAO.withdraw(accountId, amount, refId);
                statusLabel.setText("Withdrew: RWF " + amount);
                showInfo("Success", "Withdrew RWF " + String.format("%,.2f", amount) +
                        "\nReference: " + refId);
            } catch (SQLException e) {
                showError("Database Error", "Withdrawal failed: " + e.getMessage());
            } catch (IllegalStateException e) {
                showError("Transaction Error", e.getMessage());
            }
        });
    }

    @FXML
    private void transfer() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Transfer Money");
        dialog.setHeaderText("Enter transfer details");

        VBox vBox = new VBox(10);
        TextField destField = new TextField();
        TextField amountField = new TextField();

        destField.setPromptText("Destination Account ID");
        amountField.setPromptText("Amount");

        vBox.getChildren().addAll(new Label("To:"), destField, new Label("Amount:"), amountField);
        dialog.getDialogPane().setContent(vBox);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (!validateAmount(amountField.getText(), false)) return;
            double amount = Double.parseDouble(amountField.getText());
            String destId = destField.getText().trim();

            if (destId.isEmpty()) {
                showError("Invalid Account", "Destination account ID is required.");
                return;
            }

            String accountId = selectAccount();
            if (accountId == null) return;

            try {
                Account from = accountDAO.findById(accountId);
                if (from.getBalance() < amount) {
                    showError("Insufficient Balance", "Insufficient funds for transfer.");
                    return;
                }

                String refId = "TRF-" + System.currentTimeMillis();
                transactionDAO.transfer(accountId, destId, amount, refId);
                statusLabel.setText("Transferred: RWF " + amount);
                showInfo("Success", "Transferred RWF " + String.format("%,.2f", amount) +
                        "\nTo: " + destId + "\nReference: " + refId);
            } catch (SQLException e) {
                showError("Database Error", "Transfer failed: " + e.getMessage());
            } catch (IllegalStateException e) {
                showError("Transaction Error", e.getMessage());
            }
        });
    }

    @FXML
    private void viewHistory() {
        String accountId = selectAccount();
        if (accountId == null) return;

        try {
            List<Transaction> transactions = transactionDAO.findByAccountId(accountId);
            if (transactions.isEmpty()) {
                showInfo("History", "No transactions found.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (Transaction t : transactions) {
                sb.append(t.getType()).append(": RWF ")
                        .append(String.format("%,.2f", t.getAmount()))
                        .append(" (").append(t.getTimestamp()).append(")\n");
            }

            showInfo("Transaction History", sb.toString());
        } catch (SQLException e) {
            showError("Database Error", "Could not load history: " + e.getMessage());
        }
    }

    // ============================
    // EXERCISE 3.3: Transaction Reports
    // ============================

    @FXML
    private void dailySummary() {
        try {
            List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            if (accounts.isEmpty()) {
                showInfo("Summary", "No accounts found.");
                return;
            }

            double totalIn = 0;
            double totalOut = 0;
            int countIn = 0;
            int countOut = 0;

            for (Account acc : accounts) {
                List<Transaction> txns = transactionDAO.findByAccountId(acc.getId());
                for (Transaction t : txns) {
                    if (t.getType().equals("DEPOSIT")) {
                        totalIn += t.getAmount();
                        countIn++;
                    } else if (t.getType().equals("WITHDRAW") || t.getType().equals("TRANSFER")) {
                        totalOut += t.getAmount();
                        countOut++;
                    }
                }
            }

            String summary = "Total Deposits: RWF " + String.format("%,.2f", totalIn) +
                    "\nTotal Withdrawals: RWF " + String.format("%,.2f", totalOut) +
                    "\nNet Flow: RWF " + String.format("%,.2f", totalIn - totalOut) +
                    "\n\nTotal Transactions: " + (countIn + countOut);

            showInfo("Daily Transaction Summary", summary);
            statusLabel.setText("Viewed daily summary");
        } catch (SQLException e) {
            showError("Error", "Could not generate summary: " + e.getMessage());
        }
    }

    @FXML
    private void viewAllTransactions() {
        try {
            List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            if (accounts.isEmpty()) {
                showInfo("Transactions", "No accounts found.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (Account acc : accounts) {
                List<Transaction> txns = transactionDAO.findByAccountId(acc.getId());
                sb.append("\n--- ").append(acc.getAccountType()).append(" ---\n");
                for (Transaction t : txns) {
                    sb.append(t.getType()).append(": RWF ")
                            .append(String.format("%,.2f", t.getAmount()))
                            .append(" [").append(t.getTimestamp()).append("]\n");
                }
            }

            showInfo("All Transactions", sb.toString());
        } catch (SQLException e) {
            showError("Error", e.getMessage());
        }
    }
// ============================
    // EXERCISE 3.3: Transaction Reports (Export CSV)
    // ============================

    @FXML
    private void exportCSV() {
        try {
            List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            if (accounts.isEmpty()) {
                showError("Export Error", "No accounts found.");
                return;
            }

            String fileName = "transaction_history_" + LocalDate.now() + ".csv";
            FileWriter fw = new FileWriter(fileName);
            PrintWriter pw = new PrintWriter(fw);

            // Write CSV Header
            pw.println("Reference ID,Account ID,Type,Amount,Timestamp");

            // Write all transactions
            for (Account acc : accounts) {
                List<Transaction> txns = transactionDAO.findByAccountId(acc.getId());
                for (Transaction t : txns) {
                    pw.println(t.getReferenceId() + "," + acc.getId() + "," +
                            t.getType() + "," + t.getAmount() + "," + t.getTimestamp());
                }
            }

            pw.close();
            statusLabel.setText("Exported to " + fileName);
            showInfo("Export Success", "Transaction history exported to:\n" + fileName);

        } catch (Exception e) {
            showError("Export Error", "Could not export: " + e.getMessage());
        }
    }

    // ============================
    // EXERCISE 3.4: Authentication (Change PIN)
    // ============================

    @FXML
    private void changePIN() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Change PIN");
        dialog.setHeaderText("Enter your new PIN");

        VBox vBox = new VBox(10);
        PasswordField currentPinField = new PasswordField();
        PasswordField newPinField = new PasswordField();
        PasswordField confirmPinField = new PasswordField();

        currentPinField.setPromptText("Current PIN");
        newPinField.setPromptText("New PIN (5 digits)");
        confirmPinField.setPromptText("Confirm New PIN");

        vBox.getChildren().addAll(
                new Label("Current PIN:"), currentPinField,
                new Label("New PIN:"), newPinField,
                new Label("Confirm PIN:"), confirmPinField
        );

        dialog.getDialogPane().setContent(vBox);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            String currentPin = currentPinField.getText();
            String newPin = newPinField.getText();
            String confirmPin = confirmPinField.getText();

            // Validate current PIN
            try {
                if (!hashPin(currentPin).equals(currentCustomer.getPinHash())) {
                    showError("Invalid PIN", "Current PIN is incorrect.");
                    return;
                }

                if (hashPin(newPin).equals(currentCustomer.getPinHash())) {
                    showError("Invalid PIN", "New PIN must be different from your current PIN.");
                    return;
                }


                // Validate new PIN format
                if (newPin.length() != 5 || !newPin.matches("\\d+")) {
                    showError("Invalid PIN", "New PIN must be exactly 5 digits.");
                    return;
                }

                if (!newPin.equals(confirmPin)) {
                    showError("PIN Mismatch", "New PINs do not match.");
                    return;
                }

                // Update PIN in database
                currentCustomer.setPinHash(hashPin(newPin));
                customerDAO.update(currentCustomer);


                statusLabel.setText("PIN changed successfully!");
                showInfo("Success", "Your PIN has been changed successfully.");

            } catch (SQLException e) {
                showError("Database Error", "Could not update PIN: " + e.getMessage());
            }
        });
    }

    // ============================
    // ACCOUNT DELETION
    // ============================

    @FXML
    private void deleteAccount() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Account");
        confirm.setHeaderText("Are you sure you want to delete your account?");
        confirm.setContentText("This action cannot be undone. All data will be lost.");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    // Delete all accounts for this customer
                    List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
                    for (Account acc : accounts) {
                        accountDAO.delete(acc.getId());
                    }

                    // Delete customer
                    customerDAO.delete(currentCustomer.getId());


                    showInfo("Account Deleted", "Your account has been deleted.");
                    logout();

                } catch (SQLException e) {
                    showError("Database Error", "Could not delete account: " + e.getMessage());
                }
            }
        });
    }

    // ============================
    // LOGOUT
    // ============================

    @FXML
    private void logout() {
        Stage stage = (Stage) welcomeText.getScene().getWindow();
        stage.close();
        MainApp.showLogin();
    }

    // Helper: Hash PIN
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