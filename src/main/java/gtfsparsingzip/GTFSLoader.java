package gtfsparsingzip;


import gtfsparsingzip.data.*;
import org.apache.commons.csv.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.*;


public class GTFSLoader {

    private ZipFile zip;
    private String path;

    public GTFSLoader(String zipPath) throws IOException {
        File file = new File(zipPath);
        if (!file.exists() || !file.isFile()) {  // Check if the file exists and is indeed a file.
            System.out.println("Error: The zip file '" + zipPath + "' does not exist.");
            throw new FileNotFoundException("Zip file not found: " + zipPath);
        }

        try {
            this.zip = new ZipFile(file);
            this.path = zipPath;
        }
        catch (IOException e) {
            System.out.println("Error opening GTFS zip file: " + e.getMessage());
            throw e;
        }
    }

    public Map<String, Stop> loadStops() throws IOException {
        Map<String, Stop> stops = new HashMap<>();
        ZipEntry entry = zip.getEntry("stops.txt");
        if (entry == null) return stops;

        try (
                BufferedReader br = new BufferedReader(new InputStreamReader(zip.getInputStream(entry)));
                CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreSurroundingSpaces().parse(br)
        ) {
            for (CSVRecord record : parser) {
                String stopId = record.get("stop_id");
                String stopName = record.get("stop_name");
                double lat = Double.parseDouble(record.get("stop_lat"));
                double lon = Double.parseDouble(record.get("stop_lon"));
                stops.put(stopId, new Stop(stopId, stopName, lat, lon));
            }
        }
        return stops;
    }

    public Map<String, Route> loadRoutes() throws IOException {
        Map<String, Route> routes = new HashMap<>();
        ZipEntry entry = zip.getEntry("routes.txt");
        if (entry == null) return routes;

        try (
                BufferedReader br = new BufferedReader(new InputStreamReader(zip.getInputStream(entry)));
                CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreSurroundingSpaces().parse(br)
        ) {
            for (CSVRecord record : parser) {
                String routeId = record.get("route_id");
                String shortName = record.get("route_short_name");
                String longName = record.get("route_long_name");
                String type = record.get("route_type");
                routes.put(routeId, new Route(routeId, shortName, longName, type));
            }
        }
        return routes;
    }

    public Map<String, Trip> loadTrips() throws IOException {
        Map<String, Trip> trips = new HashMap<>();
        ZipEntry entry = zip.getEntry("trips.txt");
        if (entry == null) return trips;

        try (
                BufferedReader br = new BufferedReader(new InputStreamReader(zip.getInputStream(entry)));
                CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreSurroundingSpaces().parse(br)
        ) {
            for (CSVRecord record : parser) {
                String tripId = record.get("trip_id");
                String routeId = record.get("route_id");
                String serviceId = record.get("service_id");
                String headsign = record.get("trip_headsign");
                trips.put(tripId, new Trip(tripId, routeId, serviceId, headsign));
            }
        }
        return trips;
    }

    public List<StopTime> loadStopTimes() throws IOException {
        List<StopTime> stopTimes = new ArrayList<>();
        ZipEntry entry = zip.getEntry("stop_times.txt");
        if (entry == null) return stopTimes;

        try (
                BufferedReader br = new BufferedReader(new InputStreamReader(zip.getInputStream(entry)));
                CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreSurroundingSpaces().parse(br)
        ) {
            for (CSVRecord record : parser) {
                String tripId = record.get("trip_id");
                String arrivalTime = record.get("arrival_time");
                String departureTime = record.get("departure_time");
                String stopId = record.get("stop_id");
                int stopSequence = Integer.parseInt(record.get("stop_sequence"));
                stopTimes.add(new StopTime(tripId, arrivalTime, departureTime, stopId, stopSequence));
            }
        }
        return stopTimes;
    }

    public List<CalendarDate> loadCalendarDates() throws IOException {
        List<CalendarDate> calendar = new ArrayList<>();
        ZipEntry entry = zip.getEntry("calendar_dates.txt");
        if (entry == null) return calendar;

        try (
                BufferedReader br = new BufferedReader(new InputStreamReader(zip.getInputStream(entry)));
                CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreSurroundingSpaces().parse(br)
        ) {
            for (CSVRecord record : parser) {
                String serviceId = record.get("service_id");
                String date = record.get("date");
                int exceptionType = Integer.parseInt(record.get("exception_type"));
                calendar.add(new CalendarDate(serviceId, date, exceptionType));
            }
        }
        return calendar;
    }

    public void filterBerlinStops() {
        // Get the stops.txt entry from the zip file
        ZipEntry entry = zip.getEntry("stops.txt");
        if (entry == null) {
            System.out.println("stops.txt not found in the zip file.");
            return;
        }

        // Compute the output file path: use the same directory as the zip file if available
        File zipFile = new File(path);
        String parentDir = zipFile.getParent();
        String outputFile;
        if (parentDir != null) {
            outputFile = parentDir + File.separator + "berlin_stops.txt";
        } else {
            outputFile = "berlin_stops.txt";
        }

        try (
                // Read from the stops.txt entry within the zip file
                BufferedReader br = new BufferedReader(new InputStreamReader(zip.getInputStream(entry)));
                CSVParser parser = new CSVParser(br, CSVFormat.DEFAULT
                        .withFirstRecordAsHeader()
                        .withIgnoreSurroundingSpaces());
                // Write the filtered data to a text file
                BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
                CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT
                        .withHeader(parser.getHeaderMap().keySet().toArray(new String[0])))
        ) {
            for (CSVRecord record : parser) {
                String stopName = record.get("stop_name");
                // Filter rows where stop_name contains "(Berlin)"
                if (stopName != null && stopName.contains("(Berlin)")) {
                    printer.printRecord(record);
                }
            }
            System.out.println("Filtered stops written to " + outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printPath(){ System.out.println(path + "\\stops.txt"); }
}
