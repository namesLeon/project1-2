import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import com.leastfixedpoint.json.JSONReader;
import com.leastfixedpoint.json.JSONSyntaxError;
import com.leastfixedpoint.json.JSONWriter;

public class RoutingEngine {
    private JSONReader requestReader = new JSONReader(new InputStreamReader(System.in));
    private JSONWriter<OutputStreamWriter> responseWriter = new JSONWriter<>(new OutputStreamWriter(System.out));
    private GTFSData gtfsData = null;
    
    public static void main(String[] args) throws IOException {
        new RoutingEngine().run();
    }
    
    public void run() throws IOException {
        System.err.println("Starting Routing Engine");
        while (true) {
            Object json;
            try {
                json = requestReader.read();
            } catch (JSONSyntaxError e) {
                sendError("Bad JSON input");
                break; // fatal error
            } catch (EOFException e) {
                System.err.println("End of input detected");
                break;
            }
            
            if (!(json instanceof Map)) {
                sendError("Bad request");
                continue;
            }
            
            Map<String, Object> request = (Map<String, Object>) json;
            
            //processing "load" request
            if (request.containsKey("load")) {
                String filename = (String) request.get("load");
                if (loadGTFSData(filename)) {
                    sendOk("loaded");
                } else {
                    sendError("File not found or invalid GTFS data");
                    break; // fatal error
                }
                continue;
            }
            
            //processing "routeFrom" requests
            if (request.containsKey("routeFrom") &&
                request.containsKey("to") &&
                request.containsKey("startingAt")) {
                
                if (gtfsData == null) {
                    sendError("No GTFS data loaded");
                    continue;
                }
                
                Object sourceObj = request.get("routeFrom");
                Object targetObj = request.get("to");
                String startingAt = (String) request.get("startingAt");
                
                if (!(sourceObj instanceof Map) || !(targetObj instanceof Map)) {
                    sendError("Bad request: invalid point objects");
                    continue;
                }
                
                Map<String, Object> sourcePoint = (Map<String, Object>) sourceObj;
                Map<String, Object> targetPoint = (Map<String, Object>) targetObj;
                
                List<Map<String, Object>> route = computeRoute(sourcePoint, targetPoint, startingAt);
                if (route != null) {
                    sendOk(route);
                } else {
                    sendError("Routing computation failed");
                }
                continue;
            }
            sendError("Bad request");
        }
    }
    
    private boolean loadGTFSData(String filename) {
        File file = new File(filename);
        if (!file.exists() || !file.isFile()) {
            return false;
        }
        //actual parsing and validating logic would go here
        gtfsData = new GTFSData();
        System.err.println("GTFS data loaded from: " + filename);
        return true;
    }
    
    private List<Map<String, Object>> computeRoute(Map<String, Object> source, Map<String, Object> target, String startingAt) {
        List<Map<String, Object>> routeSteps = new ArrayList<>();
        //computing logic would go here
        return routeSteps;
    }
    
    //extract lat/long from a map containing coordonates
    private double getDoubleFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else {
            return 0.0;
        }
    }
    
    //add minutes to a timer(useful in time computation)
    private String addMinutesToTime(String time, int minutesToAdd) {
        try {
            String[] parts = time.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            minutes += minutesToAdd;
            hours += minutes / 60;
            minutes = minutes % 60;
            hours = hours % 24;
            return String.format("%02d:%02d", hours, minutes);
        } catch (Exception e) {
            // If parsing fails, return the original time.
            return time;
        }
    }
    
    private void sendOk(Object value) throws IOException {
        responseWriter.write(Map.of("ok", value));
        responseWriter.getWriter().write('\n');
        responseWriter.getWriter().flush();
    }
    
    private void sendError(String message) throws IOException {
        responseWriter.write(Map.of("error", message));
        responseWriter.getWriter().write('\n');
        responseWriter.getWriter().flush();
    }
    
    private class GTFSData {
        //fields, data structures for GTFS data would be defined here
    }
}
