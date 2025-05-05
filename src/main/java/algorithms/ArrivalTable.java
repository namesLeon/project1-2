package algorithms;

import structure.stops.StopStructure;

import java.util.HashMap;

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
        }
    }

    public ArrivalTableEntry getEntryForStop(StopStructure s){
        return arrivalMap.getOrDefault(s, null);
    }

    public int getCostForStop(StopStructure s){
        return arrivalMap.get(s).getCostInMinutes();
    }
}
