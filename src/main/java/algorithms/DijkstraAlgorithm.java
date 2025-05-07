package algorithms;

import structure.Graph;
import structure.stops.DynamicStop;
import structure.stops.StaticStop;
import structure.stops.StopStructure;

public class DijkstraAlgorithm extends BaseAlgorithm{

    @Override
    public int getCostTotal(int costTravel, int heuristicCost) {
        return costTravel;
    }

    public static void main(String[] args) {
        StopStructure start = new StaticStop(50.849388, 5.677847, "1");
        StopStructure stop2 = new StaticStop(50.844603, 5.681378, "2");
        StopStructure stop3 = new StaticStop(50.842103, 5.689184, "3");
        StopStructure stop4 = new StaticStop(50.840091, 5.677643, "4");
        StopStructure stop5 = new StaticStop(50.840506, 5.686032, "5");
        StopStructure goal = new StaticStop(50.833993, 5.687277, "6");

        Graph graph = new Graph();
        graph.addStop(start);
        graph.addStop(stop2);
        graph.addStop(stop3);
        graph.addStop(stop4);
        graph.addStop(stop5);
        graph.addStop(goal);

        graph.addDynamicConnection(start, stop2, );


    }
}
