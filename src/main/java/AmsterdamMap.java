import DB.DBManager;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.viewer.TileFactory;
import org.jxmapviewer.viewer.TileFactoryInfo;

import clinterface.GTFSLoader;
import planner.RoutePlanner;
import storage.TransitGraph;
import structs.JourneyStep;

import javax.swing.event.MouseInputListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.FileNotFoundException;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

public class AmsterdamMap {
    private static List<GeoPosition> markers = new ArrayList<>();
    private static RoutePlanner planner;
    private static TransitGraph graph;
    private static GTFSLoader loader;
    private static DBManager dbManager;
    private static JLabel startCoordLabel;
    private static JLabel endCoordLabel;
    private static JXMapViewer mapViewer;
    private static Painter<JXMapViewer> markerPainter;
    private static Painter<JXMapViewer> routePainter;
    private static JButton startRouteButton;
    private static boolean routeActive = false;
    private static JTextField timeInputField;
    private static JLabel startTimeLabel;
    private static JLabel endTimeLabel;
    private static ArrayList<Integer> stepDurations;


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            mapViewer = new JXMapViewer();
            try {
                URL tilesURL = AmsterdamMap.class.getResource("/tiles.zip");
                String jarURL = "jar:" + tilesURL.toURI() + "!";

                TileFactoryInfo info = new LocalTileFactoryInfo(jarURL);
                TileFactory tileFactory = new DefaultTileFactory(info);

                mapViewer.setTileFactory(tileFactory);
                graph = new TransitGraph();
                loader = new GTFSLoader(graph);
                dbManager = new DBManager();
                try {
                    String path = "data/amsterdam_gtfs_final.zip"; // Default path for GTFS data
                    loader.loadFromZip(Path.of(path));
                    planner = new RoutePlanner(graph);
                    //dbManager.initializeDataBase(path);// Takes like 30 seconds to load the database
                } catch (FileNotFoundException e) {
                    //fatal: GTFS data not found
                    System.err.println("File not found");
                } catch (Exception e) {
                    //fatal: other load error
                    System.err.print(e.getMessage());
                }

        

                // Set center to Amsterdam
                GeoPosition amsterdam = new GeoPosition(52.3702, 4.8952);
                mapViewer.addPropertyChangeListener("centerPosition", evt -> {
                    GeoPosition center = mapViewer.getCenterPosition();
                    double lat = Math.max(52.35, Math.min(52.42, center.getLatitude()));
                    double lon = Math.max(4.0, Math.min(4.9, center.getLongitude()));
                    if (lat != center.getLatitude() || lon != center.getLongitude()) {
                        mapViewer.setCenterPosition(new GeoPosition(lat, lon));
                    }
                });
                mapViewer.setZoom(3);
                mapViewer.setAddressLocation(amsterdam);

                // // Approximate polyline of Vinkenstraat
                // List<GeoPosition> testRoute = new ArrayList<>();
                // testRoute.add(new GeoPosition(52.38305, 4.88555));
                // testRoute.add(new GeoPosition(52.38335, 4.88495));
                // testRoute.add(new GeoPosition(52.38365, 4.88435));
                // testRoute.add(new GeoPosition(52.38395, 4.88375));

               

                // Add mouse listeners
                MouseInputListener mia = new PanMouseInputListener(mapViewer);
                mapViewer.addMouseListener(mia);
                mapViewer.addMouseMotionListener(mia);

                // Add click listener for adding markers
                mapViewer.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getButton() == MouseEvent.BUTTON1) { // Left click
                            Point2D point = e.getPoint();
                            GeoPosition geo = mapViewer.convertPointToGeoPosition(point);
                            
                            if (markers.size() >= 2) {
                                markers.remove(0);
                            }
                            markers.add(geo);

                            if (routeActive) {
                                resetRoute();
                                resetTimeLabels();
                            }
                            
                            updateCoordinateLabels();
                            mapViewer.repaint();
                        }
                    }
                });

                // UI
                JButton zoomIn = new JButton("+");
                zoomIn.addActionListener(e -> mapViewer.setZoom(mapViewer.getZoom() - 1));

                JButton zoomOut = new JButton("-");
                zoomOut.addActionListener(e -> mapViewer.setZoom(mapViewer.getZoom() + 1));

                JButton resetButton = new JButton("Reset Points");
                resetButton.addActionListener(e -> {
                    markers.clear();
                    resetRoute();
                    resetTimeLabels();

                    updateCoordinateLabels();
                    mapViewer.repaint();
                });

                startRouteButton = new JButton("Start Route");
                startRouteButton.setEnabled(false);
                startRouteButton.addActionListener(e -> {
                   
                    String time = getTimeFromInput();
                    startRoute(time);
                    updateTimeLabels();
                });

                // Time input field
            timeInputField = new JTextField(10);
            timeInputField.setToolTipText("Enter time (HH:mm) or leave empty for current time");
            timeInputField.setText("Right now");
            timeInputField.setForeground(Color.GRAY);
            
            timeInputField.addFocusListener(new java.awt.event.FocusAdapter() {
                public void focusGained(java.awt.event.FocusEvent evt) {
                    if (timeInputField.getText().equals("Right now")) {
                        timeInputField.setText("");
                        timeInputField.setForeground(Color.BLACK);
                    }
                }
                
                public void focusLost(java.awt.event.FocusEvent evt) {
                    if (timeInputField.getText().isEmpty()) {
                        timeInputField.setText("Right now");
                        timeInputField.setForeground(Color.GRAY);
                    }
                }
            });

                JPanel controls = new JPanel();
                controls.add(zoomIn);
                controls.add(zoomOut);
                controls.add(resetButton);
                controls.add(startRouteButton);
                controls.add(timeInputField);

                markerPainter = new MarkerPainter(markers);
                mapViewer.setOverlayPainter(markerPainter);

                JPanel bottomPanel = new JPanel();
                bottomPanel.setBorder(BorderFactory.createTitledBorder("Trip Info"));
                bottomPanel.setLayout(new GridLayout(2, 3, 10, 5));

                // Labels
                startCoordLabel = new JLabel("Start Coord: (--, --)");
                endCoordLabel = new JLabel("End Coord: (--, --)");
                startTimeLabel = new JLabel("Start Time: --:--");
                endTimeLabel = new JLabel("End Time: --:--");
                JLabel stopsLabel = new JLabel("Stops: 0");
                JLabel transfersLabel = new JLabel("Transfers: 0");

                bottomPanel.add(startTimeLabel);
                bottomPanel.add(endTimeLabel);
                bottomPanel.add(stopsLabel);
                bottomPanel.add(startCoordLabel);
                bottomPanel.add(endCoordLabel);
                bottomPanel.add(transfersLabel);

                JFrame frame = new JFrame("Map - Amsterdam");
                frame.setLayout(new BorderLayout());
                frame.add(mapViewer);
                frame.getContentPane().add(controls, BorderLayout.NORTH);
                frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                frame.setResizable(true);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private static void updateCoordinateLabels() {
        if (markers.size() > 0) {
            GeoPosition start = markers.get(0);
            startCoordLabel.setText(String.format("Start Coord: (%.5f, %.5f)", start.getLatitude(), start.getLongitude()));
        } else {
            startCoordLabel.setText("Start Coord: (--, --)");
        }

        if (markers.size() > 1) {
            GeoPosition end = markers.get(1);
            endCoordLabel.setText(String.format("End Coord: (%.5f, %.5f)", end.getLatitude(), end.getLongitude()));
            startRouteButton.setEnabled(true);
        } else {
            endCoordLabel.setText("End Coord: (--, --)");
            startRouteButton.setEnabled(false);
        }
    }

    private static void startRoute(String time) {
        if (markers.size() == 2) {
            List<String> modes = new ArrayList<>();
            List<GeoPosition> route = new ArrayList<>();
            GeoPosition start = markers.get(0);
            GeoPosition end = markers.get(1);
            route.add(start);
            List<JourneyStep> journey = planner.computeRoute(start.getLatitude(), start.getLongitude(), end.getLatitude(), end.getLongitude(), time);
            stepDurations = new ArrayList<>();
            for(JourneyStep step : journey) {
                stepDurations.add(step.durationMin);
                GeoPosition stepPos = new GeoPosition(step.to[0], step.to[1]);
                modes.add(step.mode);
                route.add(stepPos);
            }
            routePainter = new RoutePainter(route, modes);
            mapViewer.setOverlayPainter(CompositePainter.of(markerPainter, routePainter));
            mapViewer.repaint();
            routeActive = true;
        }
    }
    private static void resetRoute() {
        mapViewer.setOverlayPainter(markerPainter);
        routeActive = false;
        startRouteButton.setEnabled(markers.size() == 2);
        mapViewer.repaint();
    }


    private static String getTimeFromInput() {
        String timeText = timeInputField.getText();
        if (timeText.isEmpty() || timeText.equals("Right now")) {
            return new SimpleDateFormat("HH:mm").format(new Date());
        }
        return timeText;
    }
     private static void updateTimeLabels() {
        String timeText = timeInputField.getText();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        Date now = new Date();
        
        try {
            // Parse input time or use current time if empty or "From now"
            Date startTime = (timeText.isEmpty() || timeText.equals("From now"))
                ? now 
                : timeFormat.parse(timeText);
            
            // Calculate end time (start time + 5 minutes)
            Date endTime = new Date(startTime.getTime() + stepDurations.stream()
                    .reduce(0, Integer::sum) * 60 * 1000);
            
            startTimeLabel.setText("Start Time: " + timeFormat.format(startTime));
            endTimeLabel.setText("End Time: " + timeFormat.format(endTime));
        } catch (Exception e) {
            // If parsing fails, use current time
            startTimeLabel.setText("Start Time: " + timeFormat.format(now));
            endTimeLabel.setText("End Time: " + timeFormat.format(new Date(now.getTime() + stepDurations.stream()
  .reduce(0, Integer::sum) * 60 * 1000))); // Sum of durations in milliseconds
        }
    }

    private static void resetTimeLabels() {
        startTimeLabel.setText("Start Time: --:--");
        endTimeLabel.setText("End Time: --:--");
        timeInputField.setText("Right now");
        timeInputField.setForeground(Color.GRAY);
    }
}

class CompositePainter<T> implements Painter<T> {
    private final List<Painter<T>> painters;

    @SafeVarargs
    public static <T> CompositePainter<T> of(Painter<T>... painters) {
        return new CompositePainter<>(List.of(painters));
    }

    public CompositePainter(List<Painter<T>> painters) {
        this.painters = new ArrayList<>(painters);
    }

    @Override
    public void paint(Graphics2D g, T t, int width, int height) {
        for (Painter<T> painter : painters) {
            painter.paint(g, t, width, height);
        }
    }
}