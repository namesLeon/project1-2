package tests;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;
import java.io.IOException;

import data.Stop;
import data.StopTime;
import data.Trip;
import loaders.GTFSLoader;
import data.CalendarDate;
import data.Route;

public class StopTimesZipPerformanceTest {
    public static void main(String[] args) {
        String zipPath = "gtfs_berlin.zip"; // or correct path if different

        try {
            GTFSLoader loader = new GTFSLoader(zipPath);

            System.out.println("Opened GTFS zip file");

            long startLoad = System.currentTimeMillis();
            List<StopTime> stopTimes = loader.loadStopTimes();
            long endLoad = System.currentTimeMillis();
            System.out.println("Loaded " + stopTimes.size() + " stop_times in " + (endLoad - startLoad) + "ms.");

            if (stopTimes.isEmpty()) {
                System.err.println("No stop_times loaded");
                return;
            }

            System.out.println("Starting simulation");

            Random random = new Random();
            int simulatedTrips = 10000;
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

        } catch (IOException e) {
            System.err.println("IO Error: " + e.getMessage());
        }
    }
}