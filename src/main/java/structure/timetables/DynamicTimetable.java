package structure.timetables;

import gtfsparsingzip.data.Trip;
import structure.TravelVisitor;
import structure.records.ConnectionRecord;
import structure.records.TripRecord;
import structure.records.WalkRecord;

import java.time.Duration;
import java.time.LocalTime;
import java.util.PriorityQueue;
import java.util.Stack;

public class DynamicTimetable implements Timetable {
    private PriorityQueue<TripRecord>[] connectionArray;
    private int staticCostMinutes;

    public DynamicTimetable(int staticCostMinutes){
        connectionArray = new PriorityQueue[24];
        this.staticCostMinutes = staticCostMinutes;
    }

    @Override
    public int getCost(LocalTime time) {
        TripRecord record = getTripRecord(time);
        if(record == null) return staticCostMinutes;
        long wait = Math.abs(record.getTime().minusHours(time.getHour()).getHour()) * 60 + Math.abs(record.getTime().minusMinutes(time.getMinute()).getMinute());
        return (int) wait + record.getCostMinutes();
    }

    @Override
    public void acceptVisitor(TravelVisitor visitor) {

    }

    public void addConnection(LocalTime time, Trip trip, int costMinutes){
        int index = getIndex(time);
        if(connectionArray[index] == null){
            connectionArray[index] = new PriorityQueue<TripRecord>();
        }
        connectionArray[index].add(new TripRecord(time, trip, costMinutes));
    }

    private TripRecord getTripRecord(LocalTime time){
        int startingIndex = getIndex(time);
        int index = getIndex(time);
        boolean found = false;
        Stack<TripRecord> removed = new Stack<>();
        TripRecord polled = null;
        PriorityQueue<TripRecord> queue;
        Duration difference;
        do{
            queue = connectionArray[index];
            while(!queue.isEmpty() || !found){
                polled = queue.poll();
                removed.push(polled);
                difference = Duration.between(time, polled.getTime());
                if(index >= startingIndex){
                    if(time.isBefore(polled.getTime()) || time.equals(polled.getTime())){
                        found = true;
                    }
                }
                else{
                    if(time.isAfter(polled.getTime()) || time.equals(polled.getTime())){
                        found = true;
                    }
                }
            }
            while(!removed.isEmpty()){
                queue.add(removed.pop());
            }
            index++;
            if(index > 23) index = 0;
        }
        while(index!=startingIndex || !found|| (Math.abs(index - time.getHour()) >= Math.ceil((double) staticCostMinutes /60)));
        if(Math.abs(index - time.getHour()) >= Math.ceil((double) staticCostMinutes /60)) return null;
        return polled;
    }

    public ConnectionRecord getConnection(LocalTime time){
        TripRecord record = getTripRecord(time);
        if(record == null){
            return new WalkRecord(staticCostMinutes);
        }
        return record;
    }

    private int getIndex(LocalTime time){
        return time.getHour();
    }

}
