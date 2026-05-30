package lab2;

import lab1.Loan;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LoanDAO {

    public void create(Loan loan) throws SQLException {
        String sql = "INSERT INTO loans (id, customer_id, amount, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = Connect.getConnection().prepareStatement(sql)) {
            ps.setString(1, loan.getId());
            ps.setString(2, loan.getCustomerId());
            ps.setDouble(3, loan.getAmount());
            ps.setString(4, loan.getStatus());
            ps.executeUpdate();
        }
    }

    public List<Loan> findByCustomerId(String customerId) throws SQLException {
        List<Loan> list = new ArrayList<>();
        String sql = "SELECT * FROM loans WHERE customer_id = ? ORDER BY requested_at DESC";
        try (PreparedStatement ps = Connect.getConnection().prepareStatement(sql)) {
            ps.setString(1, customerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    private Loan mapRow(ResultSet rs) throws SQLException {
        return new Loan(
                rs.getString("id"),
                rs.getString("customer_id"),
                rs.getDouble("amount"),
                rs.getString("status"),
                rs.getTimestamp("requested_at") != null
                        ? rs.getTimestamp("requested_at").toLocalDateTime() : null
        );
    }
}
