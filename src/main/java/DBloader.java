import com.mysql.cj.util.DnsSrv;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DBloader {
    // Database connection parameters
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/gtfs_Amsterdam?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "BCS1510";
    private static final String DB_PASS = "BCS1510";
    private static final boolean useEnvJDBCurl = false;

    public static void main(String[] args) throws IOException {
        createSQLTables();
        //  "/Users/antonilazewski/Documents/Project_1-2/NL-20250425.gtfs.zip"
        // "/Users/antonilazewski/Documents/Project_1-2/amsterdam_gtfs.zip"
        Path zipPath = Path.of("/Users/antonilazewski/Documents/Project_1-2/NL-20250425.gtfs.zip");
        loadFromZip(zipPath);
    }

    private static void loadCalendarDates(Connection conn, Path filePath) throws SQLException, IOException {
        String sql = "INSERT INTO calendar_dates(service_id, date, exception_type) VALUES (?, ?, ?)";
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyyMMdd");
        try (BufferedReader reader = Files.newBufferedReader(filePath);
             PreparedStatement ps = conn.prepareStatement(sql);
             CSVParser parser = CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withTrim()
                     .parse(reader)) {
            int batchSize = 0;
            for (CSVRecord rec : parser) {
                String id = rec.get("service_id");
                LocalDate date = LocalDate.parse(rec.get("date"), df);
                int exceptionType = Integer.parseInt(rec.get("exception_type"));
                ps.setString(1, id);;
                ps.setDate(2, Date.valueOf(date));
                ps.setInt(3, exceptionType);
                ps.addBatch();

                if (++batchSize % 1000 == 0) ps.executeBatch();
            }
            ps.executeBatch();
            System.out.println("Loaded calendar_dates from " + filePath);
        }
    }

    private static void loadCalendar(Connection conn, Path filePath) throws IOException, SQLException {
        String sql = "INSERT INTO calendar(service_id, monday, tuesday, wednesday, thursday, friday, saturday, sunday) " +
                "VALUES(?,?,?,?,?,?,?,?)";
        try (BufferedReader reader = Files.newBufferedReader(filePath);
             PreparedStatement ps = conn.prepareStatement(sql);
             CSVParser parser = CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withTrim()
                     .parse(reader)) {
            int batchSize = 0;
            for (CSVRecord rec : parser) {
                ps.setString(1, rec.get("service_id"));
                ps.setString(2, rec.get("monday"));
                ps.setString(3, rec.get("tuesday"));
                ps.setString(4, rec.get("wednesday"));
                ps.setString(5, rec.get("thursday"));
                ps.setString(6, rec.get("friday"));
                ps.setString(7, rec.get("saturday"));
                ps.setString(8, rec.get("sunday"));
                ps.addBatch();
                if (++batchSize % 1000 == 0) ps.executeBatch();
            }
            ps.executeBatch();
            System.out.println("Loaded calendar from " + filePath);
        }
    }

    private static void loadStops(Connection conn, Path filePath) throws SQLException, IOException {
        String sql = "INSERT INTO stops(stop_id, stop_name, stop_lat, stop_lon) " +
                "VALUES (?, ?, ?, ?)";

        try (BufferedReader reader = Files.newBufferedReader(filePath);
             PreparedStatement ps = conn.prepareStatement(sql);
             CSVParser parser = CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withTrim()
                     .parse(reader)) {
            int batchSize = 0;
            for (CSVRecord rec : parser) {
                String id = rec.get("stop_id");
                String name = rec.get("stop_name");
                double lat = Double.parseDouble(rec.get("stop_lat"));
                double lon = Double.parseDouble(rec.get("stop_lon"));
                ps.setString(1, id);
                ps.setString(2, name);
                ps.setBigDecimal(3, new BigDecimal(lat));
                ps.setBigDecimal(4, new BigDecimal(lon));
                ps.addBatch();
                if (++batchSize % 1000 == 0) ps.executeBatch();
            }
            ps.executeBatch();
            System.out.println("Loaded stops from " + filePath);
        }
    }

    private static void loadTrips(Connection conn, Path filePath) throws SQLException, IOException {
        String sql = "INSERT INTO trips(trip_id, route_id, trip_headsign, service_id) " +
                "VALUES (?, ?, ?, ?)";

        try (BufferedReader reader = Files.newBufferedReader(filePath);
             PreparedStatement ps = conn.prepareStatement(sql);
             CSVParser parser = CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withTrim()
                     .parse(reader)) {
            int batchSize = 0;
            for (CSVRecord rec : parser) {
                String trip_id = rec.get("trip_id");
                String route_id = rec.get("route_id");
                String service_id = rec.get("service_id");
                String trip_headsign = rec.get("trip_headsign");
                ps.setString(1, trip_id);
                ps.setString(2, route_id);
                ps.setString(3, trip_headsign);
                ps.setString(4, service_id);
                ps.addBatch();
                if (++batchSize % 1000 == 0) ps.executeBatch();
            }
            ps.executeBatch();
            System.out.println("Loaded trips from " + filePath);
        }
        catch (SQLException e){
            System.out.println( e.getMessage());
        }
    }

    private static void loadStopTimes(Connection conn, Path filePath) throws SQLException, IOException {
        String sql = "INSERT INTO stop_times(trip_id, stop_sequence, stop_id, arrival_time, departure_time) " +
                "VALUES (?, ?, ?, ?, ?)";
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm:ss");
        try (BufferedReader reader = Files.newBufferedReader(filePath);
             PreparedStatement ps = conn.prepareStatement(sql);
             CSVParser parser = CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withTrim()
                     .parse(reader)) {
            int batchSize = 0;
            for (CSVRecord rec : parser) {
                String trip_id = rec.get("trip_id");
                int stop_sequence = Integer.parseInt(rec.get("stop_sequence"));
                String stop_id = rec.get("stop_id");
                String[] arrivalTimeStringArr = rec.get("arrival_time").split(":");
                String[] departureTimeStringArr = rec.get("departure_time").split(":");

                ps.setString(1, trip_id);
                ps.setInt(2, stop_sequence);
                ps.setString(3, stop_id);
                ps.setTime(4, Time.valueOf(joinTimeFromArray(arrivalTimeStringArr)));
                // ps.setTime(4, Time.valueOf(arrivalTime));
                //ps.setTime(5, Time.valueOf(departureTime));
                ps.setTime(5, Time.valueOf(joinTimeFromArray(departureTimeStringArr)));
                ps.addBatch();
                if (++batchSize % 1000 == 0) ps.executeBatch();
            }
            ps.executeBatch();

            System.out.println("Loaded stop_times from " + filePath);
        }
    }

    private static void loadAgencies(Connection conn, Path filePath) throws IOException, SQLException{
        String sql = "INSERT INTO agency(agency_id, agency_name, agency_url) " +
                "VALUES (?, ?, ?)";
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm:ss");
        try (BufferedReader reader = Files.newBufferedReader(filePath);
             PreparedStatement ps = conn.prepareStatement(sql);
             CSVParser parser = CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withTrim()
                     .parse(reader)) {
            int batchSize = 0;
            for (CSVRecord rec : parser) {
                String agency_id = rec.get("agency_id");
                String agency_name = rec.get("agency_name");
                String agency_url = rec.get("agency_url");

                ps.setString(1, agency_id);
                ps.setString(2, agency_name);
                ps.setString(3, agency_url);
                ps.addBatch();
                if (++batchSize % 1000 == 0) ps.executeBatch();
            }
            ps.executeBatch();
            System.out.println("Loaded agencies from " + filePath);
        }
    }

    private static void loadRoutes(Connection conn, Path filePath) throws IOException, SQLException {
        String sql = "INSERT INTO routes(route_id, agency_id, route_short_name, route_long_name) " +
                "VALUES (?, ?, ?, ?)";
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm:ss");
        try (BufferedReader reader = Files.newBufferedReader(filePath);
             PreparedStatement ps = conn.prepareStatement(sql);
             CSVParser parser = CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withTrim()
                     .parse(reader)) {
            int batchSize = 0;
            for (CSVRecord rec : parser) {
                String route_id = rec.get("route_id");
                String agency_id = rec.get("agency_id");
                String route_short_name = rec.get("route_short_name");
                String route_long_name = rec.get("route_long_name");

                ps.setString(1, route_id);
                ps.setString(2, agency_id);
                ps.setString(3, route_short_name);
                ps.setString(4, route_long_name);
                ps.addBatch();
                if (++batchSize % 1000 == 0) ps.executeBatch();
            }
            ps.executeBatch();
            System.out.println("Loaded routes from " + filePath);
        }
    }

    private static void createEdgesTable(Connection conn) throws SQLException{
        Statement st = conn.createStatement();
        String dropTableQuery = "DROP TABLE IF EXISTS edges";
        String createTableQuery = """
                CREATE TABLE edges AS(
                    SELECT st1.stop_id as start_stop, st2.stop_id as end_stop, trip_id,
                    st1.departure_time as departure_time, st2.arrival_time as arrival_time,
                    TIMEDIFF(st2.arrival_time, st1.departure_time) AS transfer_length, st1.stop_sequence as edge_sequence
                    FROM stop_times st1 JOIN stop_times st2
                    USING (trip_id)
                    WHERE st1.stop_sequence = st2.stop_sequence - 1)
                """;
        String createIndexQuery = "CREATE INDEX edges_trip_id_ix ON edges(trip_id)";

        st.executeUpdate(dropTableQuery);
        st.executeUpdate(createTableQuery);
        st.executeUpdate(createIndexQuery);
    }


    public static void loadFromZip(Path zipPath) throws IOException {
        //clear previous data
        Path tmpDir = Files.createTempDirectory("gtfs_temp");
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null){
                if (!entry.isDirectory()) {
                    String fileName = Paths.get(entry.getName()).getFileName().toString();
                    Path out = tmpDir.resolve(fileName);
                    Files.copy(zis, out, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
        // Add this after your ZIP extraction
        System.out.println("Contents of temp directory:");
        try (Stream<Path> files = Files.walk(tmpDir)) {
            files.forEach(System.out::println);
        }
        System.out.println("Loaded gtfs zip");
        try (Connection conn = DriverManager.getConnection(useEnvJDBCurl ? System.getenv("ROUTING_ENGINE_MYSQL_JDBC") : JDBC_URL, DB_USER, DB_PASS)) {
            conn.setAutoCommit(false);

            loadCalendarDates(conn, tmpDir.resolve("calendar_dates.txt"));
            conn.commit();
            loadStops(conn, tmpDir.resolve("stops.txt"));
            conn.commit();
            loadTrips(conn, tmpDir.resolve("trips.txt"));
            conn.commit();
            loadStopTimes(conn, tmpDir.resolve("stop_times.txt"));
            conn.commit();
            loadAgencies(conn, tmpDir.resolve("agency.txt"));
            conn.commit();
            loadRoutes(conn, tmpDir.resolve("routes.txt"));
            conn.commit();
            if(Files.exists(tmpDir.resolve("calendar.txt")))  loadCalendar(conn, tmpDir.resolve("calendar.txt"));
            conn.commit();
            //createEdgesTable(conn);
            //conn.commit();
            System.out.println("Data loaded successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean manageTimeOverflow(String[] time){
        int hourValueInt = Integer.parseInt(time[0]);
//        if(hourValueInt >= 24){
//            hourValueInt -= 24;
//        }
        if(hourValueInt < 10) time[0] = "0"+String.valueOf(hourValueInt);
        else{
            time[0] = String.valueOf(hourValueInt);
            return false;
        }
        return true;
    }

    private static String joinTimeFromArray(String[] time){
        return time[0]+":"+time[1]+":"+time[2];
    }

    private static void createSQLTables(){
        try (Connection conn = DriverManager.getConnection(useEnvJDBCurl ? System.getenv("ROUTING_ENGINE_MYSQL_JDBC") : JDBC_URL, DB_USER, DB_PASS)) {

            String sqlContent = Files.readString(Paths.get("amsterdam_database/schema.sql"));

            // Split statements by semicolon
            String[] statements = sqlContent.split(";");

            try (Statement st = conn.createStatement()) {
                for (String sql : statements) {
                    sql = sql.trim();
                    if (!sql.isEmpty() /*&& !sql.startsWith("--")*/) {
                        //System.out.println("To execute : "+sql);
                        st.executeUpdate(sql);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

