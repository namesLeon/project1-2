package gui;

import java.awt.Color;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import structs.Stop;

// Represents a single square cell in the heatmap grid
public class SquareArea {
    private final double widthMeters;
    private final double heightMeters;
    private final double originLat;      // latitude of the NW (top-left) corner
    private final double originLon;      // longitude of the NW (top-left) corner
    private final List<Stop> stops;   // all stops within this cell
    private Stop stop;
    private Color color;         // fill color (may encode yellow->red)
    private float opacity;                // transparency [0..1]

    /**
     * @param widthMeters   width of each square (meters)
     * @param heightMeters  height of each square (meters)
     * @param originLat     latitude of the top-left corner of this cell
     * @param originLon     longitude of the top-left corner of this cell
     * @param color         fill color for rendering
     * @param opacity       alpha transparency [0..1]
     */
    public SquareArea(double widthMeters,
                      double heightMeters,
                      double originLat,
                      double originLon,
                      Color color,
                      float opacity) {
        this.widthMeters  = widthMeters;
        this.heightMeters = heightMeters;
        this.originLat    = originLat;
        this.originLon    = originLon;
        this.stops        = new ArrayList<>();
        this.color        = color;
        this.opacity      = opacity;
    }

    /**
     * Returns the list of stops in this area.
     */
    public List<Stop> getStops() {
        return Collections.unmodifiableList(stops);
    }

    /**
     * Adds a stop to this area if it's not already present.
     */
    public void addStop(Stop stop) {
        if (!stops.contains(stop)) {
            stops.add(stop);
        }
    }

    public void setStop(Stop stop){
        this.stop = stop;
    }

    public Stop getStop(){
        return this.stop;
    }

    public void setOpacity(float opacity){
        this.opacity = opacity;
    }

    public void setColor(Color color){
        this.color = color;
    }

    /**
     * Populates this cell's stops list by filtering a master stop list.
     * Uses a simple lat/lon bounding box approximated from meters.
     */
    public void populateStops(java.util.List<Stop> allStops) {
        stops.clear();
        // convert meters to degrees
        double degPerMeterLat = 1.0 / 111320.0;
        double latDeg = heightMeters * degPerMeterLat;
        double degPerMeterLon = degPerMeterLat / Math.cos(Math.toRadians(originLat));
        double lonDeg = widthMeters * degPerMeterLon;

        double maxLat = originLat;
        double minLat = originLat - latDeg;
        double minLon = originLon;
        double maxLon = originLon + lonDeg;

        for (Stop stop : allStops) {
            double lat = stop.lat;
            double lon = stop.lon;
            if (lat <= maxLat && lat >= minLat && lon >= minLon && lon <= maxLon) {
                stops.add(stop);
            }
        }
    }


    // Getters for geometry and rendering
    public double getWidthMeters()  { return widthMeters; }
    public double getHeightMeters() { return heightMeters; }
    public double getOriginLat()    { return originLat; }
    public double getOriginLon()    { return originLon; }
    public Color getColor(){ return color; }
    public float getOpacity()       { return opacity; }

}
