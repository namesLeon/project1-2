package structure.timetables;

import structure.visitor.TravelVisitor;

import java.time.LocalTime;

public class StaticTimetable implements Timetable{
    int walkingCostMinutes;

    public StaticTimetable(int walkingCostMinutes){
        this.walkingCostMinutes = walkingCostMinutes;
    }
    @Override
    public int getCost(LocalTime time) {
        return walkingCostMinutes;
    }

    @Override
    public void acceptVisitor(TravelVisitor visitor) {

    }
}
