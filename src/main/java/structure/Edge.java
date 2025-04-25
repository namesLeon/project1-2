package structure;

import structure.stops.Stop;
import structure.timetables.DynamicTimetable;
import structure.timetables.Timetable;

import java.time.LocalTime;

public class Edge {
    private Stop start;
    private Stop end;
    private double speed;
    private int staticCostMinutes;
    private Timetable timetable;

    public int getCost(LocalTime time){
        return timetable.getCost(time);
    }

    public Stop getStart(){ return start;}
    public Stop getEnd(){ return end;}

    public void addConnection(LocalTime time, Trip trip){
        if(timetable instanceof DynamicTimetable){
            ((DynamicTimetable) timetable).addConnection();
        }
    }

    public void acceptVisitor(TravelVisitor visitor){

    }
}
