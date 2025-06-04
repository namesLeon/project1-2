package structs;

public class RouteInfo {
    public final String routeId;
    public final String operator;
    public final String shortName;
    public final String longName;
    public final String headSign;

    public RouteInfo(String routeId,
                     String operator,
                     String shortName,
                     String longName,
                     String headSign) {
        this.routeId = routeId;
        this.operator = operator;
        this.shortName = shortName;
        this.longName = longName;
        this.headSign = headSign;
    }

    @Override
    public String toString(){
        return " | routeId : "+routeId+" | operator : "+operator + " | short name : "+shortName+
                " | long name : " + longName + " | head sign : "+headSign;
    }
}