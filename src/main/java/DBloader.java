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

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: java GTFSLoader <calendar_dates.txt> <stops.txt> <trips.txt> <stop_times.txt>");
            System.exit(1);
        }
        String calendarFile = args[0];
        String stopsFile = args[1];
        String tripsFile = args[2];
        String stopTimesFile = args[3];
        try (Connection conn = DriverManager.getConnection(useEnvJDBCurl ? System.getenv("ROUTING_ENGINE_MYSQL_JDBC") : JDBC_URL, DB_USER, DB_PASS)) {
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
        try (BufferedReader reader = Files.newBufferedReader(Path.of(filePath));
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
            System.out.println("Loaded stops from " + filePath);
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

    private static void loadStops(Connection conn, String filePath) throws SQLException, IOException {
        String sql = "INSERT INTO stops(stop_id, stop_name, stop_lat, stop_lon) " +
                "VALUES (?, ?, ?, ?)";

        try (BufferedReader reader = Files.newBufferedReader(Path.of(filePath));
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

    private static void loadTrips(Connection conn, String filePath) throws SQLException, IOException {
        String sql = "INSERT INTO trips(trip_id, route_id, trip_headsign, service_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (BufferedReader reader = Files.newBufferedReader(Path.of(filePath));
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
            System.out.println("Loaded stops from " + filePath);
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

    private static void loadStopTimes(Connection conn, String filePath) throws SQLException, IOException {
        String sql = "INSERT INTO stop_times(trip_id, stop_sequence, stop_id, arrival_time, departure_time) " +
                "VALUES (?, ?, ?, ?, ?)";
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm:ss");
        try (BufferedReader reader = Files.newBufferedReader(Path.of(filePath));
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
            System.out.println("Loaded stops from " + filePath);
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

    private static void loadAgencies(Connection conn, String filePath) throws IOException, SQLException{
        String sql = "INSERT INTO agency(agency_id, agency_name, agency_url) " +
                "VALUES (?, ?, ?)";
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm:ss");
        try (BufferedReader reader = Files.newBufferedReader(Path.of(filePath));
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
            System.out.println("Loaded stops from " + filePath);
        }
    }

    private static void loadRoutes(Connection conn, String filePath) throws IOException, SQLException {
        String sql = "INSERT INTO routes(route_id, agency_id, agency_url) " +
                "VALUES (?, ?, ?)";
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm:ss");
        try (BufferedReader reader = Files.newBufferedReader(Path.of(filePath));
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
            System.out.println("Loaded stops from " + filePath);
        }
    }


    public void loadFromZip(Path zipPath) throws IOException {
        //clear previous data
        Path tmpDir = Files.createTempDirectory("gtfs");
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
        parseStops(tmpDir.resolve("stops.txt"));
        parseAgencies(tmpDir.resolve("agency.txt"));
        parseRoutes(tmpDir.resolve("routes.txt"));
        parseTrips(tmpDir.resolve("trips.txt"));
        parseStopTimes(tmpDir.resolve("stop_times.txt"));
    }

    private void parseCalendarDates(Path p) throws IOException{

    }


    private void parseStops(Path p) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(p);
             CSVParser parser = CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withTrim()
                     .parse(reader)) {
            for (CSVRecord rec : parser) {
                String id = rec.get("stop_id");
                String name = rec.get("stop_name");
                double lat = Double.parseDouble(rec.get("stop_lat"));
                double lon = Double.parseDouble(rec.get("stop_lon"));
                graph.stops.put(id, new Stop(id, name, lat, lon));
            }
        }
    }

    private void parseAgencies(Path p) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(p);
             CSVParser parser = CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withTrim()
                     .parse(reader)) {
            for (CSVRecord rec : parser) {
                agencyMap.put(rec.get("agency_id"), rec.get("agency_name"));
            }
        }
    }

    private void parseRoutes(Path p) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(p);
             CSVParser parser = CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withTrim()
                     .parse(reader)) {
            for (CSVRecord rec : parser) {
                String id = rec.get("route_id");
                String agId = rec.get("agency_id");
                String operator = agencyMap.getOrDefault(agId, "");
                String shortName = rec.get("route_short_name");
                String longName = rec.get("route_long_name");
                routes.put(id, new RouteInfo(id, operator, shortName, longName, ""));
            }
        }
    }

    private void parseTrips(Path p) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(p);
             CSVParser parser = CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withTrim()
                     .parse(reader)) {
            for (CSVRecord rec : parser) {
                String tripId = rec.get("trip_id");
                String routeId = rec.get("route_id");
                String headSign = rec.get("trip_headsign");
                Trip trip = new Trip(tripId, routeId, headSign);
                trips.put(tripId, trip);
            }
        }
    }

    private void parseStopTimes(Path p) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(p);
             CSVParser parser = CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withTrim()
                     .parse(reader)) {
            for (CSVRecord rec : parser) {
                String tripId = rec.get("trip_id");
                Trip trip = trips.get(tripId);
                if (trip == null) continue;
                String stopId = rec.get("stop_id");
                int arrival = StopTime.parseTime(rec.get("arrival_time"));
                int departure = StopTime.parseTime(rec.get("departure_time"));
                int seq = Integer.parseInt(rec.get("stop_sequence"));
                trip.addStopTime(new StopTime(tripId, stopId, arrival, departure, seq));
            }
        }
        for (Trip trip : trips.values()) {
            List<StopTime> list = trip.stopTimes;
            RouteInfo info = routes.get(trip.routeId);
            for (int i = 0; i + 1 < list.size(); i++) {
                StopTime curr = list.get(i);
                StopTime next = list.get(i+1);
                DepartureEvent ev = new DepartureEvent(
                        curr.departureSec,
                        curr.tripId,
                        curr.stopId,
                        next.stopId,
                        next.arrivalSec,
                        new RouteInfo(
                                info.routeId,
                                info.operator,
                                info.shortName,
                                info.longName,
                                trip.headSign)
                );
                graph.departures.computeIfAbsent(curr.stopId, k -> new ArrayList<>()).add(ev);
            }
        }
    }
}

