package structure.stops;

import structure.TravelVisitor;

public abstract class StopStructure {
    protected double latitude;
    protected double longitude;
    protected String stopID;
    private int distanceToGoal;
    private int inDegree;
    private int outDegree;

    public StopStructure(double lat, double longt, String stopID){
        this.latitude  = lat;
        this.longitude = longt;
        this.stopID = stopID;
        this.inDegree = 0;
        this.outDegree = 0;
    }

    public double getLatitude(){ return latitude;}
    public double getLongitude(){ return longitude;}
    public String getStopId(){ return stopID;}

    public void setDistanceToGoal(int distanceToGoal){
        this.distanceToGoal = distanceToGoal;
    }

    public int getDistanceToGoal() {
        return distanceToGoal;
    }

    @Override
    public boolean equals(Object o){
        if(o == null) return false;
        if(!(o instanceof StopStructure)) return false;
        StopStructure other = (StopStructure) o;
        return stopID.equals(other.getStopId());
    }
    public void incrementInDegree(){inDegree++;}
    public void incrementOutDegree(){outDegree++;}
    public boolean isUnused(){ return inDegree==0 & outDegree==0;}
    public abstract void acceptVisitor(TravelVisitor visitor);
}
