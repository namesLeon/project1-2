package algorithms;

import gtfsparsingzip.data.Trip;
import structure.Graph;
import structure.GraphUtilities;
import structure.stops.DynamicStop;
import structure.stops.StaticStop;
import structure.stops.StopStructure;

import java.time.LocalTime;
import java.util.ArrayList;

public class DijkstraAlgorithm extends BaseAlgorithm{

    @Override
    public int getCostTotal(int costTravel, int heuristicCost) {
        return costTravel;
    }

    public static void main(String[] args) {
        StopStructure start = new StaticStop(52.385494,4.876722, "1");
        StopStructure stop2 = new StaticStop(52.384984,4.883662, "2");
        StopStructure stop3 = new StaticStop(52.378306,4.879031, "3");
        StopStructure stop4 = new StaticStop(52.378337,4.883301, "4");
        StopStructure stop5 = new StaticStop(52.379475,4.8797641, "5");
        StopStructure goal = new StaticStop(52.3783069,4.8790319, "6");
        StopStructure stop7 = new StaticStop(52.378587,4.8754411, "7");
        // StopStructure end = new StaticStop(48, 10, "7");

        //System.out.println(GraphUtilities.calculateStaticCostMinutes(stop3, stop4, 5));
//        System.out.println(GraphUtilities.calculateStaticCostMinutes(stop2, stop3, 5));
//        System.out.println(GraphUtilities.calculateStaticCostMinutes(stop3, stop4, 5));
//        System.out.println(GraphUtilities.calculateStaticCostMinutes(stop4, stop5, 5));
//        System.out.println(GraphUtilities.calculateStaticCostMinutes(stop5, goal, 5));
        // System.out.println(GraphUtilities.distanceBetweenPoints(start, end));
        Graph graph = new Graph();
        graph.addStop(start);
        graph.addStop(stop2);
        graph.addStop(stop3);
        graph.addStop(stop4);
        graph.addStop(stop5);
        graph.addStop(goal);
        graph.addDynamicConnection(start, stop3, LocalTime.of(11,22), new Trip("a", "a", "a", "a"), 3);
        graph.addDynamicConnection(start, stop5, LocalTime.of(11, 23), new Trip("a", "a", "a", "a"), 1);
        BaseAlgorithm dijkstraAlgorithm = new DijkstraAlgorithm();
        ArrayList<StopStructure> list = dijkstraAlgorithm.findPath(graph, start, stop5, LocalTime.of(11,20));
        System.out.println();
//        for(StopStructure s : list){
//            System.out.println(s.getStopId());
//        }
//
//        System.out.println("\n times:");
//        //System.out.println(GraphUtilities.calculateStaticCostMinutes(start, stop5, 5));
//        //System.out.println(GraphUtilities.calculateStaticCostMinutes(stop3, stop5, 5));
//        System.out.println("From 1 to 3 : "+graph.getEdge(start, stop3).getCost(LocalTime.of(11,20)));
//        System.out.println("From 1 to 5 : "+graph.getEdge(start, stop3).getCost(LocalTime.of(11,20)));

//        graph.addDynamicConnection(start, stop2, );
//

    }
}
