import java.sql.*;
import java.util.*;

public class MySQLGTFSLoader {

    private final Connection conn;

    public MySQLGTFSLoader(String host, String port, String dbName, String user, String password) throws SQLException {
        String url = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", host, port, dbName);
        this.conn = DriverManager.getConnection(url, user, password);
    }

    public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) conn.close();
    }

    // Methods like loadStops(), loadTrips(), etc. below...
}