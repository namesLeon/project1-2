package data;

public class Trip {

    private String tripId;
    private String routeId;
    private String serviceId;
    private String headsign;

    public Trip(String tripId, String routeId, String serviceId, String headsign) {
        this.tripId = tripId;
        this.routeId = routeId;
        this.serviceId = serviceId;
        this.headsign = headsign;
    }

    public String getTripId() { return tripId; }
    public String getRouteId() { return routeId; }
    public String getServiceId() { return serviceId; }
    public String getHeadsign() { return headsign; }

}
