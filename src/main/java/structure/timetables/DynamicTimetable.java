package structure.timetables;

import gtfsparsingzip.data.Trip;
import structure.TravelVisitor;
import structure.records.ConnectionRecord;
import structure.records.RideRecord;
import structure.records.WalkRecord;

import java.sql.SQLOutput;
import java.time.Duration;
import java.time.LocalTime;
import java.util.PriorityQueue;
import java.util.Stack;

public class DynamicTimetable implements Timetable {
    private PriorityQueue<TimetableCell>[] connectionArray;
    private int staticCostMinutes;

    public DynamicTimetable(int staticCostMinutes){
        connectionArray = new PriorityQueue[24];
        this.staticCostMinutes = staticCostMinutes;
    }

    @Override
    public int getCost(LocalTime time) {
        TimetableCell cell = getTimetableCell(time);
        if(cell == null) return staticCostMinutes;
        int wait = getWaitCost(time, cell.getTime());
        return Math.min(staticCostMinutes,wait + cell.getCostMinutes());
    }

    @Override
    public void acceptVisitor(TravelVisitor visitor) {

    }

    public void addConnection(LocalTime time, Trip trip, int costMinutes){
        int index = getIndex(time);
        if(connectionArray[index] == null){
            connectionArray[index] = new PriorityQueue<TimetableCell>();
        }
        connectionArray[index].add(new TimetableCell(time, trip, costMinutes));
    }

    private TimetableCell getTimetableCell(LocalTime time){
        int startingIndex = getIndex(time);
        int index = getIndex(time);
        int indexDiff = 0;
        boolean found = false;
        Stack<TimetableCell> removed = new Stack<>();
        TimetableCell polled = null;
        PriorityQueue<TimetableCell> queue;
        Duration difference;
        do{
            queue = connectionArray[index];
            while(queue!=null&&(!queue.isEmpty() || !found)){
                polled = queue.poll();
                removed.push(polled);
                difference = Duration.between(time, polled.getTime());
                if(difference.isNegative()) difference = difference.plusHours(24);
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
            indexDiff++;
            if(index > 23) index = 0;
            if(indexDiff >= Math.ceil(staticCostMinutes/60)+1) break;
        }
        while(index!=startingIndex && !found );
        //if(indexDiff>= Math.ceil((double) staticCostMinutes /60)+1) return null;
        return polled;
    }

    public ConnectionRecord getConnection(LocalTime time){
        TimetableCell entry = getTimetableCell(time);
        if(entry == null){
            return new WalkRecord(staticCostMinutes);
        }
        Duration duration = Duration.between(time, entry.getTime());
        if(duration.isNegative()){ duration = duration.plusHours(24);
        }
        ConnectionRecord record = new RideRecord(entry.getTime(), entry.getTrip(), entry.getCostMinutes(), (int)duration.toMinutes());
        if(record.getTotalCost() > staticCostMinutes){
            record = new WalkRecord(staticCostMinutes);
        }
        return record;
    }

    private int getIndex(LocalTime time){
        return time.getHour();
    }
    private int getWaitCost(LocalTime start, LocalTime end){
        Duration wait = Duration.between(start, end);
        if(wait.isNegative()) wait = wait.plusHours(24);
        return (int) wait.toMinutes();
    }

    public static void main(String[] args){
        Trip trip1 = new Trip("1","1","1","1");
        LocalTime time1 = LocalTime.of(0,10);
        Trip trip2 = new Trip("2","2","2","2");
        LocalTime time2 = LocalTime.of(23,39, 49);
        LocalTime time3 = LocalTime.of(23,50);
        DynamicTimetable tb = new DynamicTimetable(60);
        tb.addConnection(time1, trip1, 30);
        printTrip(tb, time2);
        printTrip(tb, time3);

        //System.out.println("Trip time: "+tb.getCost(time2));

    }

    public static void printTrip(DynamicTimetable tb, LocalTime time2){
        ConnectionRecord record = tb.getConnection(time2);
        System.out.println("Started at: "+time2);
        System.out.println(record);
        System.out.println("Arrived at: "+time2.plusMinutes(record.getTotalCost()));
    }
}
