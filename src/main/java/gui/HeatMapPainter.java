package gui;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactory;
import java.awt.*;
import java.awt.geom.Point2D;

/**
 * Draws a discrete heatmap grid over the map, with each cell colored based
 * on its distance from a selected position. No borders between cells.
 */
public class HeatMapPainter implements Painter<JXMapViewer> {
    private final double metersPerCell;   // size of each grid cell in meters
    private final float opacity;          // overlay opacity [0..1]
    private final int numBins;            // number of discrete color levels
    private GeoPosition selectedPosition;

    /**
     * @param metersPerCell size of each square in meters
     * @param opacity how transparent the overlay should be
     * @param numBins number of discrete color steps (e.g. 5)
     */
    public HeatMapPainter(double metersPerCell, float opacity, int numBins) {
        this.metersPerCell = metersPerCell;
        this.opacity = opacity;
        this.numBins = numBins;
    }

    /**
     * Set the clicked location; triggers new coloring on repaint
     */
    public void setSelectedPosition(GeoPosition pos) {
        this.selectedPosition = pos;
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int w, int h) {
        if (selectedPosition == null) return;

        Composite oldComp = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));

        TileFactory tf = map.getTileFactory();
        int zoom = map.getZoom();
        Rectangle vp = map.getViewportBounds();

        // determine geo-bounds of viewport
        GeoPosition topLeft     = tf.pixelToGeo(new Point2D.Double(vp.x, vp.y), zoom);
        GeoPosition bottomRight = tf.pixelToGeo(
                new Point2D.Double(vp.x + vp.width, vp.y + vp.height), zoom);

        // convert cell size from meters to degrees
        double degPerMeterLat = 1.0 / 111320.0;
        double latCellDeg     = metersPerCell * degPerMeterLat;
        double degPerMeterLon = degPerMeterLat / Math.cos(Math.toRadians(selectedPosition.getLatitude()));
        double lonCellDeg     = metersPerCell * degPerMeterLon;

        // align grid to world coords
        double minLat = Math.floor(bottomRight.getLatitude() / latCellDeg) * latCellDeg;
        double maxLat = Math.ceil(   topLeft.getLatitude()     / latCellDeg) * latCellDeg;
        double minLon = Math.floor(   topLeft.getLongitude()   / lonCellDeg) * lonCellDeg;
        double maxLon = Math.ceil(   bottomRight.getLongitude() / lonCellDeg) * lonCellDeg;

        // compute max distance for normalization
        GeoPosition[] corners = new GeoPosition[]{
                topLeft,
                new GeoPosition(topLeft.getLatitude(),      bottomRight.getLongitude()),
                bottomRight,
                new GeoPosition(bottomRight.getLatitude(), topLeft.getLongitude())
        };
        double maxD = 0;
        for (GeoPosition gp : corners) {
            maxD = Math.max(maxD, haversine(selectedPosition, gp));
        }

        // draw cells with discrete colors
        for (double lat = minLat; lat < maxLat; lat += latCellDeg) {
            for (double lon = minLon; lon < maxLon; lon += lonCellDeg) {
                // center of cell
                GeoPosition center = new GeoPosition(lat + latCellDeg/2, lon + lonCellDeg/2);
                double d = haversine(selectedPosition, center);
                double norm = Math.min(d / maxD, 1.0);

                // discrete bin index [0..numBins-1]
                int bin = (int) Math.floor(norm * (numBins - 1));
                float t = (float) bin / (numBins - 1);

                // color interpolation: yellow -> red
                int green = (int)(255 * (1 - t));
                Color fill = new Color(255, green, 0);
                g.setColor(fill);

                // compute pixel rectangle
                Point2D pUL = tf.geoToPixel(new GeoPosition(lat + latCellDeg, lon), zoom);
                Point2D pLR = tf.geoToPixel(new GeoPosition(lat, lon + lonCellDeg), zoom);
                int x  = (int)(pUL.getX() - vp.x);
                int y  = (int)(pUL.getY() - vp.y);
                int wPx = (int)(pLR.getX() - pUL.getX()) + 1;  // +1 to avoid gaps
                int hPx = (int)(pLR.getY() - pUL.getY()) + 1;

                g.fillRect(x, y, wPx, hPx);
            }
        }

        g.setComposite(oldComp);
    }

    /** Haversine formula to get distance in meters */
    private static double haversine(GeoPosition a, GeoPosition b) {
        final double R = 6_371_000; // earth radius in m
        double φ1 = Math.toRadians(a.getLatitude());
        double φ2 = Math.toRadians(b.getLatitude());
        double Δφ = Math.toRadians(b.getLatitude() - a.getLatitude());
        double Δλ = Math.toRadians(b.getLongitude() - a.getLongitude());
        double sinΔφ = Math.sin(Δφ/2), sinΔλ = Math.sin(Δλ/2);
        double v = sinΔφ*sinΔφ + Math.cos(φ1)*Math.cos(φ2)*sinΔλ*sinΔλ;
        return R * 2 * Math.atan2(Math.sqrt(v), Math.sqrt(1-v));
    }
}

// ---- Integration in AmsterdamMap.java ----
// 1) import your HeatMapPainter:
//     import your.package.HeatMapPainter;
// 2) define a field near your other painters:
//     private static final HeatMapPainter heatPainter = new HeatMapPainter(200.0, 0.5f, 5);
// 3) when setting overlays (initial, route, reset):
//     mapViewer.setOverlayPainter(
//       CompositePainter.of(markerPainter, heatPainter, routeActive ? routePainter : null)
//     );
// 4) in mouseClicked(...):
//     heatPainter.setSelectedPosition(geo);
//     mapViewer.repaint();
