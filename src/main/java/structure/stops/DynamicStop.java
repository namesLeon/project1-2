package structure.stops;

import structure.visitor.TravelVisitor;

public class DynamicStop extends StopStructure {

    public DynamicStop(double lat, double longt, String stopID){
        super(lat, longt, stopID);
    }

    @Override
    public void acceptVisitor(TravelVisitor visitor) {

    }
}
