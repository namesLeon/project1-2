package structs;

public class JourneyStep {
    public final String mode;         
    public final double[] to;         
    public final int durationMin;
    public final String startTime;    
    public final String stop;         
    public final RouteInfo routeInfo; 

    public JourneyStep(double toLat, double toLon, int durationMin, String startTime) {
        this.mode = "walk";
        this.to = new double[]{toLat, toLon};
        this.durationMin = durationMin;
        this.startTime = startTime;
        this.stop = null;
        this.routeInfo = null;
    }

    public JourneyStep(double toLat, double toLon, int durationMin, String startTime,
                       String stop, RouteInfo routeInfo) {
        this.mode = "ride";
        this.to = new double[]{toLat, toLon};
        this.durationMin = durationMin;
        this.startTime = startTime;
        this.stop = stop;
        this.routeInfo = routeInfo;
    }
}