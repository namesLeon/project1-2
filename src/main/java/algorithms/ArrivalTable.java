package algorithms;

import structure.stops.StopStructure;

import java.util.HashMap;
import java.util.Map;

public class ArrivalTable {
    private final HashMap<StopStructure, ArrivalTableEntry> arrivalMap;

    public ArrivalTable(){
        this.arrivalMap = new HashMap<>();
    }

    public void setTimeDiff(StopStructure to, StopStructure from, int timeDiffMinutes){
        if(!arrivalMap.containsKey(to)) arrivalMap.put(to, new ArrivalTableEntry(from, timeDiffMinutes));
        else{
            ArrivalTableEntry entry = arrivalMap.get(to);
            entry.setFromStop(from);
            entry.setCostInMinutes(timeDiffMinutes);
        }
    }

    public ArrivalTableEntry getEntryForStop(StopStructure s){
        return arrivalMap.getOrDefault(s, null);
    }

    public StopStructure getFromStop(StopStructure s){
        ArrivalTableEntry entry = getEntryForStop(s);
        if(entry!=null){
            return entry.getFromStop();
        }
        return null;
    }

    public int getCostForStop(StopStructure s){
        return arrivalMap.get(s).getCostInMinutes();
    }

    public void printCosts(){
        for(Map.Entry<StopStructure, ArrivalTableEntry> e : arrivalMap.entrySet()){
            System.out.println(e.getKey().getStopId() + " : " + e.getValue().getCostInMinutes());
        }
    }

    private static class ArrivalTableEntry implements Comparable<ArrivalTableEntry>{
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

}
