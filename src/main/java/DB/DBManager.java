package DB;

import structs.DepartureEvent;
import structs.RouteInfo;
import structs.StopTime;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;

/**
 * If you want to use this class, make sure that you have a proper database setup
 *  - create a gtfs_Amsterdam database if none exists yet
 *  - change the DB_USER and DB_PASS according to your database setup
 *
 *  Also, when you use the class first time in your code, you have to run initializeDataBase() method!
 */
public class DBManager {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/gtfs_Amsterdam?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String JDBC_USER = "BCS1510";
    private static final String JDBC_PASS = "BCS1510";

    /**
     * Run the main method if you don't have the database initialized yet.
     * Make sure to first:
     *  - activate mysql on your computer
     *  - create a database named 'gtfs_Amsterdam' on your computer
     *  - change the JDBC_USER and JDBC_PASS to credentials of some user from your database
     * You don't have to run this method everytime, matter of time it is best to run it as rarely as possible
     * since it takes like 30 seconds to load the gtfs files into the database
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        DBManager manager = new DBManager();
        ArrayList<DepartureEvent> departureEvents = manager.getDepartureEventsAfterStopDeletion("2635837");
        DepartureEvent e = departureEvents.get(1);
        System.out.println(e.toString()
        );
    }

    public void initializeDataBase(String zipDirectory){
        try(Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS)) {
            DBloader loader = new DBloader();
            Path zipPath = Path.of(zipDirectory);
            loader.createSQLTables(conn);
            loader.loadFromZip(zipPath, conn);
        }
        catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ArrayList<DepartureEvent> getDepartureEventsAfterStopDeletion(String deletedStopId){
        ArrayList<DepartureEvent> newDepartures = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS)){
            PreparedStatement st = conn.prepareStatement("""
                    SELECT
                    route_id, trip_id, start_stop, end_stop, departure_time, arrival_time, transfer_length,
                    trip_headsign, agency_name, route_short_name, route_long_name
                    FROM (SELECT * FROM
                    	(
                    	SELECT e1.start_stop as start_stop, e2.end_stop as end_stop, trip_id,
                    	e1.departure_time as departure_time, e2.arrival_time as arrival_time,
                    	TIMEDIFF(e2.arrival_time, e2.departure_time) as transfer_length
                    	FROM edges e1 JOIN edges e2
                    	USING(trip_id)
                    	WHERE e1.end_stop = e2.start_stop AND e1.end_stop = ?
                    	) a
                    	JOIN
                    	(
                    	SELECT trip_id, trip_headsign FROM trips
                    	) b
                    	USING(trip_id)
                    ) c
                    JOIN
                    (SELECT route_id, agency_name, route_short_name, route_long_name
                    FROM routes JOIN agency USING(agency_id))d
                    """);
            st.setString(1, deletedStopId);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                newDepartures.add(createNewDepartureEvent(rs));
            }
            System.out.println("Created new edges.");
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return newDepartures;
    }

    private DepartureEvent createNewDepartureEvent(ResultSet rs) throws SQLException {
        String startStop = rs.getString(3);
        String endStop = rs.getString(4);

        RouteInfo routeInfo = new RouteInfo(
                rs.getString(1),
                rs.getString(9),
                rs.getString(10),
                rs.getString(11),
                rs.getString(8)
        );
        return new DepartureEvent(
                StopTime.parseTime(rs.getString(5)),
                rs.getString(2),
                startStop,
                endStop,
                StopTime.parseTime(rs.getString(6)),
                routeInfo
        );
    }
}
