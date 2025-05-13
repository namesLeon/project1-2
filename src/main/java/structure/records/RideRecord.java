package structure.records;

import gtfsparsingzip.data.Trip;
import structure.visitor.TravelVisitor;

import java.time.LocalTime;

public class RideRecord implements Comparable, ConnectionRecord{
    private LocalTime time;
    private Trip trip;
    private int rideCost;
    private int waitCost;

    public RideRecord(LocalTime time, Trip trip, int costMinutes, int waitCost){
        this.time = time;
        this.trip = trip;
        this.rideCost = costMinutes;
        this.waitCost = waitCost;
    }

    public LocalTime getTime() {
        return time;
    }

    public Trip getTrip() {
        return trip;
    }

    public int getRideCost() {
        return rideCost;
    }

    public int getWaitCost(){
        return waitCost;
    }

    @Override
    public int getTotalCost(){
        return waitCost + rideCost;
    }

    @Override
    public int compareTo(Object o) {
        RideRecord other = (RideRecord) o;
        return this.time.compareTo(other.getTime());
    }

    @Override
    public void acceptVisitor(TravelVisitor visitor) {

    }

    @Override
    public String toString(){
        return "Ride Record - wait : "+waitCost + " | ride : "+rideCost;
    }
}
