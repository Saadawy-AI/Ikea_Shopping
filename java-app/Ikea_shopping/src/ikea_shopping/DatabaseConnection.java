package ikea_shopping;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String SERVER   = "MOHAMOSTAFA";
    private static final String DATABASE = "Ikea_Shopping";
    private static final String URL      = "jdbc:sqlserver://" + SERVER
                                         + ";databaseName=" + DATABASE
                                         + ";integratedSecurity=true;"
                                         + "trustServerCertificate=true;";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver not found: " + e.getMessage());
        }
        return DriverManager.getConnection(URL);
    }
}