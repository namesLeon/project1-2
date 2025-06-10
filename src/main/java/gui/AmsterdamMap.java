import DB.DBManager;
import gui.*;
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
import structs.Stop;

import javax.swing.event.MouseInputListener;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.FileNotFoundException;

import javax.swing.*;
import java.awt.*;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.PlatformLoggingMXBean;
import java.net.URL;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

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
    private static JButton startHeatmapButton;
    private static boolean routeActive = false;
    private static JTextField timeInputField;
    private static JLabel startTimeLabel;
    private static JLabel endTimeLabel;
    private static ArrayList<Integer> stepDurations;

    private static final double CELL_SIZE_METERS = 200;
    private static final float  CELL_OPACITY     = 0.7f;
    private static final int    NUM_BINS         = 60;
    private static HeatMapMaker heatMaker;
    private static HeatMapPainter heatPainter;

    private static final double AMST_MIN_LAT = 52.2430;
    private static final double AMST_MAX_LAT = 52.4311;
    private static final double AMST_MIN_LON = 4.7283;
    private static final double AMST_MAX_LON = 5.0616;



    static Map<String, Float> stopTimeDiffs;
    private static List<Stop> deletedStopsList = new ArrayList<>();



    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            mapViewer = new JXMapViewer();
            try {
                stopTimeDiffs = loadStopTimeDiffs("data/stop_time_diff.txt");

                URL tilesURL = AmsterdamMap.class.getResource("/tiles.zip");
                String jarURL = "jar:" + tilesURL.toURI() + "!";

                TileFactoryInfo info = new LocalTileFactoryInfo(jarURL);
                TileFactory tileFactory = new DefaultTileFactory(info);

                mapViewer.setTileFactory(tileFactory);
                graph = new TransitGraph();
                loader = new GTFSLoader(graph);
                dbManager = new DBManager();
                try {
                    String path = "data/amsterdam_gtfs.zip"; // Default path for GTFS data
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

                        else if (e.getButton() == MouseEvent.BUTTON3) {
                            // 1. Get click location in geo-coordinates
                            Point2D pt = e.getPoint();
                            GeoPosition clickGeo = mapViewer.convertPointToGeoPosition(pt);

                            // 2. Find nearest stop in graph.stops
                            Stop nearest = null;
                            double minSqDist = Double.MAX_VALUE;
                            for (Stop s : graph.stops.values()) {
                                double dLat = s.lat - clickGeo.getLatitude();
                                double dLon = s.lon - clickGeo.getLongitude();
                                double sqDist = dLat*dLat + dLon*dLon;
                                if (sqDist < minSqDist) {
                                    minSqDist = sqDist;
                                    nearest = s;
                                }
                            }

                            // 3. If within threshold (≈50 m), delete it
                            double thresholdDeg = 0.0005;  // ≈50 m in degrees
                            if (nearest != null && minSqDist <= thresholdDeg*thresholdDeg) {
                                if(!deletedStopsList.contains(nearest)) {
                                    deletedStopsList.add(nearest);
                                    stopTimeDiffs.remove(nearest.stopId);
                                    System.out.println("Removed stop: " + nearest.stopName);
                                }
                                mapViewer.repaint();
                            }
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

                JButton resetDeletedStopsButton = new JButton("Reset Deleted Stops");
                resetDeletedStopsButton.addActionListener(e -> {

                    // 1) Read all stop‐time diffs from file into a Map
                    Map<String,Float> diffMap = loadStopTimeDiffs("data/stop_time_diff.txt");

                    // 1) re-insert every Stop we previously removed
                    for (Stop s : deletedStopsList) {

                        Float diff = diffMap.get(s.stopId);
                        stopTimeDiffs.put(s.stopId, diff);
                    }
                    // 2) clear the “trash bin” so next remove-click will repopulate it
                    deletedStopsList.clear();

                    mapViewer.repaint();
                });

                startRouteButton = new JButton("Start Route");
                startRouteButton.setEnabled(false);
                startRouteButton.addActionListener(e -> {
                   
                    String time = getTimeFromInput();
                    startRoute(time);
                    updateTimeLabels();
                });

                startHeatmapButton = new JButton("Start Heatmap");
                startHeatmapButton.addActionListener(e ->{
                    String time = getTimeFromInput();
                    startHeatmap(time);
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
                controls.add(resetDeletedStopsButton);
                controls.add(startHeatmapButton);
                controls.add(timeInputField);
                int resolution = 6;
                for(int i = 0; i <= resolution; i++){
                    controls.add(createSquareForBinIndex(i*(NUM_BINS)/resolution));
                }



                Painter<JXMapViewer> stopPainter = (Graphics2D g, JXMapViewer map, int w, int h) -> {
                    for (Map.Entry<String, Float> entry : stopTimeDiffs.entrySet()) {
                        String stopId = entry.getKey();
                        float diff = entry.getValue();

                        // Lookup stop in graph
                        Stop s = graph.stops.get(stopId);
                        if (s == null) continue;

                        // Convert lat/lon to pixel
                        GeoPosition pos = new GeoPosition(s.lat, s.lon);
                        Point2D p = map.convertGeoPositionToPoint(pos);

                        // Decide color
                        Color color;
                        if (diff == 0.0f) {
                            color = new Color(235, 245, 255);         // very light blue
                        } else if (diff <= 5.0f) {
                            color = new Color(180, 210, 255);         // light blue
                        } else if (diff <= 20.0f) {
                            color = new Color(100, 150, 255);         // normal blue
                        } else { // up to 50.0
                            color = new Color(30, 90, 220);           // dark blue
                        }

                        // Draw circle
                        double radius = 6;  // 4px radius
                        Ellipse2D.Double circle = new Ellipse2D.Double(
                                p.getX() - radius,
                                p.getY() - radius,
                                radius * 2,
                                radius * 2
                        );
                        g.setColor(color);
                        g.fill(circle);
                        g.setStroke(new BasicStroke(2f));
                        g.setColor(Color.BLACK);
                        g.draw(circle);
                    }
                };

                markerPainter = new MarkerPainter(markers);
                mapViewer.setOverlayPainter(CompositePainter.of(markerPainter, stopPainter));

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
                ArrayList<String> deletedStops = new ArrayList<>();
                deletedStops.add("2953526");
                deleteStops(deletedStops);

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

    private static void startHeatmap(String time){

        GeoPosition start;
        if (markers.size() >= 1) start = markers.get(0);
        else start = new GeoPosition(52.37455, 4.89527);
        System.out.println(start.toString());
        heatMaker = new HeatMapMaker(
                AMST_MAX_LAT,  // top edge
                AMST_MIN_LON,  // left edge
                AMST_MIN_LAT,  // bottom edge
                AMST_MAX_LON,  // right edge
                CELL_SIZE_METERS,
                CELL_OPACITY,
                NUM_BINS,
                graph,
                planner,
                start.getLatitude(),
                start.getLongitude(),
                time
        );
        System.out.println("HeatMap has " + heatMaker.getGrid().size() + " cells");

        // 4) Create the painter that will draw those squares:
        long startTime = System.currentTimeMillis();
        heatPainter = new HeatMapPainter(
                heatMaker.getGrid(),
                mapViewer.getTileFactory()
        );


        mapViewer.setOverlayPainter(CompositePainter.of(markerPainter, heatPainter/*, routePainter*/));
        mapViewer.repaint();
        //routeActive = true;
        graph.clearHeatMapStops();
        System.out.println("Painting time : "+(System.currentTimeMillis()-startTime)/1000);
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

            // 1) Read the current viewport in pixels:
            Rectangle vp = mapViewer.getViewportBounds();
            int zoomLevel = mapViewer.getZoom();
            TileFactory tf = mapViewer.getTileFactory();

            // 2) Convert the two corners to geographic coordinates:
            GeoPosition topLeftGeo = tf.pixelToGeo(
                    new Point2D.Double(vp.x, vp.y), zoomLevel);
            GeoPosition botRightGeo = tf.pixelToGeo(
                    new Point2D.Double(vp.x + vp.width, vp.y + vp.height),
                    zoomLevel);

//            // 3) Build the grid of SquareArea objects:
//            heatMaker = new HeatMapMaker(
//                    AMST_MAX_LAT,  // top edge
//                    AMST_MIN_LON,  // left edge
//                    AMST_MIN_LAT,  // bottom edge
//                    AMST_MAX_LON,  // right edge
//                    CELL_SIZE_METERS,
//                    CELL_OPACITY,
//                    NUM_BINS,
//                    graph,
//                    planner,
//                    start.getLatitude(),
//                    start.getLongitude(),
//                    getTimeFromInput()
//            );
//            System.out.println("HeatMap has " + heatMaker.getGrid().size() + " cells");
//
//            // 4) Create the painter that will draw those squares:
//            long startTime = System.currentTimeMillis();
//            heatPainter = new HeatMapPainter(
//                    heatMaker.getGrid(),
//                    mapViewer.getTileFactory()
//            );


            mapViewer.setOverlayPainter(CompositePainter.of(markerPainter/*, heatPainter*/, routePainter));
            mapViewer.repaint();
            routeActive = true;
        }
    }
    private static void resetRoute() {

        Painter<JXMapViewer> stopPainter = (Graphics2D g, JXMapViewer map, int w, int h) -> {
            for (Map.Entry<String, Float> entry : stopTimeDiffs.entrySet()) {
                String stopId = entry.getKey();
                float diff = entry.getValue();

                // Lookup stop in graph
                Stop s = graph.stops.get(stopId);
                if (s == null) continue;

                // Convert lat/lon to pixel
                GeoPosition pos = new GeoPosition(s.lat, s.lon);
                Point2D p = map.convertGeoPositionToPoint(pos);

                // Decide color
                Color color;
                if (diff == 0.0f) {
                    color = new Color(235, 245, 255);         // very light blue
                } else if (diff <= 5.0f) {
                    color = new Color(180, 210, 255);         // light blue
                } else if (diff <= 20.0f) {
                    color = new Color(100, 150, 255);         // normal blue
                } else { // up to 50.0
                    color = new Color(30, 90, 220);           // dark blue
                }

                // Draw circle
                double radius = 6;  // 4px radius
                Ellipse2D.Double circle = new Ellipse2D.Double(
                        p.getX() - radius,
                        p.getY() - radius,
                        radius * 2,
                        radius * 2
                );
                g.setColor(color);
                g.fill(circle);
                g.setStroke(new BasicStroke(2f));
                g.setColor(Color.BLACK);
                g.draw(circle);
            }
        };

        mapViewer.setOverlayPainter(CompositePainter.of(markerPainter, stopPainter));
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
            if(stepDurations != null) endTimeLabel.setText("End Time: " + timeFormat.format(new Date(now.getTime() + stepDurations.stream()
  .reduce(0, Integer::sum) * 60 * 1000))); // Sum of durations in milliseconds
        }
    }

    private static void resetTimeLabels() {
        startTimeLabel.setText("Start Time: --:--");
        endTimeLabel.setText("End Time: --:--");
        timeInputField.setText("Right now");
        timeInputField.setForeground(Color.GRAY);
    }

    public static void deleteStops(ArrayList<String> deletedStopIds){
        for(String s : deletedStopIds){
            planner.deleteStop(s);
        }
    }

    public static void restoreDeletedStops(){
        planner.reloadDeletedStop();
    }

    public static JPanel createSquareForBinIndex(int index){
        int size = 500;
        Color color = HeatMapMaker.computeColorBin(index, NUM_BINS);
        String timeLabel = index+" min";

        JPanel square = new JPanel();
        square.setSize(new Dimension(size,size));
        square.setBackground(color);
        JPanel result = new JPanel();
        result.add(new JLabel(timeLabel));
        result.add(square);
        return result;
    }

    public static Map<String, Float> loadStopTimeDiffs(String filename) {
        Map<String, Float> stopTimeDiffMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                // skip empty lines
                if (line.trim().isEmpty()) continue;
                // each line is stopId=diffValue
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String stopId = parts[0];
                    Float diff = Float.parseFloat(parts[1]);
                    stopTimeDiffMap.put(stopId, diff);
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading stop time diffs: " + e.getMessage());
        }
        return stopTimeDiffMap;
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