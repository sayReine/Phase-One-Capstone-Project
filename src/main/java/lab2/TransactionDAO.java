package lab2;

import lab1.Account;
import lab1.Transaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO implements DAO<Transaction> {

    private final AccountDAO          accountDAO   = new AccountDAO();
    private final ProcessedRequestDAO processedDAO = new ProcessedRequestDAO();

    @Override
    public void create(Transaction t) throws SQLException {
        saveTransaction(t.getAccountId(), t);
    }

    @Override
    public Transaction findById(String id) throws SQLException {
        String sql = "SELECT * FROM transactions WHERE id = ?";
        try (PreparedStatement ps = Connect.getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    @Override
    public List<Transaction> findAll() throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions ORDER BY created_at DESC";
        try (Statement st = Connect.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    public void update(Transaction t) throws SQLException {
        throw new UnsupportedOperationException("Transactions are immutable.");
    }

    @Override
    public void delete(String id) throws SQLException {
        throw new UnsupportedOperationException("Transactions cannot be deleted.");
    }

    // ✅ Fix: query uses account_id column correctly with uuid cast
    public List<Transaction> findByAccountId(String accountId) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE account_id = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = Connect.getConnection().prepareStatement(sql)) {
            ps.setString(1, accountId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ── DEPOSIT ──────────────────────────────────────────────────

    public void deposit(String accountId, double amount, String referenceId) throws SQLException {

        // Exception: invalid amount
        if (amount <= 0)
            throw new IllegalArgumentException("Deposit amount must be greater than zero.");

        // Exception: invalid account ID
        if (accountId == null || accountId.isBlank())
            throw new IllegalArgumentException("Account ID is required.");

        // Idempotency check: duplicate referenceId → reject, balance stays the same
        if (processedDAO.isAlreadyProcessed(referenceId)) {
            throw new IllegalStateException(
                    "Duplicate request rejected. Reference ID '" + referenceId +
                            "' was already used. Your balance has NOT changed."
            );
        }

        Connection conn = Connect.getConnection();
        conn.setAutoCommit(false);
        try {
            Account account = accountDAO.findById(accountId);
            if (account == null)
                throw new IllegalArgumentException("No account found with ID: " + accountId);

            double newBalance = account.getBalance() + amount;
            accountDAO.updateBalance(accountId, newBalance);

            Transaction t = new Transaction(accountId, referenceId, amount, "Deposit");
            saveTransaction(accountId, t);

            // Mark refId as processed — future retries with same ID will be rejected
            processedDAO.create(referenceId);

            conn.commit();
            System.out.println("Deposit OK. New balance: RWF " + newBalance);

        } catch (Exception e) {
            conn.rollback(); // undo everything — balance stays exactly as it was
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ── WITHDRAW ─────────────────────────────────────────────────
    // Exception handling: invalid amount, insufficient balance, duplicate refId, invalid account
    public void withdraw(String accountId, double amount, String referenceId) throws SQLException {

        if (amount <= 0)
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero.");

        if (accountId == null || accountId.isBlank())
            throw new IllegalArgumentException("Account ID is required.");

        // Idempotency check
        if (processedDAO.isAlreadyProcessed(referenceId)) {
            throw new IllegalStateException(
                    "Duplicate request rejected. Reference ID '" + referenceId +
                            "' was already used. Your balance has NOT changed."
            );
        }

        Connection conn = Connect.getConnection();
        conn.setAutoCommit(false);
        try {
            Account account = accountDAO.findById(accountId);
            if (account == null)
                throw new IllegalArgumentException("No account found with ID: " + accountId);

            // Exception: insufficient balance
            if (account.getBalance() < amount)
                throw new IllegalStateException(
                        "Insufficient balance. Available: RWF " +
                                String.format("%,.2f", account.getBalance()) +
                                " — Requested: RWF " + String.format("%,.2f", amount)
                );

            boolean success = account.withdraw(amount, referenceId);
            if (!success)
                throw new IllegalStateException(
                        "Withdrawal failed. Check balance or withdrawal limit.");

            accountDAO.updateBalance(accountId, account.getBalance());

            Transaction t = new Transaction(accountId, referenceId, amount, "Withdraw");
            saveTransaction(accountId, t);

            processedDAO.create(referenceId);

            conn.commit();
            System.out.println("Withdraw OK. New balance: RWF " + account.getBalance());

        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ── TRANSFER ─────────────────────────────────────────────────
    // Exception handling: same account transfer, invalid accounts, insufficient balance, duplicate refId
    public void transfer(String fromId, String toId, double amount, String referenceId)
            throws SQLException {

        if (amount <= 0)
            throw new IllegalArgumentException("Transfer amount must be greater than zero.");

        if (fromId == null || fromId.isBlank())
            throw new IllegalArgumentException("Source account ID is required.");

        if (toId == null || toId.isBlank())
            throw new IllegalArgumentException("Destination account ID is required.");

        if (fromId.equals(toId))
            throw new IllegalArgumentException("Cannot transfer to the same account.");

        // Idempotency check
        if (processedDAO.isAlreadyProcessed(referenceId)) {
            throw new IllegalStateException(
                    "Duplicate request rejected. Reference ID '" + referenceId +
                            "' was already used. Your balance has NOT changed."
            );
        }

        Connection conn = Connect.getConnection();
        conn.setAutoCommit(false);
        try {
            Account from = accountDAO.findById(fromId);
            Account to   = accountDAO.findById(toId);

            if (from == null)
                throw new IllegalArgumentException("Sender account not found: " + fromId);
            if (to == null)
                throw new IllegalArgumentException("Receiver account not found: " + toId);

            if (from.getBalance() < amount)
                throw new IllegalStateException(
                        "Insufficient balance. Available: RWF " +
                                String.format("%,.2f", from.getBalance())
                );

            boolean debit = from.withdraw(amount, referenceId);
            if (!debit)
                throw new IllegalStateException(
                        "Debit failed. Check withdrawal limit for this account type.");

            to.deposit(amount, referenceId);

            accountDAO.updateBalance(fromId, from.getBalance());
            accountDAO.updateBalance(toId,   to.getBalance());

            saveTransaction(fromId, new Transaction(fromId, referenceId + "_OUT", amount, "Transfer_Out"));
            saveTransaction(toId,   new Transaction(toId,   referenceId + "_IN",  amount, "Transfer_In"));

            processedDAO.create(referenceId);

            conn.commit();
            System.out.println("Transfer OK. RWF " + amount + " sent.");

        } catch (Exception e) {
            conn.rollback(); // both accounts stay unchanged if anything fails
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ── Private helpers ──────────────────────────────────────────

    private void saveTransaction(String accountId, Transaction t) throws SQLException {
        String sql = "INSERT INTO transactions (id, account_id, reference_id, transaction_type, amount) " +
                "VALUES (?  , ?, ?, ?, ?)";
        try (PreparedStatement ps = Connect.getConnection().prepareStatement(sql)) {
            ps.setString(1, t.getId());
            ps.setString(2, accountId);
            ps.setString(3, t.getReferenceId());
            ps.setString(4, t.getType());
            ps.setDouble(5, t.getAmount());
            ps.executeUpdate();
        }
    }

    // ✅ Fix: mapRow now builds full Transaction with all fields including timestamp
    private Transaction mapRow(ResultSet rs) throws SQLException {
        return new Transaction(
                rs.getString("id"),
                rs.getString("account_id"),
                rs.getString("reference_id"),
                rs.getDouble("amount"),
                rs.getString("transaction_type"),
                rs.getTimestamp("created_at") != null
                        ? rs.getTimestamp("created_at").toLocalDateTime()
                        : null
        );
    }
}