package gui;

import java.awt.Color;
import java.util.List;
import java.util.ArrayList;

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
                        int numBins) {
        this.topLat        = topLat;
        this.leftLon       = leftLon;
        this.bottomLat     = bottomLat;
        this.rightLon      = rightLon;
        this.cellSizeMeters= cellSizeMeters;
        this.cellOpacity   = cellOpacity;
        this.numBins       = numBins;
        this.grid          = new ArrayList<>();
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
        for (int r = 0; r < rows; r++) {
            double cellTopLat = topLat - r * latStep;
            for (int c = 0; c < cols; c++) {
                double cellLeftLon = leftLon + c * lonStep;
                // Compute color bin by default distance = 0
                Color fillColor = computeColorBin(0, numBins);
                SquareArea cell = new SquareArea(
                        cellSizeMeters,
                        cellSizeMeters,
                        cellTopLat,
                        cellLeftLon,
                        fillColor,
                        cellOpacity
                );
                grid.add(cell);
            }
        }
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
    private Color computeColorBin(int binIndex, int bins) {
        float t = (float)binIndex / (bins - 1);
        int green = (int)(255 * (1 - t));
        return new java.awt.Color(255, green, 0);
    }
}

