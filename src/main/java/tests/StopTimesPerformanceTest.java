package tests;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;

import data.StopTime;
import loaders.GTFSMySQLLoader;

public class StopTimesPerformanceTest {
    public static void main(String[] args) {
        String host = "localhost";
        String db = "gtfs_db";
        String user = "root";
        String password = "admin";

/*
 *  This test loads all stop_times from the database and simulates many trip searches like the routing engine would.
 *  -  It randomly picks stop times and searches for the next stop on the same trip,
 */

        try {
            GTFSMySQLLoader loader = new GTFSMySQLLoader(host, db, user, password);

            System.out.println("Connected to database!");

            long startLoad = System.currentTimeMillis();
            List<StopTime> stopTimes = loader.loadStopTimes();
            long endLoad = System.currentTimeMillis();
            System.out.println("Loaded " + stopTimes.size() + " stop_times in " + (endLoad - startLoad) + "ms.");

            System.out.println("Starting simulation");

            Random random = new Random();
            int simulatedTrips = 10000; // adjust for even heavier test
            long startSimulate = System.currentTimeMillis();

            for (int i = 0; i < simulatedTrips; i++) {
                StopTime randomStopTime = stopTimes.get(random.nextInt(stopTimes.size()));

                for (StopTime st : stopTimes) {
                    if (st.getTripId().equals(randomStopTime.getTripId())) {
                        if (st.getStopSequence() > randomStopTime.getStopSequence()) {
                            break;
                        }
                    }
                }
            }

            long endSimulate = System.currentTimeMillis();
            System.out.println("Simulated " + simulatedTrips + " trip searches in " + (endSimulate - startSimulate) + "ms.");

            loader.close();
            System.out.println("Connection closed.");

        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
        }
    }
}
