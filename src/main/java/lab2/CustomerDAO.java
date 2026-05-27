package lab2;

import lab1.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO implements DAO<Customer> {

    @Override
    public void create(Customer customer) throws SQLException {
        String sql = "INSERT INTO customers (id, full_name, email, phone_number, pin_hash) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = Connect.getConnection().prepareStatement(sql)) {
            ps.setString(1, customer.getId());
            ps.setString(2, customer.getFullName());
            ps.setString(3, customer.getEmail());
            ps.setString(4, customer.getPhone());
            ps.setString(5, customer.getPinHash());
            ps.executeUpdate();
            System.out.println("Customer created: " + customer.getFullName());
        }
    }

    @Override
    public Customer findById(String id) throws SQLException {
        String sql = "SELECT * FROM customers WHERE id = ?";
        try (PreparedStatement ps = Connect.getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    @Override
    public List<Customer> findAll() throws SQLException {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customers ORDER BY full_name";
        try (Statement st = Connect.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    public void update(Customer customer) throws SQLException {
        String sql = "UPDATE customers SET full_name = ?, email = ?, phone_number = ?, pin_hash = ?, failed_login_attempts = ?, locked = ? WHERE id = ?";
        try (PreparedStatement ps = Connect.getConnection().prepareStatement(sql)) {
            ps.setString(1, customer.getFullName());
            ps.setString(2, customer.getEmail());
            ps.setString(3, customer.getPhone());
            ps.setString(4, customer.getPinHash());
            ps.setInt(5, customer.getFailedLoginAttempts());
            ps.setBoolean(6, customer.isLocked());
            ps.setString(7, customer.getId());
            ps.executeUpdate();
            System.out.println("Customer updated: " + customer.getFullName());
        }
    }

    @Override
    public void delete(String id) throws SQLException {
        String balanceCheck = "SELECT SUM(balance) AS total FROM accounts WHERE customer_id = ?";
        try (PreparedStatement ps = Connect.getConnection().prepareStatement(balanceCheck)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getDouble("total") > 0) {
                throw new IllegalStateException("Cannot delete customer with remaining balance.");
            }
        }
        String sql = "DELETE FROM customers WHERE id = ?";
        try (PreparedStatement ps = Connect.getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
            System.out.println("Customer deleted: " + id);
        }
    }

    public Customer findByPhone(String phone) throws SQLException {
        String sql = "SELECT * FROM customers WHERE phone_number = ?";
        try (PreparedStatement ps = Connect.getConnection().prepareStatement(sql)) {
            ps.setString(1, phone);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public boolean phoneExists(String phone) throws SQLException {
        String sql = "SELECT 1 FROM customers WHERE phone_number = ?";
        try (PreparedStatement ps = Connect.getConnection().prepareStatement(sql)) {
            ps.setString(1, phone);
            return ps.executeQuery().next();
        }
    }

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT 1 FROM customers WHERE email = ?";
        try (PreparedStatement ps = Connect.getConnection().prepareStatement(sql)) {
            ps.setString(1, email);
            return ps.executeQuery().next();
        }
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        Customer customer = new Customer(
                rs.getString("id"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("phone_number"),
                rs.getString("pin_hash"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getInt("failed_login_attempts"),
                rs.getBoolean("locked")
        );
        return customer;
    }
}