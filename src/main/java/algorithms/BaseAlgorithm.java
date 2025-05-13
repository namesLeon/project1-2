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
        graph.connectNeighbouringVertices(30, 5);
        graph.setHeuristic(goal);
        pq = new PriorityQueue<>();
        arrivalTable = new ArrivalTable();
        visited = new HashSet<>();
        LocalTime time = startTime;
        int costTravel = 0;
        StopStructure current = start;
        for(StopStructure stop : graph.getStopList()){
            arrivalTable.setTimeDiff(stop, current, Integer.MAX_VALUE);
        }
        arrivalTable.setTimeDiff(current, current, costTravel);
        relaxEdges(graph, current, time, visited);
        //arrivalTable.printCosts();

        while(!pq.isEmpty()){
            PQEntry entry = pq.poll();
            current = entry.currentStop;
            costTravel = entry.costTravel;
            //System.out.println("current stop : "+current.getStopId());
            time = startTime.plusMinutes(entry.costTravel);
            if(visited.contains(current) || arrivalTable.getCostForStop(current) < costTravel) continue;
            arrivalTable.setTimeDiff(current, entry.fromStop, costTravel);
            relaxEdges(graph, current, time, visited);
            visited.add(current);
            //System.out.println();
            //arrivalTable.printCosts();
            //System.out.println(current.getStopId());
            //if(current.equals(goal)) break;
        }
        Stack<StopStructure> stopsStack = new Stack<>();
        current = goal;
        while(!current.equals(start)){
            stopsStack.add(current);
            current = arrivalTable.getFromStop(current);
        }
        stopsStack.add(current);
        ArrayList<StopStructure> stopsList = new ArrayList<>();
        while(!stopsStack.isEmpty()){
            stopsList.add(stopsStack.pop());
        }
        return stopsList;
    }


//    public void preprocessVertices(Graph graph, StopStructure start, StopStructure goal){
//        graph.addStartNode(start);
//    };
    public void relaxEdges(Graph graph, StopStructure stop, LocalTime time, HashSet<StopStructure> visited){
        StopStructure s;
        Edge e;
        int costTravel;
        for(Map.Entry<StopStructure, Edge> entry : graph.getAdjacentStops(stop).entrySet()){
            s = entry.getKey();
            //if(visited.contains(s)) continue;
            e = entry.getValue();
            //System.out.println("Arrival table cost for previous stop : "+arrivalTable.getCostForStop(stop));
            //System.out.println("Arrival table cost for this stop : "+arrivalTable.getCostForStop(s));
            //System.out.println("Cost of edge travel : "+e.getCost(time));
            costTravel = arrivalTable.getCostForStop(stop) + e.getCost(time);
            //System.out.println(s.getStopId() + " : "+costTravel);
            if(arrivalTable.getCostForStop(s) <= costTravel) continue;
            pq.add(new PQEntry(s, stop, costTravel, getCostTotal(costTravel, s.getDistanceToGoal())));
            //System.out.println("for "+s.getStopId() +" added pq entry : "+costTravel);

        }
    };

    public abstract int getCostTotal(int costTravel, int heuristicCost);

    public static class PQEntry implements Comparable<PQEntry>{
        StopStructure currentStop;
        StopStructure fromStop;
        int costTravel;
        int costTotal;

        public PQEntry(StopStructure currentStop, StopStructure fromStop, int costTravel, int costTotal){
            this.currentStop = currentStop;
            this.fromStop = fromStop;
            this.costTravel = costTravel;
            this.costTotal = costTotal;
        }

        @Override
        public int compareTo(PQEntry o) {
            return (this.costTotal - o.costTotal);
        }
    }
}
