import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.painter.Painter;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.ImageIcon;

class MarkerPainter implements Painter<JXMapViewer> {
    private final List<GeoPosition> markers;
    private final ImageIcon pinIcon;

    public MarkerPainter(List<GeoPosition> markers) {
        this.markers = markers;
        // Create a blue pin icon
        this.pinIcon = createBluePinIcon();
    }

    private ImageIcon createBluePinIcon() {
        // Create a simple blue pin icon
        BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        
        // Draw pin shape
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(221, 255, 0, 255)); // Same color as route
        g2.fillOval(4, 4, 16, 16); // Circle part
        g2.fillPolygon(new int[] {12, 8, 16}, new int[] {20, 24, 24}, 3); // Pointy part
        
        // Add white dot in center
        g2.setColor(Color.WHITE);
        g2.fillOval(8, 8, 8, 8);
        
        g2.dispose();
        return new ImageIcon(img);
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int width, int height) {
        g = (Graphics2D) g.create();

        for (GeoPosition pos : markers) {
            Point2D point = map.getTileFactory().geoToPixel(pos, map.getZoom());
            Point2D translatedPoint = new Point2D.Double(
                point.getX() - map.getViewportBounds().getX(),
                point.getY() - map.getViewportBounds().getY()
            );

            // Draw the pin icon centered at the point
            Image img = pinIcon.getImage();
            int x = (int) translatedPoint.getX() - pinIcon.getIconWidth()/2;
            int y = (int) translatedPoint.getY() - pinIcon.getIconHeight();
            g.drawImage(img, x, y, null);
        }

        g.dispose();
    }
}