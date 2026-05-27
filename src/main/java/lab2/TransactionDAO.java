package lab2;

import lab1.Account;
import lab1.Transaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO implements DAO<Transaction> {

    private final AccountDAO accountDAO             = new AccountDAO();
    private final ProcessedRequestDAO processedDAO  = new ProcessedRequestDAO();

    @Override
    public void create(Transaction t) throws SQLException {
        String sql = "INSERT INTO transactions (id, account_id, reference_id, transaction_type, amount) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = Connect.getConnection().prepareStatement(sql)) {
            ps.setString(1, t.getId());
            ps.setString(2, t.getReferenceId());
            ps.setString(3, t.getReferenceId());
            ps.setString(4, t.getType());
            ps.setDouble(5, t.getAmount());
            ps.executeUpdate();
        }
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
        throw new UnsupportedOperationException("Transactions cannot be updated.");
    }

    @Override
    public void delete(String id) throws SQLException {
        throw new UnsupportedOperationException("Transactions cannot be deleted.");
    }

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

    // ── Idempotency: every deposit checks the reference ID before processing ──
    public void deposit(String accountId, double amount, String referenceId) throws SQLException {
        if (processedDAO.isAlreadyProcessed(referenceId)) {
            System.out.println("Duplicate request rejected. Reference: " + referenceId);
            return;
        }

        Connection conn = Connect.getConnection();
        conn.setAutoCommit(false);
        try {
            Account account = accountDAO.findById(accountId);
            if (account == null) throw new IllegalArgumentException("Account not found.");

            account.deposit(amount, referenceId);
            accountDAO.updateBalance(accountId, account.getBalance());

            Transaction t = new Transaction(referenceId, amount, "Deposit");
            saveTransaction(accountId, t);

            // Store the reference ID so this request can never be processed again
            processedDAO.create(referenceId);

            conn.commit();
            System.out.println("Deposit successful. New balance: " + account.getBalance());
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ── Idempotency: every withdrawal checks the reference ID before processing ──
    public void withdraw(String accountId, double amount, String referenceId) throws SQLException {
        if (processedDAO.isAlreadyProcessed(referenceId)) {
            System.out.println("Duplicate request rejected. Reference: " + referenceId);
            return;
        }

        Connection conn = Connect.getConnection();
        conn.setAutoCommit(false);
        try {
            Account account = accountDAO.findById(accountId);
            if (account == null) throw new IllegalArgumentException("Account not found.");
            if (account.getBalance() < amount) throw new IllegalStateException("Insufficient balance.");

            account.withdraw(amount, referenceId);
            accountDAO.updateBalance(accountId, account.getBalance());

            Transaction t = new Transaction(referenceId, amount, "Withdraw");
            saveTransaction(accountId, t);

            // Store the reference ID so this request can never be processed again
            processedDAO.create(referenceId);

            conn.commit();
            System.out.println("Withdrawal successful. New balance: " + account.getBalance());
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ── Idempotency: every transfer checks the reference ID before processing ──
    public void transfer(String fromAccountId, String toAccountId, double amount, String referenceId) throws SQLException {
        if (processedDAO.isAlreadyProcessed(referenceId)) {
            System.out.println("Duplicate request rejected. Reference: " + referenceId);
            return;
        }

        Connection conn = Connect.getConnection();
        conn.setAutoCommit(false);
        try {
            Account from = accountDAO.findById(fromAccountId);
            Account to   = accountDAO.findById(toAccountId);

            if (from == null) throw new IllegalArgumentException("Sender account not found.");
            if (to == null)   throw new IllegalArgumentException("Receiver account not found.");
            if (from.getBalance() < amount) throw new IllegalStateException("Insufficient balance.");

            from.withdraw(amount, referenceId);
            to.deposit(amount, referenceId);

            accountDAO.updateBalance(fromAccountId, from.getBalance());
            accountDAO.updateBalance(toAccountId,   to.getBalance());

            saveTransaction(fromAccountId, new Transaction(referenceId + "_OUT", amount, "Withdraw"));
            saveTransaction(toAccountId,   new Transaction(referenceId + "_IN",  amount, "Deposit"));

            // Store the reference ID so this request can never be processed again
            processedDAO.create(referenceId);

            conn.commit();
            System.out.println("Transfer successful. RWF " + amount + " sent.");
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private void saveTransaction(String accountId, Transaction t) throws SQLException {
        String sql = "INSERT INTO transactions (id, account_id, reference_id, transaction_type, amount) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = Connect.getConnection().prepareStatement(sql)) {
            ps.setString(1, t.getId());
            ps.setString(2, accountId);
            ps.setString(3, t.getReferenceId());
            ps.setString(4, t.getType());
            ps.setDouble(5, t.getAmount());
            ps.executeUpdate();
        }
    }

    private Transaction mapRow(ResultSet rs) throws SQLException {
        return new Transaction(
                rs.getString("reference_id"),
                rs.getDouble("amount"),
                rs.getString("transaction_type")
        );
    }
}