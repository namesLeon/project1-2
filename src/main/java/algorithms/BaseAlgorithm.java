package algorithms;

import structure.*;
import structure.edges.Edge;
import structure.stops.StopStructure;

import java.time.LocalTime;
import java.util.*;

public abstract class BaseAlgorithm {
//    private Graph graph;
//    private StopStructure start;
//    private StopStructure end;
    private PriorityQueue<PQEntry> pq;
    private ArrivalTable arrivalTable;
    private HashSet<StopStructure> visited;


    public ArrayList<StopStructure> findPath(Graph graph, StopStructure start, StopStructure goal, LocalTime startTime){
        pq = new PriorityQueue<>();
        arrivalTable = new ArrivalTable();
        visited = new HashSet<>();
        LocalTime time = startTime;
        int timeDiffMinutes = 0;
        StopStructure current = start;
        for(StopStructure stop : graph.getStopList()){
            arrivalTable.setTimeDiff(stop, current, Integer.MAX_VALUE);
        }
        arrivalTable.setTimeDiff(current, current, timeDiffMinutes);
        for(Map.Entry<StopStructure, Edge> entry : graph.getAdjacentStops(start).entrySet()){
            relaxEdges(entry.getKey(), time);
        }

        while(!pq.isEmpty()){
            PQEntry entry = pq.poll();
            current = entry.to;
            timeDiffMinutes = entry.costTravel;
            time = startTime.plusMinutes(entry.costTravel);
            if(visited.contains(current) || arrivalTable.getCostForStop(current) > timeDiffMinutes) continue;
            relaxEdges(current, time);
            visited.add(current);
            if(current.equals(goal)) break;
        }

    }


    public abstract void preprocessVertices();
    public abstract void relaxEdges(StopStructure stop, LocalTime time);

    public abstract static class PQEntry implements Comparable<PQEntry>{
        StopStructure to;
        StopStructure from;
        int costTravel;
        int costTotal;

        @Override
        public int compareTo(PQEntry o) {
            return (this.costTotal - o.costTotal);
        }
    }
}
