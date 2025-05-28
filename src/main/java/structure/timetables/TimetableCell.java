package structure.timetables;

import gtfsparsingzip.data.Trip;
import structure.visitor.TravelVisitor;
import structure.records.RideRecord;

import java.time.LocalTime;


public class TimetableCell implements Comparable {
    private LocalTime time;
    private Trip trip;
    private int costMinutes;

    public TimetableCell(LocalTime time, Trip trip, int costMinutes){
        this.time = time;
        this.trip = trip;
        this.costMinutes = costMinutes;
    }

    public LocalTime getTime() {
        return time;
    }

    public Trip getTrip() {
        return trip;
    }

    public int getCostMinutes() {
        return costMinutes;
    }

    @Override
    public int compareTo(Object o) {
        RideRecord other = (RideRecord) o;
        return this.time.compareTo(other.getTime());
    }

    public void acceptVisitor(TravelVisitor visitor) {

    }
}
