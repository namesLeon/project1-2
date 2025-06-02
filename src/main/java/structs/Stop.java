package structs;

public class Stop {
    public final String stopId;
    public final String stopName;
    public final double lat;
    public final double lon;

    public Stop(String stopId, String stopName, double lat, double lon) {
        this.stopId = stopId;
        this.stopName = stopName;
        this.lat = lat;
        this.lon = lon;
    }
}