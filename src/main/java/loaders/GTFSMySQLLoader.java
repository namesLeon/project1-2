package loaders;
import java.sql.*;
import java.util.*;

import data.CalendarDate;
import data.Route;
import data.Stop;
import data.StopTime;
import data.Trip;

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
                Stop stop = new Stop(stopId, stopName, lat, lon);
                stops.put(stopId, stop);
            }
        }
        return stops;
    }

    public Map<String, Route> loadRoutes() throws SQLException {
        Map<String, Route> routes = new HashMap<>();
        String query = "SELECT route_id, route_short_name, route_long_name, route_type FROM routes";

        try (PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String routeId = rs.getString("route_id");
                String shortName = rs.getString("route_short_name");
                String longName = rs.getString("route_long_name");
                String type = rs.getString("route_type");

                Route route = new Route(routeId, shortName, longName, type);
                routes.put(routeId, route);
            }
        }
        return routes;
    }

    public List<CalendarDate> loadCalendarDates() throws SQLException {
        List<CalendarDate> calendarDates = new ArrayList<>();
        String query = "SELECT service_id, date, exception_type FROM calendar_dates";

        try (PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String serviceId = rs.getString("service_id");
                String date = rs.getString("date");
                int exceptionType = rs.getInt("exception_type");

                CalendarDate calendarDate = new CalendarDate(serviceId, date, exceptionType);
                calendarDates.add(calendarDate);
            }
        }
        return calendarDates;
    }

    public List<StopTime> loadStopTimes() throws SQLException {
    List<StopTime> stopTimes = new ArrayList<>();
    String query = "SELECT trip_id, arrival_time, departure_time, stop_id, stop_sequence FROM stop_times";

    try (PreparedStatement stmt = connection.prepareStatement(query);
        ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
            String tripId = rs.getString("trip_id");
            String arrivalTime = rs.getString("arrival_time");
            String departureTime = rs.getString("departure_time");
            String stopId = rs.getString("stop_id");
            int stopSequence = rs.getInt("stop_sequence");

            StopTime stopTime = new StopTime(tripId, arrivalTime, departureTime, stopId, stopSequence);
            stopTimes.add(stopTime);
        }
    }
    return stopTimes;
    }

    public Map<String, Trip> loadTrips() throws SQLException {
        Map<String, Trip> trips = new HashMap<>();
        String query = "SELECT trip_id, route_id, service_id, trip_headsign FROM trips";

        try (PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String tripId = rs.getString("trip_id");
                String routeId = rs.getString("route_id");
                String serviceId = rs.getString("service_id");
                String headsign = rs.getString("trip_headsign");

                Trip trip = new Trip(tripId, routeId, serviceId, headsign);
                trips.put(tripId, trip);
            }
        }
        return trips;
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
