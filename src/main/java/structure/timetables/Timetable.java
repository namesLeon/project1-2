package structure.timetables;

import structure.TravelVisitor;

import java.time.LocalTime;

public interface Timetable {
    int getCost(LocalTime time);
    void acceptVisitor(TravelVisitor visitor);

}
