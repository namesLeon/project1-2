package structure.records;

import gtfsparsingzip.data.Trip;
import structure.TravelVisitor;

import java.time.LocalTime;

public class TripRecord implements Comparable, ConnectionRecord{
    private LocalTime time;
    private Trip trip;
    private int costMinutes;

    public TripRecord(LocalTime time, Trip trip, int costMinutes){
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
        TripRecord other = (TripRecord) o;
        return this.time.compareTo(other.getTime());
    }

    @Override
    public void acceptVisitor(TravelVisitor visitor) {

    }
}
