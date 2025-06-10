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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
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
    private static JLabel stopsLabel;
    private static JLabel transfersLabel;
    private static String arrivalTime;
    private static final String DEFAULT_GTFS_PATH = "data/amsterdam_gtfs_final.zip"; // Default path for GTFS data
    private static final String DEFAULT_TILE_PATH = "/tiles.zip"; // Default path for tile data


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            mapViewer = new JXMapViewer();
            try {
                URL tilesURL = AmsterdamMap.class.getResource(DEFAULT_TILE_PATH);
                String jarURL = "jar:" + tilesURL.toURI() + "!";

                TileFactoryInfo info = new LocalTileFactoryInfo(jarURL);
                TileFactory tileFactory = new DefaultTileFactory(info);

                mapViewer.setTileFactory(tileFactory);
                graph = new TransitGraph();
                loader = new GTFSLoader(graph);
                dbManager = new DBManager();
                try {
                    String path = DEFAULT_GTFS_PATH; // Default path for GTFS data
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
                    double lon = Math.max(4.8, Math.min(4.9, center.getLongitude()));
                    if (lat != center.getLatitude() || lon != center.getLongitude()) {
                        mapViewer.setCenterPosition(new GeoPosition(lat, lon));
                    }
                });
                mapViewer.setZoom(3);
                mapViewer.setAddressLocation(amsterdam);
               
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
                                resetLabels();
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
                    resetLabels();

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
                stopsLabel = new JLabel("Stops: 0");
                transfersLabel = new JLabel("Transfers: 0");

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
            int transfers = 0;
            int stops = 0;
            String lastLine = "";
            route.add(start);
            List<JourneyStep> journey = planner.computeRoute(start.getLatitude(), start.getLongitude(), end.getLatitude(), end.getLongitude(), time);
            arrivalTime = calculateArrivalTime(journey.get(journey.size() - 1).startTime, journey.get(journey.size() - 1).durationMin); // Get the arrival time from the last step
            for(JourneyStep step : journey) {
                if(step.mode.equals("ride")) {
                    stops++;
                    if(!lastLine.equals(step.routeInfo.shortName)) {
                        transfers++;
                        lastLine = step.routeInfo.shortName;
                    }
                }
                GeoPosition stepPos = new GeoPosition(step.to[0], step.to[1]);
                modes.add(step.mode);
                route.add(stepPos);
            }
            transfers = Math.max(transfers-1, 0); // Adjust transfers count to match the number of transfers made
            transfersLabel.setText("Transfers: " + transfers);
            stopsLabel.setText("Stops: " + stops);
            routePainter = new RoutePainter(route, modes);
            mapViewer.setOverlayPainter(CompositePainter.of(markerPainter, routePainter));
            mapViewer.repaint();
            // create a popup with route info
            JFrame routeInfoFrame = createRouteInfoFrame(journey);
            routeInfoFrame.setVisible(true);
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
            
            startTimeLabel.setText("Start Time: " + timeFormat.format(startTime));
            endTimeLabel.setText("End Time: " + timeFormat.format(arrivalTime));
        } catch (Exception e) {
            // If parsing fails, use current time
            startTimeLabel.setText("Start Time: " + timeFormat.format(now));
            endTimeLabel.setText("End Time: " + arrivalTime); // Sum of durations in milliseconds
        }
    }

    private static void resetLabels() {
        startTimeLabel.setText("Start Time: --:--");
        endTimeLabel.setText("End Time: --:--");
        timeInputField.setText("Right now");
        stopsLabel.setText("Stops: 0");
        transfersLabel.setText("Transfers: 0");
        timeInputField.setForeground(Color.GRAY);
    }
    //here's an example of journey step:
    //{"mode":"walk","to":{"lat":52.388085,"lon":4.884871},"duration":2,"startTime":"12:06"},{"mode":"ride","to":{"lat":52.384843,"lon":4.88341},"duration":2,"startTime":"12:08","stop":"Amsterdam, Haarlemmerplein","route":{"operator":"GVB","shortName":"5","longName":"Lijn 5","headSign":"Amstelveen Stadshart"}},{"mode":"walk","to":{"lat":52.385159,"lon":4.883715},"duration":1,"startTime":"12:10"},{"mode":"ride","to":{"lat":52.383119,"lon":4.888056},"duration":2,"startTime":"12:10","stop":"Amsterdam, Buiten Oranjestraat","route":{"operator":"GVB","shortName":"21","longName":"Lijn 21","headSign":"Centraal Station"}},{"mode":"ride","to":{"lat":52.381507,"lon":4.89157},"duration":1,"startTime":"12:12","stop":"Amsterdam, Buiten Brouwersstraat","route":{"operator":"GVB","shortName":"21","longName":"Lijn 21","headSign":"Centraal Station"}},{"mode":"ride","to":{"lat":52.379729,"lon":4.900461},"duration":5,"startTime":"12:13","stop":"Centraal Station","route":{"operator":"GVB","shortName":"21","longName":"Lijn 21","headSign":"Centraal Station"}},{"mode":"walk","to":{"lat":52.378208,"lon":4.899949},"duration":3,"startTime":"12:18"},{"mode":"ride","to":{"lat":52.389459,"lon":4.919774},"duration":2,"startTime":"12:24","stop":"Amsterdam, Noorderpark","route":{"operator":"GVB","shortName":"52","longName":"Lijn 52","headSign":"Noord"}},{"mode":"walk","to":{"lat":52.389018,"lon":4.919558},"duration":1,"startTime":"12:26"},{"mode":"ride","to":{"lat":52.388139,"lon":4.924635},"duration":2,"startTime":"12:27","stop":"Amsterdam, Johan v.Hasseltweg","route":{"operator":"GVB","shortName":"35","longName":"Lijn 35","headSign":"Olof Palmeplein"}},{"mode":"ride","to":{"lat":52.389956,"lon":4.925057},"duration":1,"startTime":"12:29","stop":"Amsterdam, Merelstraat","route":{"operator":"GVB","shortName":"35","longName":"Lijn 35","headSign":"Olof Palmeplein"}},{"mode":"ride","to":{"lat":52.394878,"lon":4.928811},"duration":2,"startTime":"12:30","stop":"Amsterdam, Rode Kruisstraat","route":{"operator":"GVB","shortName":"35","longName":"Lijn 35","headSign":"Olof Palmeplein"}},{"mode":"ride","to":{"lat":52.394181,"lon":4.934504},"duration":2,"startTime":"12:32","stop":"Amsterdam, Nieuwe Purmerweg","route":{"operator":"GVB","shortName":"35","longName":"Lijn 35","headSign":"Olof Palmeplein"}},{"mode":"ride","to":{"lat":52.392708,"lon":4.939543},"duration":1,"startTime":"12:33","stop":"Amsterdam, Wervershoofstraat","route":{"operator":"GVB","shortName":"35","longName":"Lijn 35","headSign":"Olof Palmeplein"}},{"mode":"ride","to":{"lat":52.390458,"lon":4.945692},"duration":2,"startTime":"12:34","stop":"Amsterdam, Purmerplein","route":{"operator":"GVB","shortName":"35","longName":"Lijn 35","headSign":"Olof Palmeplein"}},{"mode":"walk","to":{"lat":52.38916848288419,"lon":4.945132807792146},"duration":2,"startTime":"12:36"}
    private static JFrame createRouteInfoFrame(List<JourneyStep> journey) {
    JFrame routeInfoFrame = new JFrame("Route Information");
    routeInfoFrame.setLayout(new BorderLayout());
    JTextArea routeInfoArea = new JTextArea();
    routeInfoArea.setEditable(false);
    routeInfoArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
    StringBuilder infoText = new StringBuilder();
    
    // Add header
    infoText.append(String.format("%-50s%8s%10s\n", "Action", "Duration", "Arrival"));
    infoText.append("------------------------------------------------------------\n");

    int i = 0;
    while (i < journey.size()) {
        JourneyStep step = journey.get(i);
        
        if (step.mode.equals("walk")) {
            infoText.append(String.format("%-50s%7s%10s\n", 
                "Walk", 
                step.durationMin + " min", 
                calculateArrivalTime(step.startTime, step.durationMin)));
            i++;
        } 
        else if (step.mode.equals("ride")) {
            // Start of a potential ride sequence
            String currentRouteName = step.routeInfo.shortName;
            infoText.append(String.format("%-50s%7s%10s\n", 
                "Take " + step.routeInfo.longName, 
                step.durationMin + " min", 
                calculateArrivalTime(step.startTime, step.durationMin)));
            
            // Add first stop
            infoText.append(String.format("  %-48s%8s%10s\n", 
                "→ " + step.stop, 
                "", 
                calculateArrivalTime(step.startTime, step.durationMin)));
            
            // Check subsequent steps for same route
            i++;
            while (i < journey.size() && 
                   journey.get(i).mode.equals("ride") && 
                   journey.get(i).routeInfo.shortName.equals(currentRouteName)) {
                JourneyStep nextStep = journey.get(i);
                infoText.append(String.format("  %-48s%8s%10s\n", 
                    "→ " + nextStep.stop, 
                    "", 
                    calculateArrivalTime(nextStep.startTime, nextStep.durationMin)));
                i++;
            }
            infoText.append("\n"); // Add empty line between different routes
        }
    }

    routeInfoArea.setText(infoText.toString());
    routeInfoFrame.add(new JScrollPane(routeInfoArea), BorderLayout.CENTER);
    routeInfoFrame.setSize(600, 400);  // Increased width for better formatting
    routeInfoFrame.setVisible(true);
    return routeInfoFrame;
}

private static String calculateArrivalTime(String startTime, int durationMin) {
    try {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        Date date = sdf.parse(startTime);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MINUTE, durationMin);
        return sdf.format(cal.getTime());
    } catch (ParseException e) {
        return startTime; // fallback if parsing fails
    }
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