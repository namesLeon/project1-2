import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.input.CenterMapListener;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.viewer.TileFactory;
import org.jxmapviewer.viewer.TileFactoryInfo;
import org.jxmapviewer.cache.FileBasedLocalCache;

import javax.swing.event.MouseInputListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;

public class AmsterdamMap {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JXMapViewer mapViewer = new JXMapViewer();
            try {
                URL tilesURL = AmsterdamMap.class.getResource("/tiles.zip");
                String jarURL = "jar:" + tilesURL.toURI() + "!";


                TileFactoryInfo info = new LocalTileFactoryInfo(jarURL);
                TileFactory tileFactory = new DefaultTileFactory(info);

                mapViewer.setTileFactory(tileFactory);

                // Set center to Amsterdam
                GeoPosition amsterdam = new GeoPosition(52.3702, 4.8952); // example
                mapViewer.addPropertyChangeListener("centerPosition", evt -> {
                    GeoPosition center = mapViewer.getCenterPosition();

                    // Clamp to bounding box
                    double lat = Math.max(52.35, Math.min(52.42, center.getLatitude()));
                    double lon = Math.max(4.9, Math.min(4.0, center.getLongitude()));

                    if (lat != center.getLatitude() || lon != center.getLongitude()) {
                        mapViewer.setCenterPosition(new GeoPosition(lat, lon));
                    }
                });
                mapViewer.setZoom(3);
                mapViewer.setAddressLocation(amsterdam);

                // Approximate polyline of Vinkenstraat
                List<GeoPosition> route = new ArrayList<>();
                route.add(new GeoPosition(52.38305, 4.88555)); // Start of Vinkenstraat
                route.add(new GeoPosition(52.38335, 4.88495));
                route.add(new GeoPosition(52.38365, 4.88435));
                route.add(new GeoPosition(52.38395, 4.88375)); //Middle of Vinkenstraat

                // Add painter to draw the route
                Painter<JXMapViewer> routePainter = new RoutePainter(route);

                // Add to map
                mapViewer.setOverlayPainter(routePainter);

                // Add pan and zoom interaction
                MouseInputListener mia = new PanMouseInputListener(mapViewer);
                mapViewer.addMouseListener(mia);
                mapViewer.addMouseMotionListener(mia);

                // Center map on click
                mapViewer.addMouseListener(new CenterMapListener(mapViewer));

                // Zoom with mouse wheel
                mapViewer.addMouseWheelListener(new MouseWheelListener() {
                    @Override
                    public void mouseWheelMoved(MouseWheelEvent e) {
                        if (e.getWheelRotation() < 0) {
                            mapViewer.setZoom(mapViewer.getZoom() - 1); // zoom in
                        } else {
                            mapViewer.setZoom(mapViewer.getZoom() + 1); // zoom out
                        }
                    }
                });

                // UI
                JButton zoomIn = new JButton("+");
                zoomIn.addActionListener(e -> mapViewer.setZoom(mapViewer.getZoom() - 1));

                JButton zoomOut = new JButton("-");
                zoomOut.addActionListener(e -> mapViewer.setZoom(mapViewer.getZoom() + 1));

                JPanel controls = new JPanel();
                controls.add(zoomIn);
                controls.add(zoomOut);

                JFrame frame = new JFrame("Map - Vinkenstraat Route");
                frame.setLayout(new BorderLayout());
                frame.add(mapViewer);
                frame.getContentPane().add(controls, BorderLayout.NORTH);
                frame.setSize(10000, 10000);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);

            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

}
