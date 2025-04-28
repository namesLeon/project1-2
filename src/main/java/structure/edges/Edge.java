package structure.edges;

import structure.TravelVisitor;
import structure.stops.StopStructure;
import structure.timetables.Timetable;

import java.time.LocalTime;

public abstract class Edge {
    private static Double latitudeToKilometerConstant;
    private static Double longitudeToKilometerConstant;
    private StopStructure start;
    private StopStructure end;
    private static double speed = 5;
    protected int staticCostMinutes;
    protected Timetable timetable;

    public Edge(StopStructure start, StopStructure end){
        this.start = start;
        this.end = end;
        if(latitudeToKilometerConstant == null){
            latitudeToKilometerConstant = 1/110.574;
        }
        if(longitudeToKilometerConstant == null){
            longitudeToKilometerConstant = 1/111.320;
        }
        this.staticCostMinutes = calculateStaticCostMinutes();
    }
    public int getCost(LocalTime time){
        return timetable.getCost(time);
    }

    public StopStructure getStart(){ return start;}
    public StopStructure getEnd(){ return end;}


    public abstract void acceptVisitor(TravelVisitor visitor);

    private int calculateStaticCostMinutes(){
        double x, y;
        x = latitudeToKilometers(Math.abs(end.getLatitude() - start.getLatitude()));
        y = longitudeToKilometers(Math.abs(end.getLongitude() - start.getLongitude()), end.getLatitude());
        return (int) (Math.sqrt(x*x + y*x) / speed);
    }

    private double latitudeToKilometers(double latitudeDiff){
        return latitudeDiff * latitudeToKilometerConstant;
    }

    private double longitudeToKilometers(double longitudeDiff, double latitude){
        return longitudeToKilometerConstant * Math.cos(Math.toRadians(latitude)) * longitudeDiff;
    }
}
