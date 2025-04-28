package structure.edges;

import structure.TravelVisitor;
import structure.stops.StopStructure;

public class StaticEdge extends Edge{

    public StaticEdge(StopStructure start, StopStructure end){
        super(start, end);
    }

    @Override
    public void acceptVisitor(TravelVisitor visitor) {

    }
}
