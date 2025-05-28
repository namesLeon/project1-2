package gtfsparsingzip.data;

public class Route {
    private String routeId;
    private String shortName;
    private String longName;
    private String type;

    public Route(String routeId, String shortName, String longName, String type) {
        this.routeId = routeId;
        this.shortName = shortName;
        this.longName = longName;
        this.type = type;
    }

    public String getRouteId() { return routeId; }
    public String getShortName() { return shortName; }
    public String getLongName() { return longName; }
    public String getType() { return type; }
}
