import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.painter.Painter;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;


class RoutePainter implements Painter<JXMapViewer> {
    private final List<GeoPosition> track;
    private final List<String> modes;
    private Color color = new Color(221, 255, 0, 255); // default yellow color

    public RoutePainter(List<GeoPosition> track, List<String> modes) {
        this.track = track;
        this.modes = modes;
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int width, int height) {
        g = (Graphics2D) g.create();

        // Convert geo-coordinates to pixel coordinates
        List<Point2D.Double> trackPoints = track.stream()
                .map(pos -> map.getTileFactory().geoToPixel(pos, map.getZoom()))
                .map(p -> new Point2D.Double(p.getX() - map.getViewportBounds().getX(), p.getY() - map.getViewportBounds().getY()))
                .toList();

        g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


        for (int i = 0; i < trackPoints.size() - 1; i++) {
            Point2D pt1 = trackPoints.get(i);
            Point2D pt2 = trackPoints.get(i + 1);
            Color currentColor = getColorByMode(modes.get(i));
            g.setColor(currentColor);
            g.drawLine((int) pt1.getX(), (int) pt1.getY(), (int) pt2.getX(), (int) pt2.getY());
        }

        g.dispose();
    }

    private Color getColorByMode(String mode) {
        switch (mode) {
            case "walk":
                return Color.MAGENTA;
            case "ride":
                return Color.GREEN;
            default:
                return color; // default color
        }
    }
}