package structure.timetables;

import structure.visitor.TravelVisitor;

import java.time.LocalTime;

public interface Timetable {
    int getCost(LocalTime time);
    void acceptVisitor(TravelVisitor visitor);

}
