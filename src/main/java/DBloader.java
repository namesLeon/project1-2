import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DBloader {
    // Database connection parameters
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/gtfs_Amsterdam?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "BCS1510";
    private static final String DB_PASS = "BCS1510";
    private static final boolean useEnvJDBCurl = false;

    public static void main(String[] args) throws IOException {
//        if (args.length < 0) {
//            System.err.println("Usage: gtfsDir.zip");
//            System.exit(1);
//        }
//        Path zipPath = Path.of(args[0]);
        Path zipPath = Path.of("/Users/antonilazewski/Documents/Project_1-2/NL-20250425.gtfs");
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

//        try (BufferedReader br = new BufferedReader(new FileReader(filePath));
//             PreparedStatement ps = conn.prepareStatement(sql)) {
//            String line = br.readLine(); // skip header
//            int batchSize = 0;
//            while ((line = br.readLine()) != null) {
//                String[] cols = line.split(",");
//                ps.setString(1, cols[0]);
//                LocalDate ld = LocalDate.parse(cols[1], df);
//                ps.setDate(2, Date.valueOf(ld));
//                ps.setInt(3, Integer.parseInt(cols[2]));
//                ps.addBatch();
//
//                if (++batchSize % 1000 == 0) ps.executeBatch();
//            }
//            ps.executeBatch();
//            System.out.println("Loaded calendar_dates from " + filePath);
//        }
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
//        try (BufferedReader br = new BufferedReader(new FileReader(filePath));
//             PreparedStatement ps = conn.prepareStatement(sql)) {
//            String line = br.readLine(); // skip header
//            int batchSize = 0;
//            while ((line = br.readLine()) != null) {
//                String[] cols = line.split(",", -1);
//                ps.setString(1, cols[0]);
//                ps.setString(2, cols[1].isEmpty() ? null : cols[1]);
//                ps.setString(3, cols[2]);
//                ps.setBigDecimal(4, new java.math.BigDecimal(cols[3].trim()));
//                System.out.println(cols[3]);
//                ps.setBigDecimal(5, new java.math.BigDecimal(cols[4].trim()));
//                ps.setInt(6, Integer.parseInt(cols[5]));
//                ps.setString(7, cols[6].isEmpty() ? null : cols[6]);
//                ps.setString(8, cols[7].isEmpty() ? null : cols[7]);
//                ps.setInt(9, cols[8].isEmpty() ? 0 : Integer.parseInt(cols[8]));
//                ps.setString(10, cols[9].isEmpty() ? null : cols[9]);
//                ps.setString(11, cols[10].isEmpty() ? null : cols[10]);
//                ps.addBatch();
//
//                if (++batchSize % 1000 == 0) ps.executeBatch();
//            }
//            ps.executeBatch();
//            System.out.println("Loaded stops from " + filePath);
//        }
    }

    private static void loadTrips(Connection conn, Path filePath) throws SQLException, IOException {
        String sql = "INSERT INTO trips(trip_id, route_id, trip_headsign, service_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (BufferedReader reader = Files.newBufferedReader(filePath);
             PreparedStatement ps = conn.prepareStatement(sql);
             CSVParser parser = CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withTrim()
                     .parse(reader)) {
            int batchSize = 0;
            for (CSVRecord rec : parser) {
                String trip_id = rec.get("trip_id");
                String route_id = rec.get("rote_id");
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

//        try (BufferedReader br = new BufferedReader(new FileReader(filePath));
//             PreparedStatement ps = conn.prepareStatement(sql)) {
//            String line = br.readLine(); // skip header
//            int batchSize = 0;
//            while ((line = br.readLine()) != null) {
//                String[] cols = line.split(",", -1);
//                ps.setString(1, cols[2]);  // trip_id
//                ps.setString(2, cols[0]);  // route_id
//                ps.setString(3, cols[1]);  // service_id
//                ps.setString(4, cols[3].isEmpty() ? null : cols[3]);
//                ps.setString(5, cols[4].isEmpty() ? null : cols[4]);
//                ps.setString(6, cols[5].isEmpty() ? null : cols[5]);
//                ps.setString(7, cols[6].isEmpty() ? null : cols[6]);
//                ps.setObject(8, cols[7].isEmpty() ? null : Integer.parseInt(cols[7]), Types.TINYINT);
//                ps.setString(9, cols[8].isEmpty() ? null : cols[8]);
//                ps.setString(10, cols[9].isEmpty() ? null : cols[9]);
//                ps.setInt(11, cols[10].isEmpty() ? 0 : Integer.parseInt(cols[10]));
//                ps.setInt(12, cols[11].isEmpty() ? 0 : Integer.parseInt(cols[11]));
//                ps.addBatch();
//
//                if (++batchSize % 1000 == 0) ps.executeBatch();
//            }
//            ps.executeBatch();
//            System.out.println("Loaded trips from " + filePath);
//        }
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
                LocalTime arrivalTime = LocalTime.parse(rec.get("arrival_time"), tf);
                LocalTime departureTime = LocalTime.parse(rec.get("departure_time"), tf);

                ps.setString(1, trip_id);
                ps.setInt(2, stop_sequence);
                ps.setString(3, stop_id);
                ps.setTime(4, Time.valueOf(arrivalTime));
                ps.setTime(5, Time.valueOf(departureTime));
                ps.addBatch();
                if (++batchSize % 1000 == 0) ps.executeBatch();
            }
            ps.executeBatch();
            System.out.println("Loaded stop_times from " + filePath);
        }

//        try (BufferedReader br = new BufferedReader(new FileReader(filePath));
//             PreparedStatement ps = conn.prepareStatement(sql)) {
//            String line = br.readLine(); // skip header
//            int batchSize = 0;
//            while ((line = br.readLine()) != null) {
//                String[] cols = line.split(",", -1);
//                ps.setString(1, cols[0]);
//                ps.setInt(2, Integer.parseInt(cols[1]));
//                ps.setString(3, cols[2]);
//                ps.setString(4, cols[3].isEmpty() ? null : cols[3]);
//                ps.setTime(5, Time.valueOf(LocalTime.parse(cols[4], tf)));
//                ps.setTime(6, Time.valueOf(LocalTime.parse(cols[5], tf)));
//                ps.setInt(7, cols[6].isEmpty() ? 0 : Integer.parseInt(cols[6]));
//                ps.setInt(8, cols[7].isEmpty() ? 0 : Integer.parseInt(cols[7]));
//                ps.setInt(9, cols[8].isEmpty() ? 0 : Integer.parseInt(cols[8]));
//                ps.setBigDecimal(10, cols[9].isEmpty() ? null : new java.math.BigDecimal(cols[9]));
//                ps.setBigDecimal(11, cols[10].isEmpty() ? null : new java.math.BigDecimal(cols[10]));
//                ps.addBatch();
//
//                if (++batchSize % 1000 == 0) ps.executeBatch();
//            }
//            ps.executeBatch();
//            System.out.println("Loaded stop_times from " + filePath);
//        }
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
        String sql = "INSERT INTO routes(route_id, agency_id, agency_url) " +
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
            System.out.println("Loaded routes from " + filePath);
        }
    }


    public static void loadFromZip(Path zipPath) throws IOException {
        //clear previous data
        Path tmpDir = Files.createTempDirectory("gtfs_temp");
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path out = tmpDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    Files.copy(zis, out);
                }
                zis.closeEntry();
            }
        }
        try (Connection conn = DriverManager.getConnection(useEnvJDBCurl ? System.getenv("ROUTING_ENGINE_MYSQL_JDBC") : JDBC_URL, DB_USER, DB_PASS)) {
            conn.setAutoCommit(false);

            loadCalendarDates(conn, tmpDir.resolve("calendar_dates.txt"));
            loadStops(conn, tmpDir.resolve("stops.txt"));
            loadTrips(conn, tmpDir.resolve("trips.txt"));
            loadStopTimes(conn, tmpDir.resolve("stop_times.txt"));
            loadAgencies(conn, tmpDir.resolve("agency.txt"));
            loadRoutes(conn, tmpDir.resolve("routes.txt"));
            loadCalendar(conn, tmpDir.resolve("calendar.txt"));
            conn.commit();
            System.out.println("Data loaded successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

