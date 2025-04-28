package structure;

import gtfsparsingzip.data.Stop;
import gtfsparsingzip.data.Trip;
import structure.edges.DynamicEdge;
import structure.edges.Edge;
import structure.edges.StaticEdge;
import structure.stops.StopStructure;
import structure.timetables.DynamicTimetable;

import java.time.LocalTime;
import java.util.HashMap;

public class Graph {
    private HashMap<String, StopStructure> stopMap;
    private HashMap<StopStructure, HashMap<StopStructure, Edge>> adjMap;

    public Graph(){
        this.stopMap = new HashMap<>();
        this.adjMap = new HashMap<>();
    }

    // Possible exception here
    public StopStructure getStopById(String id){
        return stopMap.getOrDefault(id, null);
    }

    public boolean isAdjacent(StopStructure start, StopStructure end){
        if(!stopMap.containsValue(start)) return false;
        return adjMap.get(start).containsKey(end);
    }

    public void addStop(StopStructure s){
        stopMap.putIfAbsent(s.getStopId(), s);
        adjMap.putIfAbsent(s, new HashMap<>());
    }

    public void addDynamicConnection(String startId, String endId, LocalTime time, Trip trip, int costInMinutes){
        StopStructure start = getStopById(startId);
        StopStructure end = getStopById(endId);

        if(!isAdjacent(start, end)){
            addDynamicEdge(start, end);
        }
        Edge edge= getEdge(start, end);
        if(edge instanceof DynamicEdge){
            ((DynamicEdge)edge).addConnection(time, trip, costInMinutes);
        }
    }

    private void addDynamicEdge(StopStructure start, StopStructure end){
        adjMap.get(start).putIfAbsent(end, new DynamicEdge(start, end));
    }

    public void addStaticConnection(String startId, String endId){
        StopStructure start = getStopById(startId);
        StopStructure end = getStopById(endId);

        if(!isAdjacent(start, end)){
            adjMap.get(start).putIfAbsent(end, new StaticEdge(start, end));
        }
    }

    private void addStaticEdge(StopStructure start, StopStructure end){
        adjMap.get(start).putIfAbsent(end, new StaticEdge(start, end));
    }

    public Edge getEdge(String startId, String endId){
        return getEdge(getStopById(startId), getStopById(endId));
    }

    public Edge getEdge(StopStructure start, StopStructure end){
        return adjMap.get(start).get(end);
    }

    public HashMap<StopStructure, Edge> getAdjacentStops(StopStructure start){
        adjMap.putIfAbsent(start, new HashMap<>());
        return adjMap.get(start);
    }
}
