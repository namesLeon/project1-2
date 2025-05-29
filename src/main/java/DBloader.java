import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DBloader {
    // Database connection parameters
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/gtfs_Amsterdam?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "BCS1510";
    private static final String DB_PASS = "BCS1510";

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: java GTFSLoader <calendar_dates.txt> <stops.txt> <trips.txt> <stop_times.txt>");
            System.exit(1);
        }
        String calendarFile = args[0];
        String stopsFile = args[1];
        String tripsFile = args[2];
        String stopTimesFile = args[3];

        try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS)) {
            conn.setAutoCommit(false);

            loadCalendarDates(conn, calendarFile);
            loadStops(conn, stopsFile);
            loadTrips(conn, tripsFile);
            loadStopTimes(conn, stopTimesFile);

            conn.commit();
            System.out.println("Data loaded successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadCalendarDates(Connection conn, String filePath) throws SQLException, IOException {
        String sql = "INSERT INTO calendar_dates(service_id, date, exception_type) VALUES (?, ?, ?)";
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyyMMdd");
        try (BufferedReader br = new BufferedReader(new FileReader(filePath));
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String line = br.readLine(); // skip header
            int batchSize = 0;
            while ((line = br.readLine()) != null) {
                String[] cols = line.split(",");
                ps.setString(1, cols[0]);
                LocalDate ld = LocalDate.parse(cols[1], df);
                ps.setDate(2, Date.valueOf(ld));
                ps.setInt(3, Integer.parseInt(cols[2]));
                ps.addBatch();

                if (++batchSize % 1000 == 0) ps.executeBatch();
            }
            ps.executeBatch();
            System.out.println("Loaded calendar_dates from " + filePath);
        }
    }

    private static void loadStops(Connection conn, String filePath) throws SQLException, IOException {
        String sql = "INSERT INTO stops(stop_id, stop_code, stop_name, stop_lat, stop_lon, location_type, parent_station, stop_timezone, wheelchair_boarding, platform_code, zone_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (BufferedReader br = new BufferedReader(new FileReader(filePath));
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String line = br.readLine(); // skip header
            int batchSize = 0;
            while ((line = br.readLine()) != null) {
                String[] cols = line.split(",", -1);
                ps.setString(1, cols[0]);
                ps.setString(2, cols[1].isEmpty() ? null : cols[1]);
                ps.setString(3, cols[2]);
                ps.setBigDecimal(4, new java.math.BigDecimal(cols[3].trim()));
                System.out.println(cols[3]);
                ps.setBigDecimal(5, new java.math.BigDecimal(cols[4].trim()));
                ps.setInt(6, Integer.parseInt(cols[5]));
                ps.setString(7, cols[6].isEmpty() ? null : cols[6]);
                ps.setString(8, cols[7].isEmpty() ? null : cols[7]);
                ps.setInt(9, cols[8].isEmpty() ? 0 : Integer.parseInt(cols[8]));
                ps.setString(10, cols[9].isEmpty() ? null : cols[9]);
                ps.setString(11, cols[10].isEmpty() ? null : cols[10]);
                ps.addBatch();

                if (++batchSize % 1000 == 0) ps.executeBatch();
            }
            ps.executeBatch();
            System.out.println("Loaded stops from " + filePath);
        }
    }

    private static void loadTrips(Connection conn, String filePath) throws SQLException, IOException {
        String sql = "INSERT INTO trips(trip_id, route_id, service_id, realtime_trip_id, trip_headsign, trip_short_name, trip_long_name, direction_id, block_id, shape_id, wheelchair_accessible, bikes_allowed) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (BufferedReader br = new BufferedReader(new FileReader(filePath));
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String line = br.readLine(); // skip header
            int batchSize = 0;
            while ((line = br.readLine()) != null) {
                String[] cols = line.split(",", -1);
                ps.setString(1, cols[2]);  // trip_id
                ps.setString(2, cols[0]);  // route_id
                ps.setString(3, cols[1]);  // service_id
                ps.setString(4, cols[3].isEmpty() ? null : cols[3]);
                ps.setString(5, cols[4].isEmpty() ? null : cols[4]);
                ps.setString(6, cols[5].isEmpty() ? null : cols[5]);
                ps.setString(7, cols[6].isEmpty() ? null : cols[6]);
                ps.setObject(8, cols[7].isEmpty() ? null : Integer.parseInt(cols[7]), Types.TINYINT);
                ps.setString(9, cols[8].isEmpty() ? null : cols[8]);
                ps.setString(10, cols[9].isEmpty() ? null : cols[9]);
                ps.setInt(11, cols[10].isEmpty() ? 0 : Integer.parseInt(cols[10]));
                ps.setInt(12, cols[11].isEmpty() ? 0 : Integer.parseInt(cols[11]));
                ps.addBatch();

                if (++batchSize % 1000 == 0) ps.executeBatch();
            }
            ps.executeBatch();
            System.out.println("Loaded trips from " + filePath);
        }
    }

    private static void loadStopTimes(Connection conn, String filePath) throws SQLException, IOException {
        String sql = "INSERT INTO stop_times(trip_id, stop_sequence, stop_id, stop_headsign, arrival_time, departure_time, pickup_type, drop_off_type, timepoint, shape_dist_traveled, fare_units_traveled) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm:ss");
        try (BufferedReader br = new BufferedReader(new FileReader(filePath));
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String line = br.readLine(); // skip header
            int batchSize = 0;
            while ((line = br.readLine()) != null) {
                String[] cols = line.split(",", -1);
                ps.setString(1, cols[0]);
                ps.setInt(2, Integer.parseInt(cols[1]));
                ps.setString(3, cols[2]);
                ps.setString(4, cols[3].isEmpty() ? null : cols[3]);
                ps.setTime(5, Time.valueOf(LocalTime.parse(cols[4], tf)));
                ps.setTime(6, Time.valueOf(LocalTime.parse(cols[5], tf)));
                ps.setInt(7, cols[6].isEmpty() ? 0 : Integer.parseInt(cols[6]));
                ps.setInt(8, cols[7].isEmpty() ? 0 : Integer.parseInt(cols[7]));
                ps.setInt(9, cols[8].isEmpty() ? 0 : Integer.parseInt(cols[8]));
                ps.setBigDecimal(10, cols[9].isEmpty() ? null : new java.math.BigDecimal(cols[9]));
                ps.setBigDecimal(11, cols[10].isEmpty() ? null : new java.math.BigDecimal(cols[10]));
                ps.addBatch();

                if (++batchSize % 1000 == 0) ps.executeBatch();
            }
            ps.executeBatch();
            System.out.println("Loaded stop_times from " + filePath);
        }
    }
}

