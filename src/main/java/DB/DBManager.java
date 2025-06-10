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
 *  Also, when using the class first time in your code, you have to run initializeDataBase() method!
 */
public class DBManager {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/gtfs_Amsterdam?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String JDBC_USER = "BCS1510";
    private static final String JDBC_PASS = "BCS1510";

    /**
     * Run the main method if you don't have the database initialized yet.
     * Make sure to first: <br>
     *  - activate mysql on your computer <br>
     *  - create a database named 'gtfs_Amsterdam' on your computer <br>
     *  - change the JDBC_USER and JDBC_PASS to credentials of some user from your database <br>
     * You don't have to run this method everytime, matter of time it is best to run it as rarely as possible
     * as it takes like 30 seconds to load the gtfs files into the database
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        DBManager manager = new DBManager();
        manager.initializeDataBase("data/amsterdam_gtfs.zip");
//        ArrayList<DepartureEvent> events = manager.getDepartureEventsAfterStopDeletion("2636678");
//        for(DepartureEvent event : events){
//            System.out.println(event);
//        }
//        events = manager.getDepartureEventsAfterStopDeletion("2636726");
//        for(DepartureEvent event : events){
//            System.out.println(event);
//        }
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

    /**
     * Used for determining new departure events that are a result of deleting a specific stop from the gtfs database.
     * We use sql JOINS to find them.
     * @param deletedStopId - String value of stopId corresponding to the stop you want to remove from database
     * @return
     */
    public ArrayList<DepartureEvent> getDepartureEventsAfterStopDeletion(String deletedStopId){
        ArrayList<DepartureEvent> newDepartures = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS)){
            conn.setAutoCommit(false);
            PreparedStatement st1 = conn.prepareStatement("""
                        CREATE TEMPORARY TABLE temp_edges AS
                        SELECT e1.start_stop as start_stop, e2.end_stop as end_stop, trip_id,
                        e1.departure_time as departure_time, e2.arrival_time as arrival_time,
                        TIMEDIFF(e2.arrival_time, e2.departure_time) as transfer_length
                        FROM edges e1 JOIN edges e2
                        USING(trip_id)
                        WHERE e1.end_stop = e2.start_stop AND e1.end_stop = ?
                    """);
            st1.setString(1, deletedStopId);
            st1.executeUpdate();
            PreparedStatement st3 = conn.prepareStatement("""
                        DELETE FROM edges
                        WHERE start_stop = ? OR end_stop = ?;
                    """);
            st3.setString(1,deletedStopId);
            st3.setString(2, deletedStopId);
            st3.executeUpdate();

            String updateEdges = """
                        INSERT INTO edges SELECT * FROM temp_edges
                    """;
            Statement st = conn.createStatement();
            st.executeUpdate(updateEdges);
            PreparedStatement st2 = conn.prepareStatement("""
                    SELECT
                   route_id, trip_id, start_stop, end_stop, departure_time, arrival_time, transfer_length,
                   trip_headsign, agency_name, route_short_name, route_long_name
                   FROM (SELECT * FROM
                   temp_edges
                   JOIN
                   (
                   SELECT trip_id, trip_headsign, route_id FROM trips
                   ) b
                   USING(trip_id)
                   ) c
                   JOIN
                   (SELECT route_id, agency_name, route_short_name, route_long_name
                   FROM routes JOIN agency USING(agency_id)) d
                   USING(route_id);
                    """);
            ResultSet rs = st2.executeQuery();
            while (rs.next()) {
                newDepartures.add(createNewDepartureEvent(rs));
            }
            Statement dropTempTable = conn.createStatement();
            dropTempTable.executeUpdate("""
                    DROP TEMPORARY TABLE temp_edges
                    """);
            conn.commit();
            //System.out.println("Created new edges.");
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

    public void reloadEdges(){
        DBloader dBloader = new DBloader();
        try(Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS)){
            dBloader.createEdgesTable(conn);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
