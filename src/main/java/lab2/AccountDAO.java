package lab2;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import lab1.Account;
import lab1.SavingsAccount;
import lab1.WalletAccount;

public class AccountDAO implements DAO<Account> {

    @Override
    public void create(Account account) throws SQLException {
        // ✅ Cast id and customer_id to uuid so PostgreSQL accepts them
        String sql = "INSERT INTO accounts (id, customer_id, account_type, balance) " +
                "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = Connect.getConnection().prepareStatement(sql)) {
            ps.setString(1, account.getId());
            ps.setString(2, account.getcustomerId());
            ps.setString(3, account.getAccountType()); // stores "Wallet" or "Saving" exactly
            ps.setDouble(4, account.getBalance());
            ps.executeUpdate();
            System.out.println("Created " + account.getAccountType() + " account: " + account.getId());
        }
    }

    @Override
    public Account findById(String id) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE id = ?";
        try (PreparedStatement ps = Connect.getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    @Override
    public List<Account> findAll() throws SQLException {
        List<Account> list = new ArrayList<>();
        String sql = "SELECT * FROM accounts ORDER BY created_at";
        try (Statement st = Connect.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    public void update(Account account) throws SQLException {
        updateBalance(account.getId(), account.getBalance());
    }

    @Override
    public void delete(String id) throws SQLException {
        Account account = findById(id);
        if (account == null)
            throw new IllegalArgumentException("Account not found: " + id);
        if (account.getBalance() > 0)
            throw new IllegalStateException(
                    "Cannot delete account with balance of RWF " + account.getBalance());
        String sql = "DELETE FROM accounts WHERE id = ?";
        try (PreparedStatement ps = Connect.getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
            System.out.println("Account deleted: " + id);
        }
    }

    public List<Account> findByCustomerId(String customerId) throws SQLException {
        List<Account> list = new ArrayList<>();
        // Compare as text because customer_id is stored as a string UUID in the database.
        String sql = "SELECT * FROM accounts WHERE customer_id = ? ORDER BY created_at";
        try (PreparedStatement ps = Connect.getConnection().prepareStatement(sql)) {
            ps.setString(1, customerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public void updateBalance(String accountId, double newBalance) throws SQLException {
        String sql = "UPDATE accounts SET balance = ? WHERE id = ?";
        try (PreparedStatement ps = Connect.getConnection().prepareStatement(sql)) {
            ps.setDouble(1, newBalance);
            ps.setString(2, accountId);
            ps.executeUpdate();
        }
    }

    private Account mapRow(ResultSet rs) throws SQLException {
        String type   = rs.getString("account_type"); // reads exactly what is in the DB
        String id     = rs.getString("id");
        String custId = rs.getString("customer_id");
        double bal    = rs.getDouble("balance");

        Account account;
        // ✅ Case-insensitive check — handles "Wallet", "WALLET", "wallet" all correctly
        if (type != null && type.trim().equalsIgnoreCase("WALLET")) {
            account = new WalletAccount(custId);
        } else {
            account = new SavingsAccount(custId);
        }
        account.setId(id);
        account.setBalance(bal);
        return account;
    }
}