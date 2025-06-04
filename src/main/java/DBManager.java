import structs.DepartureEvent;
import structs.RouteInfo;
import structs.StopTime;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class DBManager {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/gtfs_Amsterdam?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "BCS1510";
    private static final String DB_PASS = "BCS1510";
    private Connection conn;


    public static void main(String[] args) throws IOException {
        DBManager manager = new DBManager();
        ArrayList<DepartureEvent> departureEvents = manager.getDepartureEventsAfterStopDeletion("2635837");
        DepartureEvent e = departureEvents.get(1);
        System.out.println(e.toString()
        );
    }

    public DBManager(){
        try{
            conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
        }
        catch (SQLException e){
            conn = null;
            e.printStackTrace();
        }
    }

    public void initializeDataBase(String zipDirectory){
        DBloader loader = new DBloader();
        Path zipPath = Path.of(zipDirectory);
        loader.createSQLTables();
        try {
            loader.loadFromZip(zipPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ArrayList<DepartureEvent> getDepartureEventsAfterStopDeletion(String deletedStopId){
        ArrayList<DepartureEvent> newDepartures = null;
        try {
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
            newDepartures = new ArrayList<>();

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
