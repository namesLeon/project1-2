package structure.stops;

import structure.TravelVisitor;

public class StaticStop extends StopStructure {

    public StaticStop(double lat, double longt, String stopID){
        super(lat, longt, stopID);
    }

    @Override
    public void acceptVisitor(TravelVisitor visitor) {

    }
}
