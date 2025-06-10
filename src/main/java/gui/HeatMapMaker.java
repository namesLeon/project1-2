package gui;

import planner.RoutePlanner;
import storage.TransitGraph;
import structs.Stop;

import java.awt.Color;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TransferQueue;

// Builds the grid of SquareArea objects covering a geographic viewport
public class HeatMapMaker {
    private final double topLat;
    private final double leftLon;
    private final double bottomLat;
    private final double rightLon;
    private final double cellSizeMeters;
    private final float  cellOpacity;
    private final int    numBins;
    private final List<SquareArea> grid;
    private final TransitGraph graph;
    private final RoutePlanner planner;

    private final double startLat;
    private final double startLon;
    private final String startTime;

    /**
     * @param topLat       latitude of top edge
     * @param leftLon      longitude of left edge
     * @param bottomLat    latitude of bottom edge
     * @param rightLon     longitude of right edge
     * @param cellSizeMeters side length for each square cell
     * @param cellOpacity  uniform opacity for all cells
     * @param numBins      number of discrete color steps
     */
    public HeatMapMaker(double topLat,
                        double leftLon,
                        double bottomLat,
                        double rightLon,
                        double cellSizeMeters,
                        float cellOpacity,
                        int numBins,
                        TransitGraph graph,
                        RoutePlanner planner,
                        double startLat,
                        double startLon,
                        String startTime
    ) {
        this.topLat        = topLat;
        this.leftLon       = leftLon;
        this.bottomLat     = bottomLat;
        this.rightLon      = rightLon;
        this.cellSizeMeters= cellSizeMeters;
        this.cellOpacity   = cellOpacity;
        this.numBins       = numBins;
        this.grid          = new ArrayList<>();
        this.graph = graph;
        this.planner = planner;
        this.startLat = startLat;
        this.startLon = startLon;
        this.startTime = String.valueOf(startTime);
        buildGrid();
    }

    private void buildGrid() {
        // Approximate conversion: 1 m ≈ 1/111320° latitude
        double degPerMeterLat = 1.0 / 111320.0;
        double latStep = cellSizeMeters * degPerMeterLat;
        // Longitude degrees vary by latitude; use midpoint
        double midLat = (topLat + bottomLat) / 2;
        double degPerMeterLon = degPerMeterLat / Math.cos(Math.toRadians(midLat));
        double lonStep = cellSizeMeters * degPerMeterLon;

        // Compute number of rows/cols
        int rows = (int)Math.ceil((topLat - bottomLat) / latStep);
        int cols = (int)Math.ceil((rightLon - leftLon) / lonStep);

        // Loop from top-left downward and rightward
        int counter = 0;
        for (int r = 0; r < rows; r++) {
            double cellTopLat = topLat - r * latStep;
            for (int c = 0; c < cols; c++) {
                double cellLeftLon = leftLon + c * lonStep;
                counter++;
                // Compute color bin by default distance = 0
                Color fillColor = computeColorBin(60, numBins);
                SquareArea cell = new SquareArea(
                        cellSizeMeters,
                        cellSizeMeters,
                        cellTopLat,
                        cellLeftLon,
                        fillColor,
                        cellOpacity
                );
                cell.setStop(new Stop("Heat"+counter, "Heat"+counter, cellTopLat+0.5*latStep, cellLeftLon+0.5*lonStep));
                graph.addStopToHeatMap(cell.getStop());
                grid.add(cell);
            }
        }
        long startTime = System.currentTimeMillis();
        graph.computeWalkEdgesForHeatMap(cellSizeMeters);
        HashMap<Stop, Integer> travelTimes = planner.computeAllShortestPaths(startLat, startLon, this.startTime);
        long routeTime = (System.currentTimeMillis() - startTime);
        System.out.println("Route calculating : "+routeTime/1000);
        for(SquareArea sq : grid){
            Stop s = sq.getStop();
            int index = (int)(travelTimes.get(s)/60);
            sq.setColor(computeColorBin(index, numBins));
            //System.out.println(s.stopId+" "+index);
        }
        //graph.clearHeatMapStops();
        System.out.println("Created heatmap");
//        for(Map.Entry<Stop, Integer> e : travelTimes.entrySet()){
//            if(e.getKey().stopId.startsWith("Heat")){
//                System.out.println(e.getKey().stopId + " : "+e.getValue());
//            }
//        }
    }

    /**
     * Returns the list of all grid cells, in top-left to bottom-right order.
     */
    public List<SquareArea> getGrid() {
        return grid;
    }

    /**
     * Helper to pick a discrete color between yellow and red for a given bin index
     */
//    private Color computeColorBin(int binIndex, int bins) {
//        float t;
//        if(binIndex < bins) t = (float)binIndex / (bins - 1);
//        else t = 1;
//        int green = (int)(255 * (1 - t));
//        return new java.awt.Color((int)(255 - green), green, 0);
//    }
//    private Color computeColorBin(int binIndex, int bins) {
//        float t = Math.min(1.0f, (float)binIndex / (bins - 1));
//
//        // Blue to red gradient
//        int red = (int)(255 * (1-t));
//        int blue = (int)(255 * t);
//        return new Color(red, 0, blue);
//    }
    public Color computeColorBin(int binIndex, int bins) {
        float t = Math.min(1.0f, (float)binIndex / (bins - 1));

        // Invert t so lower binIndex = warmer colors
        t = 1.0f - t;

        int red, green, blue;

        if (t < 0.5f) {
            // Dark blue to cyan (cold colors for high binIndex)
            float localT = t * 2;
            red = 0;
            green = (int)(255 * localT);
            blue = 255;
        } else {
            // Cyan to yellow to red (warm colors for low binIndex)
            float localT = (t - 0.5f) * 2;
            red = (int)(255 * localT);
            green = 255;
            blue = (int)(255 * (1 - localT));
        }

        return new Color(red, green, blue);
    }
}

