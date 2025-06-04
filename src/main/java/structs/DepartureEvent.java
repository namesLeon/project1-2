package structs;

public class DepartureEvent {
    public final int departureSec;
    public final String tripId;
    public final String fromStopId;
    public final String toStopId;
    public final int arrivalSec;
    public final RouteInfo routeInfo;

    public DepartureEvent(int departureSec,
                          String tripId,
                          String fromStopId,
                          String toStopId,
                          int arrivalSec,
                          RouteInfo routeInfo) {
        this.departureSec = departureSec;
        this.tripId = tripId;
        this.fromStopId = fromStopId;
        this.toStopId = toStopId;
        this.arrivalSec = arrivalSec;
        this.routeInfo = routeInfo;
    }

    @Override
    public String toString(){
        return "departure sec : "+departureSec+" | trip id : "+tripId+" | from stop : "+fromStopId+
                " | to stop : "+toStopId+" | arrival sec : "+arrivalSec+ routeInfo.toString();
    }
}