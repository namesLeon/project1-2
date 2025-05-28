package structure.edges;

import structure.visitor.TravelVisitor;
import structure.stops.StopStructure;
import structure.timetables.Timetable;

import java.time.LocalTime;

public abstract class Edge {
    private static Double latitudeToKilometerConstant;
    private static Double longitudeToKilometerConstant;
    private StopStructure start;
    private StopStructure end;
    protected int staticCostMinutes;
    protected Timetable timetable;

    public Edge(StopStructure start, StopStructure end, int walkingCostMinutes){
        this.start = start;
        this.end = end;
        this.staticCostMinutes = walkingCostMinutes;
    }
    public int getCost(LocalTime time){
        return timetable.getCost(time);
    }

    public StopStructure getStart(){ return start;}
    public StopStructure getEnd(){ return end;}


    public abstract void acceptVisitor(TravelVisitor visitor);

//    private int calculateStaticCostMinutes(){
//        return GraphUtilities.calculateStaticCostMinutes(start, end, speed);
//    }
}
