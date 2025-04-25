import java.sql.SQLException;
import java.util.Map;

import data.Stop;
import data.Route;

public class MySQLtests {
    public static void main(String[] args) {
        String host = "localhost";
        String db = "gtfs_db"; // to change db (gtfs_db = berlin) lol
        String user = "root";
        String password = "admin";

        try {
            GTFSMySQLLoader loader = new GTFSMySQLLoader(host, db, user, password);

            System.out.println("Connected to database!");

            Map<String, Stop> stops = loader.loadStops();
            System.out.println("Loaded " + stops.size() + " stops.");
            System.out.println("First 5 stops:");
            stops.values().stream().limit(5).forEach(stop ->
                System.out.println(stop.getStopId() + " - " + stop.getName() + " (" + stop.getLat() + ", " + stop.getLon() + ")")
            );

            loader.close();
            System.out.println("Connection closed.");

        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
        }
    }
}
