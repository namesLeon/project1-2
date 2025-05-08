

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.painter.Painter;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;

class RoutePainter implements Painter<JXMapViewer> {
    private final List<GeoPosition> track;

    public RoutePainter(List<GeoPosition> track) {
        this.track = track;
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int width, int height) {
        g = (Graphics2D) g.create();

        // Convert geo-coordinates to pixel coordinates
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(0, 0, 255, 128)); // semi-transparent blue
        g.setStroke(new BasicStroke(6)); // road-like thickness

        Point2D prev = null;
        for (GeoPosition gp : track) {
            Point2D pt = map.getTileFactory().geoToPixel(gp, map.getZoom());
            if (prev != null) {
                g.drawLine((int) prev.getX(), (int) prev.getY(), (int) pt.getX(), (int) pt.getY());
            }
            prev = pt;
        }

        g.dispose();
    }
}
