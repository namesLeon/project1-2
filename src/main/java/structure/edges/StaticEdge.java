package structure.edges;

import structure.TravelVisitor;
import structure.stops.StopStructure;
import structure.timetables.StaticTimetable;

public class StaticEdge extends Edge{

    public StaticEdge(StopStructure start, StopStructure end, int walkingCostMinutes){
        super(start, end, walkingCostMinutes);
        this.timetable = new StaticTimetable(walkingCostMinutes);
    }

    @Override
    public void acceptVisitor(TravelVisitor visitor) {

    }
}
