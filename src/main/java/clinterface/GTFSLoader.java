package clinterface;

import storage.TransitGraph;
import structs.*;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

//unzips GTFS feed and loads stops, agency, routes, trips, stop_times into TransitGraph
//i assume all trips described in stop_times.txt happen in the same, current day
//uses Apache Commons CSV for reliable parsing of quoted fields
//(some stop_name fields in stops.txt might contain commas, which would break the standard split on ',')
//I assume a standard time format of hh:mm:ss when parsing stop_times.txt data (arrival_time, departure_time), 
//as all sample GTFS feeds I've looked at used this format (and not hh:mm, which is used in both 
//routeFrom and routeStep objects)

public class GTFSLoader {
    private final TransitGraph graph;
    private final Map<String,String> agencyMap = new HashMap<>();
    private final Map<String,RouteInfo> routes = new HashMap<>();
    private final Map<String,Trip> trips = new LinkedHashMap<>();

    public GTFSLoader(TransitGraph graph) {
        this.graph = graph;
    }

    public void loadFromZip(Path zipPath) throws IOException {
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
        for (List<DepartureEvent> list : graph.departures.values()) {
            list.sort(Comparator.comparingInt(e -> e.departureSec));
        }
        graph.computeAllWalkEdges();
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