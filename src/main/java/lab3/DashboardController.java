package lab3;

import com.igirepay.igirepay.MainApp;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import lab1.*;
import lab2.AccountDAO;
import lab2.CustomerDAO;
import lab2.LoanDAO;
import lab2.TransactionDAO;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class DashboardController {

    @FXML private Label     welcomeLabel;
    @FXML private Label     pageTitle;
    @FXML private Label     balanceBadge;
    @FXML private Label     statusLabel;
    @FXML private StackPane contentPane;

    private Customer             currentCustomer;
    private final CustomerDAO    customerDAO    = new CustomerDAO();
    private final AccountDAO     accountDAO     = new AccountDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final LoanDAO        loanDAO        = new LoanDAO();

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

    private void setStatus(String msg) { statusLabel.setText(msg); }

    private void refreshBalanceBadge() {
        try {
            List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            double total = accounts.stream().mapToDouble(Account::getBalance).sum();
            balanceBadge.setText("Total: RWF " + String.format("%,.2f", total));
        } catch (SQLException e) { balanceBadge.setText("Total: —"); }
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
        } catch (Exception e) { throw new RuntimeException("Hashing failed", e); }
    }

    // ── HOME ─────────────────────────────────────────────────────

    @FXML public void showHome() { setPage("Dashboard", buildHomePanel()); }

    private ScrollPane buildHomePanel() {
        VBox root = new VBox(20);

        try {
            List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            double total   = accounts.stream().mapToDouble(Account::getBalance).sum();
            // ✅ Fix: compare against actual stored values "Wallet" and "Saving"
            long wallets  = accounts.stream().filter(a -> "WALLET".equalsIgnoreCase(a.getAccountType())).count();
            long savings  = accounts.stream().filter(a -> "SAVINGS".equalsIgnoreCase(a.getAccountType())).count();

            HBox stats = new HBox(16);
            stats.getChildren().addAll(
                    statCard("Total Balance",    "RWF " + String.format("%,.2f", total), "All accounts", "#1a5c2e"),
                    statCard("Wallet Accounts",  String.valueOf(wallets), "Active", "#c8860a"),
                    statCard("Savings Accounts", String.valueOf(savings), "Active", "#0f3d1e")
            );
            for (javafx.scene.Node n : stats.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
            root.getChildren().add(stats);
        } catch (SQLException e) {
            root.getChildren().add(errLabel("Could not load stats: " + e.getMessage()));
        }

        // Quick actions
        VBox actBox = new VBox(12);
        actBox.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-padding:20; " +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),12,0,0,3);");
        Label at = new Label("Quick Actions");
        at.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#1a3a2a;");
        HBox acts = new HBox(12);
        acts.setAlignment(Pos.CENTER_LEFT);
        acts.getChildren().addAll(
                quickBtn("⬆\nDeposit",  this::showDeposit),
                quickBtn("⬇\nWithdraw", this::showWithdraw),
                quickBtn("↔\nTransfer", this::showTransfer),
                quickBtn("📋\nHistory",  this::showHistory),
                quickBtn("📊\nReports",  this::showDailySummary)
        );
        actBox.getChildren().addAll(at, acts);
        root.getChildren().add(actBox);

        // Recent transactions
        try {
            List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            VBox histBox = new VBox(8);
            histBox.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-padding:20; " +
                    "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),12,0,0,3);");
            Label ht = new Label("Recent Transactions");
            ht.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#1a3a2a;");
            histBox.getChildren().add(ht);
            boolean found = false;
            for (Account a : accounts) {
                List<Transaction> txns = transactionDAO.findByAccountId(a.getId());
                int c = 0;
                for (Transaction t : txns) {
                    if (c++ >= 5) break;
                    histBox.getChildren().add(buildTxRow(t));
                    found = true;
                }
            }
            if (!found) {
                Label none = new Label("No transactions yet.");
                none.setStyle("-fx-text-fill:#8aaa9a; -fx-font-size:13px;");
                histBox.getChildren().add(none);
            }
            root.getChildren().add(histBox);
        } catch (SQLException ignored) {}

        return scrollWrap(root);
    }

    private VBox statCard(String title, String value, String sub, String color) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color:" + color + "; -fx-background-radius:12; -fx-padding:22; " +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),12,0,0,4);");
        card.setMaxWidth(Double.MAX_VALUE);
        Label t = new Label(title); t.setStyle("-fx-font-size:12px; -fx-text-fill:rgba(255,255,255,0.7); -fx-font-weight:bold;");
        Label v = new Label(value); v.setStyle("-fx-font-size:24px; -fx-font-weight:bold; -fx-text-fill:white;");
        Label s = new Label(sub);   s.setStyle("-fx-font-size:11px; -fx-text-fill:rgba(255,255,255,0.5);");
        card.getChildren().addAll(t, v, s);
        return card;
    }

    private Button quickBtn(String text, Runnable action) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:#f0f9f3; -fx-text-fill:#1a5c2e; -fx-font-size:13px; " +
                "-fx-font-weight:bold; -fx-background-radius:10; -fx-cursor:hand; " +
                "-fx-padding:16 10; -fx-pref-width:110px; -fx-pref-height:75px; " +
                "-fx-border-color:#c8ecd4; -fx-border-radius:10; -fx-border-width:1.5; -fx-alignment:CENTER;");
        b.setOnAction(e -> action.run());
        return b;
    }

    private HBox buildTxRow(Transaction t) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding:8 0; -fx-border-color:transparent transparent #f0f0f0 transparent; -fx-border-width:0 0 1 0;");

        String type = t.getType() != null ? t.getType() : "Unknown";
        String color = type.toLowerCase().contains("deposit") || type.toLowerCase().contains("in")
                ? "-fx-background-color:#e8f5ec; -fx-text-fill:#1a5c2e;"
                : type.toLowerCase().contains("withdraw") || type.toLowerCase().contains("out")
                  ? "-fx-background-color:#fdecea; -fx-text-fill:#c0392b;"
                  : "-fx-background-color:#fff3e0; -fx-text-fill:#c8860a;";

        Label badge = new Label(type);
        badge.setStyle(color + "-fx-font-size:11px; -fx-font-weight:bold; -fx-padding:3 10; -fx-background-radius:12;");
        badge.setPrefWidth(100);

        Label ref = new Label(t.getReferenceId() != null ? t.getReferenceId() : "—");
        ref.setStyle("-fx-font-size:11px; -fx-text-fill:#8aaa9a;");
        HBox.setHgrow(ref, Priority.ALWAYS);

        Label amt = new Label("RWF " + String.format("%,.2f", t.getAmount()));
        amt.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#1a3a2a;");

        row.getChildren().addAll(badge, ref, amt);
        return row;
    }

    // ── ACCOUNTS ─────────────────────────────────────────────────

    @FXML public void showAccounts() { setPage("My Accounts", buildAccountsPanel()); }

    private ScrollPane buildAccountsPanel() {
        VBox root = new VBox(16);
        try {
            List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            if (accounts.isEmpty()) {
                Label e = new Label("No accounts yet. Use 'Create Account' to get started.");
                e.setStyle("-fx-text-fill:#8aaa9a; -fx-font-size:14px;");
                root.getChildren().add(e);
            } else {
                for (Account a : accounts) {
                    // ✅ Fix: checks actual account type from DB
                    boolean isWallet = "WALLET".equalsIgnoreCase(a.getAccountType());
                    String bg = isWallet ? "#1a5c2e" : "#c8860a";

                    VBox card = new VBox(10);
                    card.setStyle("-fx-background-color:" + bg + "; -fx-background-radius:12; -fx-padding:22; " +
                            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),12,0,0,4);");

                    Label type = new Label(a.getAccountType() + " Account");
                    type.setStyle("-fx-font-size:12px; -fx-text-fill:rgba(255,255,255,0.7); -fx-font-weight:bold;");

                    Label bal = new Label("RWF " + String.format("%,.2f", a.getBalance()));
                    bal.setStyle("-fx-font-size:28px; -fx-font-weight:bold; -fx-text-fill:white;");

                    Label id = new Label("ID: " + a.getId());
                    id.setStyle("-fx-font-size:10px; -fx-text-fill:rgba(255,255,255,0.5);");

                    String btnStyle = "-fx-text-fill:white; -fx-border-color:rgba(255,255,255,0.5); " +
                            "-fx-background-color:transparent; -fx-background-radius:8; " +
                            "-fx-border-radius:8; -fx-border-width:1.5; -fx-cursor:hand; -fx-padding:8 16;";
                    Button dep = new Button("Deposit");  dep.setStyle(btnStyle);
                    Button wit = new Button("Withdraw"); wit.setStyle(btnStyle);
                    Button del = new Button("Delete");
                    del.setStyle("-fx-background-color:#8b1a1a; -fx-text-fill:white; " +
                            "-fx-background-radius:8; -fx-cursor:hand; -fx-padding:8 16;");

                    String accId = a.getId();
                    dep.setOnAction(e -> openDeposit(accId));
                    wit.setOnAction(e -> openWithdraw(accId));
                    del.setOnAction(e -> deleteAccount(accId));

                    HBox btnRow = new HBox(10);
                    btnRow.getChildren().addAll(dep, wit, del);
                    card.getChildren().addAll(type, bal, id, btnRow);
                    root.getChildren().add(card);
                }
            }
        } catch (SQLException e) {
            root.getChildren().add(errLabel("Database error: " + e.getMessage()));
        }
        return scrollWrap(root);
    }

    @FXML public void showCreateAccount() { setPage("Create Account", buildCreateAccountPanel()); }

    private VBox buildCreateAccountPanel() {
        VBox root = new VBox(20);
        root.setMaxWidth(500);

        VBox wc = new VBox(10);
        wc.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-padding:24; " +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),12,0,0,3);");
        Label wt = new Label("💳  Wallet Account");
        wt.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#1a5c2e;");
        Label wd = new Label("Instant transfers and deposits. No withdrawal fees.");
        wd.setStyle("-fx-text-fill:#6a8a7a; -fx-font-size:13px;");
        Button bw = new Button("Create Wallet");
        bw.setStyle("-fx-background-color:#1a5c2e; -fx-text-fill:white; -fx-font-size:13px; " +
                "-fx-font-weight:bold; -fx-background-radius:8; -fx-cursor:hand; -fx-padding:10 20;");
        bw.setOnAction(e -> createWallet());
        wc.getChildren().addAll(wt, wd, bw);

        VBox sc = new VBox(10);
        sc.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-padding:24; " +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),12,0,0,3);");
        Label st = new Label("🏦  Savings Account");
        st.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#c8860a;");
        Label sd = new Label("Save securely. Withdrawal fee applies. Limit: RWF 10,500 per transaction.");
        sd.setStyle("-fx-text-fill:#6a8a7a; -fx-font-size:13px;");
        Button bs = new Button("Create Savings");
        bs.setStyle("-fx-background-color:#c8860a; -fx-text-fill:white; -fx-font-size:13px; " +
                "-fx-font-weight:bold; -fx-background-radius:8; -fx-cursor:hand; -fx-padding:10 20;");
        bs.setOnAction(e -> createSavings());
        sc.getChildren().addAll(st, sd, bs);

        root.getChildren().addAll(wc, sc);
        return root;
    }

    private void createWallet() {
        try {
            List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            boolean hasWallet = accounts.stream().anyMatch(a -> "WALLET".equalsIgnoreCase(a.getAccountType()));
            if (hasWallet) {
                alert(Alert.AlertType.WARNING, "Not Allowed", "You already have a Wallet account.");
                return;
            }
            WalletAccount w = new WalletAccount(currentCustomer.getId());
            accountDAO.create(w);
            refreshBalanceBadge();
            setStatus("Wallet created.");
            alert(Alert.AlertType.INFORMATION, "Success", "Wallet account created!\nID: " + w.getId());
            showAccounts();
        } catch (SQLException e) { alert(Alert.AlertType.ERROR, "Error", e.getMessage()); }
    }

    private void createSavings() {
        try {
            List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            boolean hasSavings = accounts.stream().anyMatch(a -> "SAVINGS".equalsIgnoreCase(a.getAccountType()));
            if (hasSavings) {
                alert(Alert.AlertType.WARNING, "Not Allowed", "You already have a Savings account.");
                return;
            }
            SavingsAccount s = new SavingsAccount(currentCustomer.getId());
            accountDAO.create(s);
            refreshBalanceBadge();
            setStatus("Savings created.");
            alert(Alert.AlertType.INFORMATION, "Success", "Savings account created!\nID: " + s.getId());
            showAccounts();
        } catch (SQLException e) { alert(Alert.AlertType.ERROR, "Error", e.getMessage()); }
    }

    private void deleteAccount(String accountId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Account");
        confirm.setHeaderText("Delete this account?");
        confirm.setContentText("Only accounts with zero balance can be deleted. This cannot be undone.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    accountDAO.delete(accountId);
                    refreshBalanceBadge();
                    setStatus("Account deleted.");
                    showAccounts();
                } catch (IllegalStateException e) {
                    alert(Alert.AlertType.WARNING, "Cannot Delete", e.getMessage());
                } catch (Exception e) {
                    alert(Alert.AlertType.ERROR, "Error", e.getMessage());
                }
            }
        });
    }

    // ── DEPOSIT / WITHDRAW ────────────────────────────────────────

    @FXML public void showDeposit()  { openDeposit(null); }
    @FXML public void showWithdraw() { openWithdraw(null); }

    private void openDeposit(String preId)  { setPage("Deposit Money",  buildTxPanel("Deposit",  preId)); }
    private void openWithdraw(String preId) { setPage("Withdraw Money", buildTxPanel("Withdraw", preId)); }

    // ✅ Shared panel for Deposit and Withdraw — includes Reference ID field for idempotency
    private ScrollPane buildTxPanel(String type, String preselectedId) {
        VBox root = new VBox(20);
        root.setMaxWidth(480);

        VBox card = new VBox(16);
        card.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-padding:28; " +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),12,0,0,3);");

        Label accLbl = lbl("Select Account");
        ComboBox<String> combo = new ComboBox<>();
        combo.setMaxWidth(Double.MAX_VALUE);

        List<Account> accounts;
        try {
            accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            for (Account a : accounts)
                combo.getItems().add(a.getId() + "  (" + a.getAccountType() +
                        " — RWF " + String.format("%,.2f", a.getBalance()) + ")");
            if (!accounts.isEmpty()) {
                int sel = 0;
                if (preselectedId != null)
                    for (int i = 0; i < accounts.size(); i++)
                        if (accounts.get(i).getId().equals(preselectedId)) { sel = i; break; }
                combo.getSelectionModel().select(sel);
            }
        } catch (SQLException e) { accounts = List.of(); }

        Label amtLbl = lbl("Amount (RWF)");
        TextField amtField = inp("e.g. 5000.00");

        // ✅ Reference ID field — user supplies it for idempotency
        // If left blank → UUID is auto-generated (still unique, idempotency still works)
//        Label refLbl = lbl("Reference ID  (optional — leave blank to auto-generate)");
        Label refHint = new Label("⚠  If you use the same Reference ID twice, the second transaction will be REJECTED and your balance will NOT change.");
        refHint.setStyle("-fx-text-fill:#c8860a; -fx-font-size:11px;");
        refHint.setWrapText(true);
        TextField refField = inp("e.g. PAY-2024-001  or leave blank");

        Label errLbl = new Label("");
        errLbl.setStyle("-fx-text-fill:#c0392b; -fx-font-size:12px; -fx-font-weight:bold;");
        errLbl.setWrapText(true);
        errLbl.setMaxWidth(Double.MAX_VALUE);

        boolean isDeposit = "Deposit".equals(type);
        Button btn = new Button(type.toUpperCase());
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle((isDeposit ? "-fx-background-color:#1a5c2e;" : "-fx-background-color:#c8860a;") +
                "-fx-text-fill:white; -fx-font-size:14px; -fx-font-weight:bold; " +
                "-fx-background-radius:8; -fx-cursor:hand; -fx-pref-height:44px;");

        final List<Account> finalAccounts = accounts;

        btn.setOnAction(e -> {
            errLbl.setText("");

            // Exception: no account selected
            int idx = combo.getSelectionModel().getSelectedIndex();
            if (idx < 0 || finalAccounts.isEmpty()) {
                errLbl.setText("Please select an account.");
                return;
            }
            String accountId = finalAccounts.get(idx).getId();

            // Exception: invalid amount
            double amount;
            try {
                amount = Double.parseDouble(amtField.getText().trim());
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                errLbl.setText("Invalid amount. Enter a number greater than zero.");
                return;
            }

            // Use typed ref ID or auto-generate
            String refId = refField.getText().trim();
//            if (refId.isEmpty()) refId = UUID.randomUUID().toString();

            try {
                if (isDeposit) {
                    transactionDAO.deposit(accountId, amount, refId);
                    setStatus("Deposited RWF " + String.format("%,.2f", amount));
                    alert(Alert.AlertType.INFORMATION, "Deposit Successful",
                            "Amount: RWF " + String.format("%,.2f", amount) + "\nReference: " + refId);
                } else {
                    transactionDAO.withdraw(accountId, amount, refId);
                    setStatus("Withdrew RWF " + String.format("%,.2f", amount));
                    alert(Alert.AlertType.INFORMATION, "Withdrawal Successful",
                            "Amount: RWF " + String.format("%,.2f", amount) + "\nReference: " + refId);
                }
                refreshBalanceBadge();
                amtField.clear();
                refField.clear();

            } catch (IllegalStateException ex) {
                // Covers: duplicate refId, insufficient balance
                errLbl.setText(ex.getMessage());
                setStatus("Rejected: " + ex.getMessage());
            } catch (IllegalArgumentException ex) {
                // Covers: invalid account ID, zero amount
                errLbl.setText(ex.getMessage());
            } catch (SQLException ex) {
                // Exception: SQL / DB connection failure
                errLbl.setText("Database error: " + ex.getMessage());
                setStatus("Database error.");
            }
        });

        card.getChildren().addAll(accLbl, combo, amtLbl, amtField,  refHint, refField, errLbl, btn);
        root.getChildren().add(card);
        return scrollWrap(root);
    }

    // ── TRANSFER ─────────────────────────────────────────────────

    @FXML public void showTransfer() { setPage("Transfer Money", buildTransferPanel()); }

    private ScrollPane buildTransferPanel() {
        VBox root = new VBox(20);
        root.setMaxWidth(480);

        VBox card = new VBox(16);
        card.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-padding:28; " +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),12,0,0,3);");

        Label fromLbl = lbl("From Account");
        ComboBox<String> fromCombo = new ComboBox<>();
        fromCombo.setMaxWidth(Double.MAX_VALUE);

        List<Account> accounts;
        try {
            accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            for (Account a : accounts)
                fromCombo.getItems().add(a.getId() + "  (" + a.getAccountType() +
                        " — RWF " + String.format("%,.2f", a.getBalance()) + ")");
            if (!accounts.isEmpty()) fromCombo.getSelectionModel().selectFirst();
        } catch (SQLException e) { accounts = List.of(); }

        Label toLbl  = lbl("Recipient — Phone Number or Account ID");
        TextField toField  = inp("e.g. 0781234567  or paste account ID");
        Label amtLbl = lbl("Amount (RWF)");
        TextField amtField = inp("e.g. 10000.00");

        // ✅ Reference ID for transfer idempotency
        Label refLbl  = lbl("Reference ID  (optional — leave blank to auto-generate)");
        Label refHint = new Label("⚠  Same Reference ID used twice = second transfer REJECTED, balances unchanged.");
        refHint.setStyle("-fx-text-fill:#c8860a; -fx-font-size:11px;");
        TextField refField = inp("e.g. TRF-001");

        Label errLbl = new Label("");
        errLbl.setStyle("-fx-text-fill:#c0392b; -fx-font-size:12px; -fx-font-weight:bold;");
        errLbl.setWrapText(true);

        Button btn = new Button("TRANSFER");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color:#c8860a; -fx-text-fill:white; -fx-font-size:14px; " +
                "-fx-font-weight:bold; -fx-background-radius:8; -fx-cursor:hand; -fx-pref-height:44px;");

        final List<Account> finalAccounts = accounts;
        btn.setOnAction(e -> {
            errLbl.setText("");
            int idx = fromCombo.getSelectionModel().getSelectedIndex();
            if (idx < 0 || finalAccounts.isEmpty()) { errLbl.setText("Select source account."); return; }
            String fromId = finalAccounts.get(idx).getId();
            String toInput = toField.getText().trim();
            if (toInput.isEmpty()) { errLbl.setText("Enter recipient phone number or account ID."); return; }

            double amount;
            try {
                amount = Double.parseDouble(amtField.getText().trim());
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) { errLbl.setText("Enter a valid amount."); return; }

            String refId = refField.getText().trim();

            try {
                // Resolve phone number to wallet account ID if needed
                String toId = toInput;
                if (toInput.matches("\\d{10,}")) { // looks like a phone number
                    Customer recipient = customerDAO.findByPhone(toInput);
                    if (recipient == null) {
                        errLbl.setText("No customer found with phone: " + toInput);
                        return;
                    }
                    List<Account> recipientAccounts = accountDAO.findByCustomerId(recipient.getId());
                    Account wallet = recipientAccounts.stream()
                            .filter(a -> "WALLET".equalsIgnoreCase(a.getAccountType()))
                            .findFirst().orElse(null);
                    if (wallet == null) {
                        errLbl.setText("Recipient has no Wallet account.");
                        return;
                    }
                    toId = wallet.getId();
                }

                transactionDAO.transfer(fromId, toId, amount, refId);
                refreshBalanceBadge();
                setStatus("Transferred RWF " + String.format("%,.2f", amount));
                alert(Alert.AlertType.INFORMATION, "Transfer Successful",
                        "Sent: RWF " + String.format("%,.2f", amount) +
                                "\nTo: " + toInput + "\nReference: " + refId);
                amtField.clear(); toField.clear(); refField.clear();
            } catch (IllegalStateException ex) {
                errLbl.setText(ex.getMessage());
                setStatus("Rejected.");
            } catch (IllegalArgumentException ex) {
                errLbl.setText(ex.getMessage());
            } catch (SQLException ex) {
                errLbl.setText("Database error: " + ex.getMessage());
            }
        });

        card.getChildren().addAll(fromLbl, fromCombo, toLbl, toField, amtLbl, amtField,
                refLbl, refHint, refField, errLbl, btn);
        root.getChildren().add(card);
        return scrollWrap(root);
    }

    // ── HISTORY ──────────────────────────────────────────────────

    @FXML public void showHistory() { setPage("Transaction History", buildHistoryPanel()); }

    private ScrollPane buildHistoryPanel() {
        VBox root = new VBox(16);

        HBox selector = new HBox(10);
        selector.setAlignment(Pos.CENTER_LEFT);
        Label accLbl = lbl("Account:");
        ComboBox<String> combo = new ComboBox<>();
        combo.setMinWidth(340);

        List<Account> accounts;
        try {
            accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            for (Account a : accounts)
                combo.getItems().add(a.getId() + "  (" + a.getAccountType() + ")");
            if (!accounts.isEmpty()) combo.getSelectionModel().selectFirst();
        } catch (SQLException e) {
            accounts = List.of();
            root.getChildren().add(errLabel("Could not load accounts: " + e.getMessage()));
        }

        final List<Account> finalAccounts = accounts;

        // ✅ Fix: use a simple VBox list instead of TableView
        // TableView with PropertyValueFactory requires JavaFX property bindings.
        // Using a plain VBox avoids that complexity and always shows data correctly.
        VBox txList = new VBox(0);
        txList.setStyle("-fx-background-color:white; -fx-background-radius:10; -fx-padding:0;");

        // Header row
        HBox header = new HBox(0);
        header.setStyle("-fx-background-color:#1a5c2e; -fx-padding:10 14;");
        Label h1 = new Label("Reference ID");     h1.setStyle("color1"); HBox.setHgrow(h1, Priority.ALWAYS);
        Label h2 = new Label("Type");             h2.setMinWidth(110); h2.setStyle("color1");
        Label h3 = new Label("Amount (RWF)");     h3.setMinWidth(130); h3.setStyle("color1");
        Label h4 = new Label("Date & Time");      h4.setMinWidth(160); h4.setStyle("color1");
        for (Label h : new Label[]{h1, h2, h3, h4})
            h.setStyle("-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:12px; -fx-padding:0 8;");
        header.getChildren().addAll(h1, h2, h3, h4);
        txList.getChildren().add(header);

        Runnable loadTx = () -> {
            int idx = combo.getSelectionModel().getSelectedIndex();
            if (idx < 0 || finalAccounts.isEmpty()) return;
            // Remove all rows except header
            txList.getChildren().subList(1, txList.getChildren().size()).clear();
            try {
                List<Transaction> txns = transactionDAO.findByAccountId(finalAccounts.get(idx).getId());
                if (txns.isEmpty()) {
                    Label none = new Label("  No transactions found for this account.");
                    none.setStyle("-fx-padding:16; -fx-text-fill:#8aaa9a; -fx-font-size:13px;");
                    txList.getChildren().add(none);
                    setStatus("No transactions.");
                    return;
                }
                boolean odd = false;
                for (Transaction t : txns) {
                    HBox row = new HBox(0);
                    String rowBg = odd ? "#f8faf9" : "white";
                    row.setStyle("-fx-background-color:" + rowBg + "; -fx-padding:9 14;");
                    odd = !odd;

                    Label r1 = new Label(t.getReferenceId() != null ? t.getReferenceId() : "—");
                    r1.setStyle("-fx-font-size:11px; -fx-text-fill:#4a6a5a;");
                    HBox.setHgrow(r1, Priority.ALWAYS);

                    String type = t.getType() != null ? t.getType() : "—";
                    String badgeColor = type.toLowerCase().contains("deposit") || type.toLowerCase().contains("in")
                            ? "-fx-background-color:#e8f5ec; -fx-text-fill:#1a5c2e;"
                            : type.toLowerCase().contains("withdraw") || type.toLowerCase().contains("out")
                              ? "-fx-background-color:#fdecea; -fx-text-fill:#c0392b;"
                              : "-fx-background-color:#fff3e0; -fx-text-fill:#c8860a;";
                    Label r2 = new Label(type);
                    r2.setStyle(badgeColor + "-fx-font-size:11px; -fx-font-weight:bold; -fx-padding:2 8; -fx-background-radius:10;");
                    r2.setMinWidth(110);

                    Label r3 = new Label(String.format("%,.2f", t.getAmount()));
                    r3.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#1a3a2a;");
                    r3.setMinWidth(130);

                    String ts = t.getTimestamp() != null
                            ? t.getTimestamp().toString().replace("T", "  ").substring(0, Math.min(19, t.getTimestamp().toString().length()))
                            : "—";
                    Label r4 = new Label(ts);
                    r4.setStyle("-fx-font-size:11px; -fx-text-fill:#8aaa9a;");
                    r4.setMinWidth(160);

                    row.getChildren().addAll(r1, r2, r3, r4);
                    txList.getChildren().add(row);
                }
                setStatus("Loaded " + txns.size() + " transaction(s).");
            } catch (SQLException ex) {
                setStatus("Error loading history: " + ex.getMessage());
            }
        };

        Button loadBtn = new Button("Refresh");
        loadBtn.setStyle("-fx-background-color:#1a5c2e; -fx-text-fill:white; -fx-font-size:13px; " +
                "-fx-font-weight:bold; -fx-background-radius:8; -fx-cursor:hand; -fx-padding:9 18;");
        loadBtn.setOnAction(e -> loadTx.run());

        // Auto-load first account on open
        combo.getSelectionModel().selectedIndexProperty().addListener((obs, old, nw) -> loadTx.run());

        selector.getChildren().addAll(accLbl, combo, loadBtn);
        root.getChildren().addAll(selector, txList);

        // Trigger initial load
        if (!finalAccounts.isEmpty()) loadTx.run();

        return scrollWrap(root);
    }

    // ── REPORTS ──────────────────────────────────────────────────

    @FXML public void showDailySummary() { setPage("Daily Summary", buildDailySummaryPanel()); }

    private ScrollPane buildDailySummaryPanel() {
        VBox root = new VBox(16);
        try {
            List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            double in = 0, out = 0;
            for (Account acc : accounts) {
                for (Transaction t : transactionDAO.findByAccountId(acc.getId())) {
                    if (t.getType() != null && t.getType().toLowerCase().contains("deposit"))       in  += t.getAmount();
                    else if (t.getType() != null && t.getType().toLowerCase().contains("withdraw")) out += t.getAmount();
                }
            }
            HBox stats = new HBox(16);
            stats.getChildren().addAll(
                    statCard("Total Deposited",   "RWF " + String.format("%,.2f", in),      "All time", "#1a5c2e"),
                    statCard("Total Withdrawn",   "RWF " + String.format("%,.2f", out),     "All time", "#c8860a"),
                    statCard("Net Flow",          "RWF " + String.format("%,.2f", in - out),"All time", "#0f3d1e")
            );
            for (javafx.scene.Node n : stats.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
            root.getChildren().add(stats);
        } catch (SQLException e) { root.getChildren().add(errLabel("Error: " + e.getMessage())); }
        return scrollWrap(root);
    }

    @FXML public void showStatement() { setPage("Account Statement", buildStatementPanel()); }

    private ScrollPane buildStatementPanel() {
        VBox root = new VBox(16);
        try {
            List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            for (Account acc : accounts) {
                VBox block = new VBox(8);
                block.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-padding:20; " +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),12,0,0,3);");
                Label h = new Label(acc.getAccountType() + " — " + acc.getId());
                h.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#1a5c2e;");
                Label b = new Label("Balance: RWF " + String.format("%,.2f", acc.getBalance()));
                b.setStyle("-fx-font-size:13px; -fx-text-fill:#4a6a5a;");
                block.getChildren().addAll(h, b);
                for (Transaction t : transactionDAO.findByAccountId(acc.getId()))
                    block.getChildren().add(buildTxRow(t));
                root.getChildren().add(block);
            }
        } catch (SQLException e) { root.getChildren().add(errLabel("Error: " + e.getMessage())); }
        return scrollWrap(root);
    }

    @FXML public void exportCSV() {
        try {
            List<Account> accounts = accountDAO.findByCustomerId(currentCustomer.getId());
            String fileName = "igire_transactions_" + LocalDate.now() + ".csv";
            try (PrintWriter pw = new PrintWriter(new FileWriter(fileName))) {
                pw.println("Reference ID,Account ID,Type,Amount,Timestamp");
                for (Account acc : accounts)
                    for (Transaction t : transactionDAO.findByAccountId(acc.getId()))
                        pw.printf("%s,%s,%s,%.2f,%s%n",
                                t.getReferenceId(), acc.getId(), t.getType(),
                                t.getAmount(), t.getTimestamp());
            }
            setStatus("Exported to " + fileName);
            alert(Alert.AlertType.INFORMATION, "Export Complete", "Saved to:\n" + fileName);
        } catch (Exception e) { alert(Alert.AlertType.ERROR, "Export Failed", e.getMessage()); }
    }

    // ── CHANGE PIN ───────────────────────────────────────────────

    @FXML public void showChangePin() { setPage("Change PIN", buildChangePinPanel()); }

    // ── LOAN ─────────────────────────────────────────────────────

    @FXML public void showLoan() { setPage("Request a Loan", buildLoanPanel()); }

    private ScrollPane buildLoanPanel() {
        VBox root = new VBox(20);
        root.setMaxWidth(500);

        // Request form
        VBox form = new VBox(16);
        form.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-padding:28; " +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),12,0,0,3);");
        Label title = new Label("💰  Apply for a Loan");
        title.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#1a5c2e;");
        Label amtLbl = lbl("Loan Amount (RWF)");
        TextField amtField = inp("e.g. 50000");
        Label errLbl = new Label("");
        errLbl.setStyle("-fx-text-fill:#c0392b; -fx-font-size:12px; -fx-font-weight:bold;");
        errLbl.setWrapText(true);
        Button applyBtn = new Button("APPLY FOR LOAN");
        applyBtn.setMaxWidth(Double.MAX_VALUE);
        applyBtn.setStyle("-fx-background-color:#1a5c2e; -fx-text-fill:white; -fx-font-size:14px; " +
                "-fx-font-weight:bold; -fx-background-radius:8; -fx-cursor:hand; -fx-pref-height:44px;");

        applyBtn.setOnAction(e -> {
            errLbl.setText("");
            double amount;
            try {
                amount = Double.parseDouble(amtField.getText().trim());
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                errLbl.setText("Enter a valid amount greater than zero.");
                return;
            }
            try {
                Loan loan = new Loan(currentCustomer.getId(), amount);
                loanDAO.create(loan);
                amtField.clear();
                setStatus("Loan request submitted.");
                alert(Alert.AlertType.INFORMATION, "Loan Submitted",
                        "Your loan request of RWF " + String.format("%,.2f", amount) +
                                " has been submitted.\nStatus: PENDING");
                showLoan(); // refresh to show updated history
            } catch (SQLException ex) {
                errLbl.setText("Database error: " + ex.getMessage());
            }
        });

        form.getChildren().addAll(title, amtLbl, amtField, errLbl, applyBtn);
        root.getChildren().add(form);

        // Loan history
        VBox histBox = new VBox(0);
        histBox.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-padding:0; " +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),12,0,0,3);");
        Label histTitle = new Label("My Loan Requests");
        histTitle.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#1a3a2a; -fx-padding:16 20 10 20;");
        histBox.getChildren().add(histTitle);

        try {
            List<Loan> loans = loanDAO.findByCustomerId(currentCustomer.getId());
            if (loans.isEmpty()) {
                Label none = new Label("No loan requests yet.");
                none.setStyle("-fx-text-fill:#8aaa9a; -fx-font-size:13px; -fx-padding:10 20;");
                histBox.getChildren().add(none);
            } else {
                for (Loan l : loans) {
                    HBox row = new HBox(12);
                    row.setStyle("-fx-padding:10 20; -fx-border-color:transparent transparent #f0f0f0 transparent; -fx-border-width:0 0 1 0;");
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    String badgeColor = "APPROVED".equals(l.getStatus())
                            ? "-fx-background-color:#e8f5ec; -fx-text-fill:#1a5c2e;"
                            : "REJECTED".equals(l.getStatus())
                              ? "-fx-background-color:#fdecea; -fx-text-fill:#c0392b;"
                              : "-fx-background-color:#fff3e0; -fx-text-fill:#c8860a;";
                    Label status = new Label(l.getStatus());
                    status.setStyle(badgeColor + "-fx-font-size:11px; -fx-font-weight:bold; -fx-padding:3 10; -fx-background-radius:12;");
                    status.setMinWidth(80);
                    Label amt = new Label("RWF " + String.format("%,.2f", l.getAmount()));
                    amt.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#1a3a2a;");
                    HBox.setHgrow(amt, javafx.scene.layout.Priority.ALWAYS);
                    String ts = l.getRequestedAt() != null ? l.getRequestedAt().toString().replace("T", "  ").substring(0, 16) : "—";
                    Label date = new Label(ts);
                    date.setStyle("-fx-font-size:11px; -fx-text-fill:#8aaa9a;");
                    row.getChildren().addAll(status, amt, date);
                    histBox.getChildren().add(row);
                }
            }
        } catch (SQLException e) {
            histBox.getChildren().add(errLabel("Error loading loans: " + e.getMessage()));
        }
        root.getChildren().add(histBox);
        return scrollWrap(root);
    }

    private VBox buildChangePinPanel() {
        VBox root = new VBox(20);
        root.setMaxWidth(420);

        VBox card = new VBox(16);
        card.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-padding:28; " +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),12,0,0,3);");

        Label cl = lbl("Current PIN");       PasswordField cf = pin();
        Label nl = lbl("New PIN (5 digits)"); PasswordField nf = pin();
        Label kl = lbl("Confirm New PIN");    PasswordField kf = pin();

        Label err = new Label("");
        err.setStyle("-fx-text-fill:#c0392b; -fx-font-size:12px; -fx-font-weight:bold;");
        err.setWrapText(true);

        Button save = new Button("SAVE NEW PIN");
        save.setMaxWidth(Double.MAX_VALUE);
        save.setStyle("-fx-background-color:#1a5c2e; -fx-text-fill:white; -fx-font-size:14px; " +
                "-fx-font-weight:bold; -fx-background-radius:8; -fx-cursor:hand; -fx-pref-height:44px;");

        save.setOnAction(e -> {
            err.setText("");
            if (!hashPin(cf.getText()).equals(currentCustomer.getPinHash())) { err.setText("Current PIN is incorrect."); return; }
            if (nf.getText().length() != 5 || !nf.getText().matches("\\d+")) { err.setText("New PIN must be exactly 5 digits."); return; }
            if (!nf.getText().equals(kf.getText())) { err.setText("New PINs do not match."); return; }
            if (hashPin(nf.getText()).equals(currentCustomer.getPinHash())) { err.setText("New PIN cannot be same as current."); return; }
            try {
                currentCustomer.setPinHash(hashPin(nf.getText()));
                customerDAO.update(currentCustomer);
                setStatus("PIN changed.");
                alert(Alert.AlertType.INFORMATION, "Success", "PIN updated successfully.");
                cf.clear(); nf.clear(); kf.clear();
            } catch (SQLException ex) { err.setText("Database error: " + ex.getMessage()); }
        });

        card.getChildren().addAll(cl, cf, nl, nf, kl, kf, err, save);
        root.getChildren().add(card);
        return root;
    }

    // ── LOGOUT ───────────────────────────────────────────────────

    @FXML public void logout() {
        Stage stage = (Stage) statusLabel.getScene().getWindow();
        stage.close();
        MainApp.showLogin();
    }

    // ── Mini helpers ─────────────────────────────────────────────

    private ScrollPane scrollWrap(javafx.scene.Node n) {
        ScrollPane sp = new ScrollPane(n);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:transparent; -fx-background-color:transparent;");
        return sp;
    }

    private Label lbl(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#4a6a5a; -fx-padding:0 0 3 2;");
        return l;
    }

    private TextField inp(String prompt) {
        TextField t = new TextField();
        t.setPromptText(prompt);
        t.setMaxWidth(Double.MAX_VALUE);
        t.setStyle("-fx-background-color:#f8faf9; -fx-border-color:#d0e0d8; -fx-border-radius:8; " +
                "-fx-background-radius:8; -fx-padding:10 14; -fx-font-size:13px; -fx-pref-height:42px;");
        return t;
    }

    private PasswordField pin() {
        PasswordField p = new PasswordField();
        p.setMaxWidth(Double.MAX_VALUE);
        p.setStyle("-fx-background-color:#f8faf9; -fx-border-color:#d0e0d8; -fx-border-radius:8; " +
                "-fx-background-radius:8; -fx-padding:10 14; -fx-font-size:13px; -fx-pref-height:42px;");
        return p;
    }

    private Label errLabel(String msg) {
        Label l = new Label(msg);
        l.setStyle("-fx-text-fill:#c0392b; -fx-font-size:13px;");
        return l;
    }
}