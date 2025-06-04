package structs;

import java.util.ArrayList;
import java.util.List;

public class Trip {
    public final String tripId;
    public final String routeId;
    public final String headSign;  // from trips.txt
    public final List<StopTime> stopTimes = new ArrayList<>();

    public Trip(String tripId, String routeId, String headSign) {
        this.tripId = tripId;
        this.routeId = routeId;
        this.headSign = headSign;
    }

    public void addStopTime(StopTime st) {
        stopTimes.add(st);
    }
}