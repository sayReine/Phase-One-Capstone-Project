// DashboardController.java - Complete Implementation
package lab3;

import com.igirepay.igirepay.MainApp;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
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
    private final AccountDAO accountDAO = new AccountDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    @FXML private TextField welcomeText;
    @FXML private Label statusLabel;

    // Constructor - Initialize with current customer (demo)
    public DashboardController() {
        try {
            // In a real app, this comes from login
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
        if (welcomeText != null && currentCustomer != null) {
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
                showInfo("Success", "Deposited RWF " + String.format("%,.2f", amount) + "\nReference: " + refId);
            } catch (SQLException e) {
                showError("SQL Error", "Deposit failed: " + e.getMessage());
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
                Account account = accountDAO.findById(accountId);
                if (account == null) {
                    showError("Invalid Account", "Account ID not found.");
                    return;
                }
                if (account.getBalance() < amount) {
                    showError("Insufficient Balance", "Your balance is RWF " + String.format("%,.2f", account.getBalance()));
                    return;
                }

                String refId = "WTH-" + System.currentTimeMillis();
                transactionDAO.withdraw(accountId, amount, refId);
                statusLabel.setText("Withdrew: RWF " + amount);
                showInfo("Success", "Withdrew RWF " + String.format("%,.2f", amount) + "\nReference: " + refId);
            } catch (SQLException e) {
                showError("SQL Error", "Withdrawal failed: " + e.getMessage());
            } catch (IllegalStateException e) {
                showError("Transaction Error", e.getMessage());
            } catch (IllegalArgumentException e) {
                showError("Invalid Account", e.getMessage());
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
                Account to = accountDAO.findById(destId);
                if (from == null || to == null) {
                    showError("Invalid Account", "One of the account IDs is invalid.");
                    return;
                }
                if (from.getBalance() < amount) {
                    showError("Insufficient Balance", "Insufficient funds for transfer.");
                    return;
                }

                String refId = "TRF-" + System.currentTimeMillis();
                transactionDAO.transfer(accountId, destId, amount, refId);
                statusLabel.setText("Transferred: RWF " + amount);
                showInfo("Success", "Transferred RWF " + String.format("%,.2f", amount) + "\nTo: " + destId + "\nReference: " + refId);
            } catch (SQLException e) {
                showError("SQL Error", "Transfer failed: " + e.getMessage());
            } catch (IllegalStateException e) {
                showError("Transaction Error", e.getMessage());
            } catch (IllegalArgumentException e) {
                showError("Invalid Account", e.getMessage());
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
                        .append(" (" ).append(t.getTimestamp()).append(")\n");
            }
            showInfo("Transaction History", sb.toString());
        } catch (SQLException e) {
            showError("SQL Error", "Could not load history: " + e.getMessage());
        }
    }

    // ============================
    // EXERCISE 3.3: Reports
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
            for (Account acc : accounts) {
                List<Transaction> txns = transactionDAO.findByAccountId(acc.getId());
                for (Transaction t : txns) {
                    if (t.getType() != null && t.getType().equalsIgnoreCase("DEPOSIT")) {
                        totalIn += t.getAmount();
                    } else if (t.getType() != null && (t.getType().equalsIgnoreCase("WITHDRAW") || t.getType().equalsIgnoreCase("TRANSFER"))) {
                        totalOut += t.getAmount();
                    }
                }
            }

            String summary = "Total Deposits: RWF " + String.format("%,.2f", totalIn) +
                    "\nTotal Withdrawals: RWF " + String.format("%,.2f", totalOut) +
                    "\nNet Flow: RWF " + String.format("%,.2f", totalIn - totalOut);

            showInfo("Daily Transaction Summary", summary);
            statusLabel.setText("Viewed daily summary");
        } catch (SQLException e) {
            showError("SQL Error", "Could not generate summary: " + e.getMessage());
        }
    }

    // Statement for last 7 days
    @FXML
    private void customerStatement() {
        try {
            List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            if (accounts.isEmpty()) {
                showInfo("Statement", "No accounts found.");
                return;
            }

            // Use first account for simplicity
            Account acc = accounts.get(0);
            List<Transaction> txns = transactionDAO.findByAccountId(acc.getId());

            LocalDateTime from = LocalDate.now().minusDays(7).atStartOfDay();
            LocalDateTime to = LocalDateTime.now();

            StringBuilder sb = new StringBuilder();
            sb.append("Customer Statement (Last 7 days)\n");
            sb.append("Account: ").append(acc.getId()).append(" (" + acc.getAccountType() + ")\n\n");

            for (Transaction t : txns) {
                LocalDateTime ts = t.getTimestamp();
                if (ts != null && (ts.isAfter(from) || ts.equals(from)) && (ts.isBefore(to) || ts.equals(to))) {
                    sb.append(t.getTimestamp()).append(" - ")
                            .append(t.getType()).append(": RWF ")
                            .append(String.format("%,.2f", t.getAmount()))
                            .append("\n");
                }
            }

            showInfo("Statement", sb.toString());
        } catch (SQLException e) {
            showError("SQL Error", "Could not generate statement: " + e.getMessage());
        }
    }

    @FXML
    private void exportCSV() {
        try {
            List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            if (accounts.isEmpty()) {
                showError("Export Error", "No accounts found.");
                return;
            }

            String fileName = "transaction_history_" + LocalDate.now() + ".csv";
            try (FileWriter fw = new FileWriter(fileName);
                 PrintWriter pw = new PrintWriter(fw)) {

                pw.println("Reference ID,Account ID,Type,Amount,Timestamp");

                for (Account acc : accounts) {
                    List<Transaction> txns = transactionDAO.findByAccountId(acc.getId());
                    for (Transaction t : txns) {
                        pw.println(t.getReferenceId() + "," + acc.getId() + "," +
                                t.getType() + "," + t.getAmount() + "," + t.getTimestamp());
                    }
                }
            }

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

            try {
                if (!hashPin(currentPin).equals(currentCustomer.getPinHash())) {
                    showError("Invalid PIN", "Current PIN is incorrect.");
                    return;
                }

                if (hashPin(newPin).equals(currentCustomer.getPinHash())) {
                    showError("Invalid PIN", "New PIN must be different from your current PIN.");
                    return;
                }

                if (newPin == null || newPin.length() != 5 || !newPin.matches("\\d+")) {
                    showError("Invalid PIN", "New PIN must be exactly 5 digits.");
                    return;
                }

                if (!newPin.equals(confirmPin)) {
                    showError("PIN Mismatch", "New PINs do not match.");
                    return;
                }

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
                    List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
                    for (Account acc : accounts) {
                        accountDAO.delete(acc.getId());
                    }

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
        Stage stage = (Stage) statusLabel.getScene().getWindow();
        stage.close();
        MainApp.showLogin();
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

