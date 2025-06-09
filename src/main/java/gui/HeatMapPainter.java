package gui;

// Paints the grid of SquareArea objects onto the JXMapViewer
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactory;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;

public class HeatMapPainter implements Painter<JXMapViewer> {
    private final List<SquareArea> grid;
    private final TileFactory tileFactory;

    /**
     * @param grid       list of SquareArea covering the viewport
     * @param tileFactory map's tile factory for geo<->pixel conversions
     */
    public HeatMapPainter(List<SquareArea> grid,
                          TileFactory tileFactory) {
        this.grid        = grid;
        this.tileFactory = tileFactory;
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int width, int height) {
        Rectangle vp = map.getViewportBounds();
        int zoom = map.getZoom();

        for (SquareArea cell : grid) {
            // Determine pixel coords for this cell's top-left and bottom-right
            GeoPosition ulGeo = new GeoPosition(cell.getOriginLat(), cell.getOriginLon());
            GeoPosition lrGeo = new GeoPosition(
                    cell.getOriginLat() - (cell.getHeightMeters() / 111320.0),
                    cell.getOriginLon() + (cell.getWidthMeters()  /
                            (111320.0 * Math.cos(Math.toRadians(cell.getOriginLat()))))
            );
            Point2D pUL = tileFactory.geoToPixel(ulGeo, zoom);
            Point2D pLR = tileFactory.geoToPixel(lrGeo, zoom);

            int x  = (int)(pUL.getX() - vp.x);
            int y  = (int)(pUL.getY() - vp.y);
            int wPx = (int)(pLR.getX() - pUL.getX());
            int hPx = (int)(pLR.getY() - pUL.getY());

            // Set opacity
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, cell.getOpacity()));

            // Fill
            g.setColor(cell.getColor());
            g.fillRect(x, y, wPx, hPx);

            // Restore composite
            g.setComposite(old);
        }
    }
}
