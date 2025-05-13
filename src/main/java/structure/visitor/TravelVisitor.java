package structure.visitor;

import structure.edges.DynamicEdge;
import structure.edges.StaticEdge;
import structure.stops.DynamicStop;
import structure.stops.StaticStop;
import structure.timetables.DynamicTimetable;
import structure.timetables.StaticTimetable;

public interface TravelVisitor {
    void accept(StaticStop staticStop);
    void accept(DynamicStop dynamicStop);
    void accept(StaticEdge staticEdge);
    void accept(DynamicEdge dynamicEdge);
    void accept(StaticTimetable staticTimetable);
    void accept(DynamicTimetable dynamicTimetable);
}
