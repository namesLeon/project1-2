package structure.edges;

import gtfsparsingzip.data.Trip;
import structure.TravelVisitor;
import structure.stops.StopStructure;
import structure.timetables.DynamicTimetable;

import java.time.LocalTime;

public class DynamicEdge extends Edge{

    public DynamicEdge(StopStructure start, StopStructure end){
        super(start, end);
        this.timetable = new DynamicTimetable(staticCostMinutes);
    }

    public void addConnection(LocalTime time, Trip trip, int costInMinutes){
        ((DynamicTimetable) timetable).addConnection(time, trip, costInMinutes);
    }

    @Override
    public void acceptVisitor(TravelVisitor visitor){

    }
}
