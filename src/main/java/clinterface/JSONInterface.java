package clinterface;

import com.leastfixedpoint.json.JSONReader;
import com.leastfixedpoint.json.JSONSyntaxError;
import com.leastfixedpoint.json.JSONWriter;
import planner.RoutePlanner;
import storage.TransitGraph;
import structs.JourneyStep;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class JSONInterface {
    private final TransitGraph graph;
    private final GTFSLoader loader;
    private RoutePlanner planner;
    private final JSONReader reader;
    private final JSONWriter<OutputStreamWriter> writer;

    public JSONInterface() throws IOException {
        graph = new TransitGraph();
        loader = new GTFSLoader(graph);
        reader = new JSONReader(new InputStreamReader(System.in));
        writer = new JSONWriter<>(new OutputStreamWriter(System.out));
    }

    public void run() throws IOException {
        while (true) {
            Object obj;
            try {
                obj = reader.read();
            } catch (JSONSyntaxError e) {
                //bad JSON, must terminate
                sendError("Bad JSON input");
                break;
            } catch (EOFException e) {
                //clean end of input
                break;
            } catch (IOException e) {
                //non-fatal I/O error reading input
                sendError(e.getMessage());
                continue;
            }

            if (!(obj instanceof Map)) {
                //valid JSON but not an object
                sendError("Bad request");
                continue;
            }

            @SuppressWarnings("unchecked")
            Map<String,Object> req = (Map<String,Object>) obj;

            if (req.containsKey("load")) {
                try {
                    String path = (String) req.get("load");
                    loader.loadFromZip(Path.of(path));
                    planner = new RoutePlanner(graph);
                    sendOk();
                } catch (FileNotFoundException e) {
                    sendError("File not found");
                    break;
                } catch (IllegalArgumentException e) {
                    //fatal: invalid GTFS data
                    sendError(e.getMessage());
                    break;
                } catch (Exception e) {
                    //fatal: other load error
                    sendError(e.getMessage());
                    break;
                }

            } else if (req.containsKey("routeFrom")) {
                if (planner == null) {
                    sendError("No GTFS data loaded");
                    continue;
                }
                try {
                    @SuppressWarnings("unchecked")
                    Map<String,Object> rf = (Map<String,Object>) req.get("routeFrom");
                    @SuppressWarnings("unchecked")
                    Map<String,Object> to = (Map<String,Object>) req.get("to");
                    String start = (String) req.get("startingAt");

                    double slat = ((Number) rf.get("lat")).doubleValue();
                    double slon = ((Number) rf.get("lon")).doubleValue();
                    double dlat = ((Number) to.get("lat")).doubleValue();
                    double dlon = ((Number) to.get("lon")).doubleValue();

                    List<JourneyStep> journey = planner.computeRoute(slat, slon, dlat, dlon, start);
                    sendJourney(journey);
                } catch (ClassCastException | NullPointerException e) {
                    //missing or wrong-type fields
                    sendError("Bad request");
                } catch (IllegalArgumentException e) {
                    //invalid parameters or no route found
                    sendError(e.getMessage());
                } catch (Exception e) {
                    //routing error (non-fatal)
                    sendError(e.getMessage());
                }

            } else {
                sendError("Bad request");
            }
        }
    }

    private void sendOk() throws IOException {
        LinkedHashMap<String,Object> resp = new LinkedHashMap<>();
        resp.put("ok", "loaded");
        writer.write(resp);
        writer.getWriter().write('\n');
        writer.getWriter().flush();
    }

    private void sendError(String msg) throws IOException {
        LinkedHashMap<String,Object> resp = new LinkedHashMap<>();
        resp.put("error", msg);
        writer.write(resp);
        writer.getWriter().write('\n');
        writer.getWriter().flush();
    }

    private void sendJourney(List<JourneyStep> steps) throws IOException {
        Writer out = writer.getWriter();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"ok\":[");
        for (int i = 0; i < steps.size(); i++) {
            JourneyStep js = steps.get(i);
            sb.append("{\"mode\":\"")
              .append(js.mode).append("\"");
            sb.append(",\"to\":{\"lat\":")
              .append(js.to[0]).append(',').append("\"lon\":").append(js.to[1]).append('}');
            sb.append(",\"duration\":")
              .append(js.durationMin);
            sb.append(",\"startTime\":\"")
              .append(js.startTime).append("\"");
            if ("ride".equals(js.mode)) {
                sb.append(",\"stop\":\"")
                  .append(js.stop).append("\"");
                sb.append(",\"route\":{")
                  .append("\"operator\":\"").append(js.routeInfo.operator).append("\"")
                  .append(",\"shortName\":\"").append(js.routeInfo.shortName).append("\"")
                  .append(",\"longName\":\"").append(js.routeInfo.longName).append("\"")
                  .append(",\"headSign\":\"").append(js.routeInfo.headSign).append("\"")
                  .append('}');
            }
            sb.append('}');
            if (i < steps.size() - 1) sb.append(',');
        }
        sb.append("]}");
        out.write(sb.toString());
        out.write('\n');
        out.flush();
    }
}
