import java.sql.*;
import java.util.*;
import data.Stop;

public class GTFSMySQLLoader {
    private Connection connection;

    public GTFSMySQLLoader(String host, String database, String user, String password) throws SQLException {
        String url = "jdbc:mysql://" + host + "/" + database + "?useSSL=false&serverTimezone=UTC";
        this.connection = DriverManager.getConnection(url, user, password);
    }

    public Map<String, Stop> loadStops() throws SQLException {
        Map<String, Stop> stops = new HashMap<>();
        String query = "SELECT stop_id, stop_name, stop_lat, stop_lon FROM stops";

        try (PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String stopId = rs.getString("stop_id");
                String stopName = rs.getString("stop_name");
                double lat = rs.getDouble("stop_lat");
                double lon = rs.getDouble("stop_lon");

                //Create a Stop object
                Stop stop = new Stop(stopId, stopName, lat, lon);

                //Add it into the HashMap
                stops.put(stopId, stop);
            }
        }
        return stops;
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
