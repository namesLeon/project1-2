package algorithms;

import structure.stops.StopStructure;

import java.time.Duration;
import java.time.LocalTime;

public class ArrivalTableEntry implements Comparable<ArrivalTableEntry>{
    private StopStructure fromStop;
    private int costInMinutes;

    public ArrivalTableEntry(StopStructure from, int timeDiffMinutes){
        this.fromStop = from;
        this.costInMinutes = timeDiffMinutes;
    }

    public StopStructure getFromStop() {
        return fromStop;
    }

    public void setFromStop(StopStructure fromStop) {
        this.fromStop = fromStop;
    }

    public int getCostInMinutes() {
        return costInMinutes;
    }

    public void setCostInMinutes(int costInMinutes) {
        this.costInMinutes = costInMinutes;
    }

    @Override
    public int compareTo(ArrivalTableEntry o) {
        return costInMinutes - o.getCostInMinutes();
    }
}
