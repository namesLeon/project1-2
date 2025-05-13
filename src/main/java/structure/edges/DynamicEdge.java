package structure.edges;

import gtfsparsingzip.data.Trip;
import structure.visitor.TravelVisitor;
import structure.stops.StopStructure;
import structure.timetables.DynamicTimetable;

import java.time.LocalTime;

public class DynamicEdge extends Edge{

    public DynamicEdge(StopStructure start, StopStructure end, int costRideMinutes){
        super(start, end, costRideMinutes);
        this.timetable = new DynamicTimetable(costRideMinutes);
    }

    public void addConnection(LocalTime time, Trip trip, int costInMinutes){
        ((DynamicTimetable) timetable).addConnection(time, trip, costInMinutes);
    }

    @Override
    public void acceptVisitor(TravelVisitor visitor){

    }
}
