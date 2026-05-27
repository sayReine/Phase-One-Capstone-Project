package lab2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Connect {

    private static final String URL      = "jdbc:postgresql://localhost:5432/igirepay";
    private static final String USER     = "postgres";
    private static final String PASSWORD = "admin";

    public static Connection connection;

    public static Connection getConnection() throws SQLException {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
//            System.out.println("database connection successfull");
        } catch (SQLException e) {
            throw new SQLException("postgres driver not found:", e);
        }

        return connection;
    }
}
