import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import data.Stop;
import data.StopTime;
import data.Trip;
import data.CalendarDate;
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

            Map<String, Route> routes = loader.loadRoutes();
            System.out.println("Loaded " + routes.size() + " routes.");
            routes.values().stream().limit(5).forEach(route ->
                System.out.println("Route: " + route.getRouteId() + " - "
                                + route.getShortName() + " ("
                                + route.getLongName() + ") Type: "
                                + route.getType())
            );

            List<CalendarDate> calendarDates = loader.loadCalendarDates();
            System.out.println("Loaded " + calendarDates.size() + " calendar dates.");
            calendarDates.stream().limit(5).forEach(cd ->
                System.out.println("CalendarDate: ServiceID " + cd.getServiceId() 
                                + ", Date " + cd.getDate()
                                + ", ExceptionType " + cd.getExceptionType())
            );

            List<StopTime> stopTimes = loader.loadStopTimes();
            System.out.println("Loaded " + stopTimes.size() + " stop times.");
            stopTimes.stream().limit(5).forEach(st ->
                System.out.println("StopTime: TripID " + st.getTripId()
                                + ", Arrival " + st.getArrivalTime()
                                + ", Departure " + st.getDepartureTime()
                                + ", StopID " + st.getStopId()
                                + ", Sequence " + st.getStopSequence())
            );

            Map<String, Trip> trips = loader.loadTrips();
            System.out.println("Loaded " + trips.size() + " trips.");
            trips.values().stream().limit(5).forEach(trip ->
                System.out.println("Trip: TripID " + trip.getTripId()
                                + ", RouteID " + trip.getRouteId()
                                + ", ServiceID " + trip.getServiceId()
                                + ", HeadSign " + trip.getHeadsign())
            );


            loader.close();
            System.out.println("Connection closed.");

        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
        }
    }
}
