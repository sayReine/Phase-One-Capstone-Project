package lab3;

import com.igirepay.igirepay.MainApp;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
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
import java.util.UUID;

public class DashboardController {

    @FXML private Label      welcomeLabel;
    @FXML private Label      pageTitle;
    @FXML private Label      balanceBadge;
    @FXML private Label      statusLabel;
    @FXML private StackPane  contentPane;

    private Customer       currentCustomer;
    private final CustomerDAO    customerDAO    = new CustomerDAO();
    private final AccountDAO     accountDAO     = new AccountDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    // Called by MainApp after login to inject the customer
    public void setCustomer(Customer customer) {
        this.currentCustomer = customer;
        welcomeLabel.setText(customer.getFullName());
        refreshBalanceBadge();
        showHome();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private void setPage(String title, javafx.scene.Node content) {
        pageTitle.setText(title);
        contentPane.getChildren().setAll(content);
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg);
    }

    private void refreshBalanceBadge() {
        try {
            List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            double total = accounts.stream().mapToDouble(Account::getBalance).sum();
            balanceBadge.setText("Total: RWF " + String.format("%,.2f", total));
        } catch (SQLException e) {
            balanceBadge.setText("Total: —");
        }
    }

    private void alert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
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

    // ── HOME DASHBOARD ───────────────────────────────────────────

    @FXML public void showHome() {
        setPage("Dashboard", buildHomePanel());
    }

    private ScrollPane buildHomePanel() {
        VBox root = new VBox(20);
        root.setStyle("-fx-padding: 4 0 0 0;");

        // ── Stats row ────────────────────────────────────────────
        try {
            List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            double totalBalance = accounts.stream().mapToDouble(Account::getBalance).sum();
            long walletCount  = accounts.stream().filter(a -> "Wallet".equals(a.getAccountType())).count();
            long savingsCount = accounts.stream().filter(a -> "Saving".equals(a.getAccountType())).count();

            HBox stats = new HBox(16);
            stats.getChildren().addAll(
                    statCard("Total Balance", "RWF " + String.format("%,.2f", totalBalance), "All accounts combined", "card-green"),
                    statCard("Wallet Accounts", String.valueOf(walletCount), "Active wallets", "card-amber"),
                    statCard("Savings Accounts", String.valueOf(savingsCount), "Active savings", "card-dark")
            );
            for (javafx.scene.Node n : stats.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
            root.getChildren().add(stats);
        } catch (SQLException e) {
            root.getChildren().add(new Label("Could not load stats: " + e.getMessage()));
        }

        // ── Quick actions ────────────────────────────────────────
        VBox actionsBox = new VBox(12);
        actionsBox.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 12, 0, 0, 3);");

        Label actionsTitle = new Label("Quick Actions");
        actionsTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1a3a2a;");

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getChildren().addAll(
                quickBtn("⬆\nDeposit",  this::showDeposit),
                quickBtn("⬇\nWithdraw", this::showWithdraw),
                quickBtn("↔\nTransfer", this::showTransfer),
                quickBtn("📋\nHistory",  this::showHistory),
                quickBtn("📊\nReports",  this::showDailySummary)
        );
        actionsBox.getChildren().addAll(actionsTitle, actions);
        root.getChildren().add(actionsBox);

        // ── Recent transactions ──────────────────────────────────
        try {
            List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            if (!accounts.isEmpty()) {
                List<Transaction> recent = transactionDAO.findByAccountId(accounts.get(0).getId());
                if (!recent.isEmpty()) {
                    VBox histBox = new VBox(12);
                    histBox.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 12, 0, 0, 3);");
                    Label histTitle = new Label("Recent Transactions");
                    histTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1a3a2a;");
                    histBox.getChildren().add(histTitle);
                    int count = 0;
                    for (Transaction t : recent) {
                        if (count++ >= 5) break;
                        histBox.getChildren().add(buildTxRow(t));
                    }
                    root.getChildren().add(histBox);
                }
            }
        } catch (SQLException ignored) {}

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return sp;
    }

    private VBox statCard(String title, String value, String sub, String style) {
        VBox card = new VBox(6);
        card.getStyleClass().add(style);
        card.setMaxWidth(Double.MAX_VALUE);

        Label t = new Label(title); t.getStyleClass().add("card-title-white");
        Label v = new Label(value); v.getStyleClass().add("card-value-white");
        Label s = new Label(sub);   s.getStyleClass().add("card-sub-white");

        card.getChildren().addAll(t, v, s);
        return card;
    }

    private Button quickBtn(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("quick-btn");
        b.setOnAction(e -> action.run());
        return b;
    }

    private HBox buildTxRow(Transaction t) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 8 0; -fx-border-color: transparent transparent #f0f0f0 transparent; -fx-border-width: 0 0 1 0;");

        String badgeStyle = switch (t.getType()) {
            case "Deposit"  -> "-fx-background-color:#e8f5ec; -fx-text-fill:#1a5c2e;";
            case "Withdraw" -> "-fx-background-color:#fdecea; -fx-text-fill:#c0392b;";
            default         -> "-fx-background-color:#fff3e0; -fx-text-fill:#c8860a;";
        };
        Label badge = new Label(t.getType());
        badge.setStyle(badgeStyle + "-fx-font-size:11px; -fx-font-weight:bold; -fx-padding:3 10; -fx-background-radius:12;");
        badge.setPrefWidth(80);

        Label ref = new Label(t.getReferenceId());
        ref.setStyle("-fx-font-size:11px; -fx-text-fill:#8aaa9a;");
        HBox.setHgrow(ref, Priority.ALWAYS);

        Label amt = new Label("RWF " + String.format("%,.2f", t.getAmount()));
        amt.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#1a3a2a;");

        row.getChildren().addAll(badge, ref, amt);
        return row;
    }

    // ── ACCOUNTS ─────────────────────────────────────────────────

    @FXML public void showAccounts() {
        setPage("My Accounts", buildAccountsPanel());
    }

    private ScrollPane buildAccountsPanel() {
        VBox root = new VBox(16);

        try {
            List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            if (accounts.isEmpty()) {
                Label empty = new Label("No accounts yet. Use 'Create Account' to get started.");
                empty.setStyle("-fx-text-fill: #8aaa9a; -fx-font-size:14px;");
                root.getChildren().add(empty);
            } else {
                for (Account a : accounts) {
                    VBox card = new VBox(10);
                    boolean isWallet = "Wallet".equals(a.getAccountType());
                    card.setStyle("-fx-background-color:" + (isWallet ? "#1a5c2e" : "#c8860a") +
                            "; -fx-background-radius:12; -fx-padding:22; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.15),12,0,0,4);");

                    Label type = new Label(a.getAccountType() + " Account");
                    type.setStyle("-fx-font-size:12px; -fx-text-fill:rgba(255,255,255,0.7); -fx-font-weight:bold;");

                    Label bal = new Label("RWF " + String.format("%,.2f", a.getBalance()));
                    bal.setStyle("-fx-font-size:28px; -fx-font-weight:bold; -fx-text-fill:white;");

                    Label id = new Label("ID: " + a.getId());
                    id.setStyle("-fx-font-size:10px; -fx-text-fill:rgba(255,255,255,0.5);");

                    HBox btnRow = new HBox(10);
                    Button deposit  = new Button("Deposit");   deposit.getStyleClass().add("btn-outline");
                    Button withdraw = new Button("Withdraw");  withdraw.getStyleClass().add("btn-outline");
                    Button delete   = new Button("Delete");    delete.getStyleClass().add("btn-danger");
                    deposit.setStyle("-fx-text-fill:white; -fx-border-color:rgba(255,255,255,0.5); -fx-background-color:transparent;");
                    withdraw.setStyle("-fx-text-fill:white; -fx-border-color:rgba(255,255,255,0.5); -fx-background-color:transparent;");

                    String accId = a.getId();
                    deposit.setOnAction(e  -> openDeposit(accId));
                    withdraw.setOnAction(e -> openWithdraw(accId));
                    delete.setOnAction(e -> deleteAccount(accId));
                    btnRow.getChildren().addAll(deposit, withdraw, delete);

                    card.getChildren().addAll(type, bal, id, btnRow);
                    root.getChildren().add(card);
                }
            }
        } catch (SQLException e) {
            root.getChildren().add(new Label("Error: " + e.getMessage()));
        }

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return sp;
    }

    @FXML public void showCreateAccount() {
        setPage("Create Account", buildCreateAccountPanel());
    }

    private VBox buildCreateAccountPanel() {
        VBox root = new VBox(20);
        root.setMaxWidth(500);

        Label info = new Label("Choose an account type to create:");
        info.setStyle("-fx-font-size:14px; -fx-text-fill:#4a6a5a;");
        root.getChildren().add(info);

        // Wallet card
        VBox walletCard = new VBox(10);
        walletCard.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-padding:24; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),12,0,0,3);");
        Label wt = new Label("💳 Wallet Account"); wt.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#1a5c2e;");
        Label wd = new Label("Instant transfers and deposits.\nNo withdrawal fees. No limits.");
        wd.setStyle("-fx-text-fill:#6a8a7a; -fx-font-size:13px;");
        Button createWallet = new Button("Create Wallet"); createWallet.getStyleClass().add("btn-primary");
        createWallet.setOnAction(e -> createWallet());
        walletCard.getChildren().addAll(wt, wd, createWallet);

        // Savings card
        VBox savingsCard = new VBox(10);
        savingsCard.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-padding:24; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),12,0,0,3);");
        Label st = new Label("🏦 Savings Account"); st.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#c8860a;");
        Label sd = new Label("Save securely.\nWithdrawal fee applies. Limit: RWF 10,500 per transaction.");
        sd.setStyle("-fx-text-fill:#6a8a7a; -fx-font-size:13px;");
        Button createSavings = new Button("Create Savings"); createSavings.getStyleClass().add("btn-accent");
        createSavings.setOnAction(e -> createSavings());
        savingsCard.getChildren().addAll(st, sd, createSavings);

        root.getChildren().addAll(walletCard, savingsCard);
        return root;
    }

    private void createWallet() {
        try {
            WalletAccount w = new WalletAccount(currentCustomer.getId());
            accountDAO.create(w);
            refreshBalanceBadge();
            setStatus("Wallet created: " + w.getId());
            alert(Alert.AlertType.INFORMATION, "Success", "Wallet account created!\nID: " + w.getId());
            showAccounts();
        } catch (SQLException e) { alert(Alert.AlertType.ERROR, "Error", e.getMessage()); }
    }

    private void createSavings() {
        try {
            SavingsAccount s = new SavingsAccount(currentCustomer.getId());
            accountDAO.create(s);
            refreshBalanceBadge();
            setStatus("Savings created: " + s.getId());
            alert(Alert.AlertType.INFORMATION, "Success", "Savings account created!\nID: " + s.getId());
            showAccounts();
        } catch (SQLException e) { alert(Alert.AlertType.ERROR, "Error", e.getMessage()); }
    }

    private void deleteAccount(String accountId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Account");
        confirm.setHeaderText("Delete this account?");
        confirm.setContentText("Only accounts with zero balance can be deleted.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    accountDAO.delete(accountId);
                    refreshBalanceBadge();
                    setStatus("Account deleted.");
                    showAccounts();
                } catch (Exception e) { alert(Alert.AlertType.ERROR, "Cannot Delete", e.getMessage()); }
            }
        });
    }

    // ── DEPOSIT ──────────────────────────────────────────────────

    @FXML public void showDeposit() { openDeposit(null); }

    private void openDeposit(String preselectedId) {
        setPage("Deposit Money", buildTransactionPanel("Deposit", preselectedId, false));
    }

    // ── WITHDRAW ─────────────────────────────────────────────────

    @FXML public void showWithdraw() { openWithdraw(null); }

    private void openWithdraw(String preselectedId) {
        setPage("Withdraw Money", buildTransactionPanel("Withdraw", preselectedId, false));
    }

    // ── TRANSFER ─────────────────────────────────────────────────

    @FXML public void showTransfer() {
        setPage("Transfer Money", buildTransferPanel());
    }

    // ── Generic transaction panel (Deposit / Withdraw) ───────────

    private ScrollPane buildTransactionPanel(String type, String preselectedId, boolean isTransfer) {
        VBox root = new VBox(20);
        root.setMaxWidth(480);

        VBox card = new VBox(16);
        card.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-padding:28; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),12,0,0,3);");

        // Account selector
        Label accLabel = new Label("Select Account"); accLabel.getStyleClass().add("input-label");
        ComboBox<String> accountCombo = new ComboBox<>();
        accountCombo.setMaxWidth(Double.MAX_VALUE);
        accountCombo.getStyleClass().add("combo-box");

        try {
            List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            for (Account a : accounts) {
                accountCombo.getItems().add(a.getId() + "  (" + a.getAccountType() + " — RWF " + String.format("%,.2f", a.getBalance()) + ")");
            }
            if (!accounts.isEmpty()) {
                int sel = 0;
                if (preselectedId != null) {
                    for (int i = 0; i < accounts.size(); i++) {
                        if (accounts.get(i).getId().equals(preselectedId)) { sel = i; break; }
                    }
                }
                accountCombo.getSelectionModel().select(sel);
            }
        } catch (SQLException e) { setStatus("Error loading accounts: " + e.getMessage()); }

        // Amount field
        Label amtLabel = new Label("Amount (RWF)"); amtLabel.getStyleClass().add("input-label");
        TextField amtField = new TextField();
        amtField.setPromptText("e.g. 5000.00");
        amtField.getStyleClass().add("input-field");
        amtField.setMaxWidth(Double.MAX_VALUE);

        Label errorLabel = new Label(""); errorLabel.getStyleClass().add("status-error");

        // Submit button
        boolean isDeposit = "Deposit".equals(type);
        Button submitBtn = new Button(type.toUpperCase());
        submitBtn.getStyleClass().add(isDeposit ? "btn-primary" : "btn-accent");
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setStyle("-fx-pref-height:44px; -fx-font-size:14px;");

        List<Account> accountsRef;
        try {
            accountsRef = accountDAO.findByCustomerId(currentCustomer.getId());
        } catch (SQLException e) {
            accountsRef = List.of();
        }
        final List<Account> accounts = accountsRef;

        submitBtn.setOnAction(e -> {
            errorLabel.setText("");
            int idx = accountCombo.getSelectionModel().getSelectedIndex();
            if (idx < 0 || accounts.isEmpty()) { errorLabel.setText("Please select an account."); return; }
            String accountId = accounts.get(idx).getId();

            double amount;
            try {
                amount = Double.parseDouble(amtField.getText().trim());
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                errorLabel.setText("Enter a valid amount greater than zero.");
                return;
            }

            String refId = UUID.randomUUID().toString();
            try {
                if (isDeposit) {
                    transactionDAO.deposit(accountId, amount, refId);
                    setStatus("Deposited RWF " + String.format("%,.2f", amount));
                    alert(Alert.AlertType.INFORMATION, "Deposit Successful",
                            "Amount: RWF " + String.format("%,.2f", amount) + "\nReference: " + refId);
                } else {
                    Account acc = accountDAO.findById(accountId);
                    if (acc.getBalance() < amount) {
                        errorLabel.setText("Insufficient balance. Available: RWF " + String.format("%,.2f", acc.getBalance()));
                        return;
                    }
                    transactionDAO.withdraw(accountId, amount, refId);
                    setStatus("Withdrew RWF " + String.format("%,.2f", amount));
                    alert(Alert.AlertType.INFORMATION, "Withdrawal Successful",
                            "Amount: RWF " + String.format("%,.2f", amount) + "\nReference: " + refId);
                }
                refreshBalanceBadge();
                amtField.clear();
            } catch (IllegalStateException ex) {
                errorLabel.setText("Rejected: " + ex.getMessage());
            } catch (SQLException ex) {
                errorLabel.setText("Database error: " + ex.getMessage());
            }
        });

        card.getChildren().addAll(accLabel, accountCombo, amtLabel, amtField, errorLabel, submitBtn);
        root.getChildren().add(card);

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return sp;
    }

    private ScrollPane buildTransferPanel() {
        VBox root = new VBox(20);
        root.setMaxWidth(480);

        VBox card = new VBox(16);
        card.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-padding:28; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),12,0,0,3);");

        Label fromLabel = new Label("From Account"); fromLabel.getStyleClass().add("input-label");
        ComboBox<String> fromCombo = new ComboBox<>();
        fromCombo.setMaxWidth(Double.MAX_VALUE);
        fromCombo.getStyleClass().add("combo-box");

        List<Account> accounts;
        try {
            accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            for (Account a : accounts)
                fromCombo.getItems().add(a.getId() + "  (" + a.getAccountType() + " — RWF " + String.format("%,.2f", a.getBalance()) + ")");
            if (!accounts.isEmpty()) fromCombo.getSelectionModel().selectFirst();
        } catch (SQLException e) {
            accounts = List.of();
        }

        Label toLabel = new Label("Destination Account ID"); toLabel.getStyleClass().add("input-label");
        TextField toField = new TextField();
        toField.setPromptText("Paste destination account ID");
        toField.getStyleClass().add("input-field");
        toField.setMaxWidth(Double.MAX_VALUE);

        Label amtLabel = new Label("Amount (RWF)"); amtLabel.getStyleClass().add("input-label");
        TextField amtField = new TextField();
        amtField.setPromptText("e.g. 10000.00");
        amtField.getStyleClass().add("input-field");
        amtField.setMaxWidth(Double.MAX_VALUE);

        Label errorLabel = new Label(""); errorLabel.getStyleClass().add("status-error");

        Button submitBtn = new Button("TRANSFER"); submitBtn.getStyleClass().add("btn-accent");
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setStyle("-fx-pref-height:44px; -fx-font-size:14px;");

        final List<Account> finalAccounts = accounts;
        submitBtn.setOnAction(e -> {
            errorLabel.setText("");
            int idx = fromCombo.getSelectionModel().getSelectedIndex();
            if (idx < 0 || finalAccounts.isEmpty()) { errorLabel.setText("Select source account."); return; }
            String fromId = finalAccounts.get(idx).getId();
            String toId = toField.getText().trim();
            if (toId.isEmpty()) { errorLabel.setText("Enter destination account ID."); return; }

            double amount;
            try {
                amount = Double.parseDouble(amtField.getText().trim());
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) { errorLabel.setText("Enter a valid amount."); return; }

            String refId = UUID.randomUUID().toString();
            try {
                Account from = accountDAO.findById(fromId);
                if (from.getBalance() < amount) {
                    errorLabel.setText("Insufficient balance. Available: RWF " + String.format("%,.2f", from.getBalance()));
                    return;
                }
                transactionDAO.transfer(fromId, toId, amount, refId);
                refreshBalanceBadge();
                setStatus("Transferred RWF " + String.format("%,.2f", amount));
                alert(Alert.AlertType.INFORMATION, "Transfer Successful",
                        "Amount: RWF " + String.format("%,.2f", amount) + "\nTo: " + toId + "\nReference: " + refId);
                amtField.clear(); toField.clear();
            } catch (IllegalStateException ex) { errorLabel.setText("Rejected: " + ex.getMessage());
            } catch (SQLException ex) { errorLabel.setText("Database error: " + ex.getMessage()); }
        });

        card.getChildren().addAll(fromLabel, fromCombo, toLabel, toField, amtLabel, amtField, errorLabel, submitBtn);
        root.getChildren().add(card);

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return sp;
    }

    // ── HISTORY ──────────────────────────────────────────────────

    @FXML public void showHistory() {
        setPage("Transaction History", buildHistoryPanel());
    }

    private ScrollPane buildHistoryPanel() {
        VBox root = new VBox(16);

        // Account selector
        HBox selector = new HBox(10);
        selector.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label("Account:"); lbl.getStyleClass().add("input-label");
        ComboBox<String> combo = new ComboBox<>();
        combo.getStyleClass().add("combo-box");
        combo.setMinWidth(300);

        List<Account> accounts;
        try {
            accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            for (Account a : accounts)
                combo.getItems().add(a.getId() + "  (" + a.getAccountType() + ")");
            if (!accounts.isEmpty()) combo.getSelectionModel().selectFirst();
        } catch (SQLException e) { accounts = List.of(); }

        final List<Account> finalAccounts = accounts;

        // Table
        TableView<Transaction> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color:white; -fx-background-radius:10;");
        table.setPrefHeight(380);

        TableColumn<Transaction, String> refCol = new TableColumn<>("Reference ID");
        refCol.setCellValueFactory(new PropertyValueFactory<>("referenceId"));

        TableColumn<Transaction, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setMaxWidth(120);

        TableColumn<Transaction, Double> amtCol = new TableColumn<>("Amount (RWF)");
        amtCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amtCol.setMaxWidth(150);

        TableColumn<Transaction, LocalDateTime> tsCol = new TableColumn<>("Date & Time");
        tsCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        table.getColumns().addAll(refCol, typeCol, amtCol, tsCol);

        Button loadBtn = new Button("Load"); loadBtn.getStyleClass().add("btn-primary");
        loadBtn.setOnAction(e -> {
            int idx = combo.getSelectionModel().getSelectedIndex();
            if (idx < 0 || finalAccounts.isEmpty()) return;
            try {
                List<Transaction> txns = transactionDAO.findByAccountId(finalAccounts.get(idx).getId());
                table.setItems(FXCollections.observableArrayList(txns));
                setStatus("Loaded " + txns.size() + " transactions.");
            } catch (SQLException ex) { setStatus("Error: " + ex.getMessage()); }
        });

        selector.getChildren().addAll(lbl, combo, loadBtn);
        root.getChildren().addAll(selector, table);

        if (!finalAccounts.isEmpty()) {
            try {
                List<Transaction> txns = transactionDAO.findByAccountId(finalAccounts.get(0).getId());
                table.setItems(FXCollections.observableArrayList(txns));
            } catch (SQLException ignored) {}
        }

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return sp;
    }

    // ── REPORTS ──────────────────────────────────────────────────

    @FXML public void showDailySummary() {
        setPage("Daily Summary", buildDailySummaryPanel());
    }

    private ScrollPane buildDailySummaryPanel() {
        VBox root = new VBox(16);

        try {
            List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            double totalIn = 0, totalOut = 0;

            for (Account acc : accounts) {
                for (Transaction t : transactionDAO.findByAccountId(acc.getId())) {
                    if ("Deposit".equalsIgnoreCase(t.getType()))       totalIn  += t.getAmount();
                    else if ("Withdraw".equalsIgnoreCase(t.getType())) totalOut += t.getAmount();
                }
            }

            HBox stats = new HBox(16);
            stats.getChildren().addAll(
                    statCard("Total Deposits",     "RWF " + String.format("%,.2f", totalIn),          "All time",      "card-green"),
                    statCard("Total Withdrawals",  "RWF " + String.format("%,.2f", totalOut),         "All time",      "card-amber"),
                    statCard("Net Flow",           "RWF " + String.format("%,.2f", totalIn - totalOut),"All time",     "card-dark")
            );
            for (javafx.scene.Node n : stats.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
            root.getChildren().add(stats);

        } catch (SQLException e) {
            root.getChildren().add(new Label("Error: " + e.getMessage()));
        }

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return sp;
    }

    @FXML public void showStatement() {
        setPage("Account Statement", buildStatementPanel());
    }

    private ScrollPane buildStatementPanel() {
        VBox root = new VBox(16);
        try {
            List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            for (Account acc : accounts) {
                VBox accBlock = new VBox(10);
                accBlock.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-padding:20; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),12,0,0,3);");

                Label header = new Label(acc.getAccountType() + " — " + acc.getId());
                header.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#1a5c2e;");
                Label bal = new Label("Balance: RWF " + String.format("%,.2f", acc.getBalance()));
                bal.setStyle("-fx-font-size:13px; -fx-text-fill:#4a6a5a;");
                accBlock.getChildren().addAll(header, bal);

                List<Transaction> txns = transactionDAO.findByAccountId(acc.getId());
                for (Transaction t : txns) accBlock.getChildren().add(buildTxRow(t));

                root.getChildren().add(accBlock);
            }
        } catch (SQLException e) { root.getChildren().add(new Label("Error: " + e.getMessage())); }

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return sp;
    }

    @FXML public void exportCSV() {
        try {
            List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            String fileName = "igire_transactions_" + LocalDate.now() + ".csv";
            try (PrintWriter pw = new PrintWriter(new FileWriter(fileName))) {
                pw.println("Reference ID,Account ID,Type,Amount,Timestamp");
                for (Account acc : accounts) {
                    for (Transaction t : transactionDAO.findByAccountId(acc.getId())) {
                        pw.printf("%s,%s,%s,%.2f,%s%n",
                                t.getReferenceId(), acc.getId(), t.getType(),
                                t.getAmount(), t.getTimestamp());
                    }
                }
            }
            setStatus("Exported to " + fileName);
            alert(Alert.AlertType.INFORMATION, "Export Complete", "Saved to:\n" + fileName);
        } catch (Exception e) { alert(Alert.AlertType.ERROR, "Export Failed", e.getMessage()); }
    }

    // ── CHANGE PIN ───────────────────────────────────────────────

    @FXML public void showChangePin() {
        setPage("Change PIN", buildChangePinPanel());
    }

    private VBox buildChangePinPanel() {
        VBox root = new VBox(20);
        root.setMaxWidth(420);

        VBox card = new VBox(16);
        card.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-padding:28; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),12,0,0,3);");

        Label currentLabel = new Label("Current PIN"); currentLabel.getStyleClass().add("input-label");
        PasswordField currentField = new PasswordField(); currentField.getStyleClass().add("input-field"); currentField.setMaxWidth(Double.MAX_VALUE);

        Label newLabel = new Label("New PIN (5 digits)"); newLabel.getStyleClass().add("input-label");
        PasswordField newField = new PasswordField(); newField.getStyleClass().add("input-field"); newField.setMaxWidth(Double.MAX_VALUE);

        Label confirmLabel = new Label("Confirm New PIN"); confirmLabel.getStyleClass().add("input-label");
        PasswordField confirmField = new PasswordField(); confirmField.getStyleClass().add("input-field"); confirmField.setMaxWidth(Double.MAX_VALUE);

        Label errorLabel = new Label(""); errorLabel.getStyleClass().add("status-error");

        Button saveBtn = new Button("SAVE NEW PIN"); saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setStyle("-fx-pref-height:44px; -fx-font-size:14px;");

        saveBtn.setOnAction(e -> {
            errorLabel.setText("");
            String cur  = currentField.getText();
            String nw   = newField.getText();
            String conf = confirmField.getText();

            if (!hashPin(cur).equals(currentCustomer.getPinHash())) {
                errorLabel.setText("Current PIN is incorrect."); return;
            }
            if (nw.length() != 5 || !nw.matches("\\d+")) {
                errorLabel.setText("New PIN must be exactly 5 digits."); return;
            }
            if (!nw.equals(conf)) {
                errorLabel.setText("New PINs do not match."); return;
            }
            if (hashPin(nw).equals(currentCustomer.getPinHash())) {
                errorLabel.setText("New PIN cannot be same as current."); return;
            }

            try {
                currentCustomer.setPinHash(hashPin(nw));
                customerDAO.update(currentCustomer);
                setStatus("PIN changed successfully.");
                alert(Alert.AlertType.INFORMATION, "Success", "Your PIN has been updated.");
                currentField.clear(); newField.clear(); confirmField.clear();
            } catch (SQLException ex) {
                errorLabel.setText("Database error: " + ex.getMessage());
            }
        });

        card.getChildren().addAll(currentLabel, currentField, newLabel, newField, confirmLabel, confirmField, errorLabel, saveBtn);
        root.getChildren().add(card);
        return root;
    }

    // ── LOGOUT ───────────────────────────────────────────────────

    @FXML public void logout() {
        Stage stage = (Stage) statusLabel.getScene().getWindow();
        stage.close();
        MainApp.showLogin();
    }
}