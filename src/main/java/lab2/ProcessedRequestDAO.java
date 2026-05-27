package lab2;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProcessedRequestDAO implements DAO<String> {

    // Store a reference ID after a transaction is successfully processed
    @Override
    public void create(String referenceId) throws SQLException {
        String sql = "INSERT INTO processed_requests (id, reference_id) VALUES (gen_random_uuid(), ?)";
        try (PreparedStatement ps = Connect.getConnection().prepareStatement(sql)) {
            ps.setString(1, referenceId);
            ps.executeUpdate();
        }
    }

    @Override
    public String findById(String id) throws SQLException {
        String sql = "SELECT reference_id FROM processed_requests WHERE id = ?";
        try (PreparedStatement ps = Connect.getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("reference_id");
        }
        return null;
    }

    @Override
    public List<String> findAll() throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = "SELECT reference_id FROM processed_requests ORDER BY processed_at DESC";
        try (Statement st = Connect.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(rs.getString("reference_id"));
        }
        return list;
    }

    @Override
    public void update(String referenceId) throws SQLException {
        throw new UnsupportedOperationException("Processed requests cannot be updated.");
    }

    @Override
    public void delete(String id) throws SQLException {
        throw new UnsupportedOperationException("Processed requests cannot be deleted.");
    }

    // Check if a reference ID was already used — called before every transaction
    public boolean isAlreadyProcessed(String referenceId) throws SQLException {
        String sql = "SELECT 1 FROM processed_requests WHERE reference_id = ?";
        try (PreparedStatement ps = Connect.getConnection().prepareStatement(sql)) {
            ps.setString(1, referenceId);
            return ps.executeQuery().next();
        }
    }
}