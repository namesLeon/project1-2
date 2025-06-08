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
import java.util.concurrent.*;

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

    /**
     * Important note: <br>
     * Now we have two possibilities to "delete" a stop <br>
     *  1. Delete the stop at the moment of accepting the input for finding the route <br>
     *      {"routeFrom":{}, "startingAt":{}, "to":{}, "deletedStopId":"xyz"} - the last key-value pair is optional.
     *     Here the stop is added again after finding the path. <br>
     *  2. Delete the stop at the moment of loading the files into the graph <br>
     *      {"load" : "PATH", "deletedStopId":"xyz"} - the last key-value pair is optional.
     *     Here the stop deletion is more binding than in step 1. The stop is not after finding the path. <br>
     *  The 1st method would then be used for example for the GUI or some quick comparisons, whereas
     *  2nd method would be used in experiments (so that we don't waste time for repetitive stop add and removal)
     * @throws IOException
     */
    public void run() throws IOException {
        String deletedStopId = null;
        
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
                if(req.containsKey("deletedStopId")){
                    deletedStopId = (String) req.get("deletedStopId");
                }
                else{
                    deletedStopId = null;
                }
                try {
                    String path = (String) req.get("load");
                    if(deletedStopId != null) loader.setDeletedStopId(deletedStopId);
                    else loader.setDeletedStopNull();
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
                    deletedStopId = null;
                    if(req.containsKey("deletedStopId")){
                        try {
                            deletedStopId = (String) req.get("deletedStopId");
                            planner.deleteStop(deletedStopId);
                        }
                        catch (Exception e){
                            sendError("Bad request");
                        }
                    }

                    double slat = ((Number) rf.get("lat")).doubleValue();
                    double slon = ((Number) rf.get("lon")).doubleValue();
                    double dlat = ((Number) to.get("lat")).doubleValue();
                    double dlon = ((Number) to.get("lon")).doubleValue();
                    
                    //long t0 = System.nanoTime();
                    List<JourneyStep> journey = planner.computeRoute(slat, slon, dlat, dlon, start);
                    sendJourney(journey);
                    if(deletedStopId!=null)planner.reloadDeletedStop();
                    deletedStopId = null;
                    //long t1 = System.nanoTime();
                    //long elapsedNanos = t1 - t0;
                    //double elapsedMillis = elapsedNanos / 1_000_000.0;
                    //JourneyStep finalstep = journey.get(journey.size()-1);
                    //int fin = parsetime(finalstep.startTime)/60 + finalstep.durationMin;
                    //int st = parsetime(start)/60;
                    //System.out.printf("Query took %.3f ms, returned %d steps, total journey time %d minutes\n",
                    //elapsedMillis, journey.size(), fin - st);
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

            } else if (req.containsKey("batchRoutes")) {
                handleBatch(req);
            } else if (req.containsKey("runFile")) {
                handleRunFile(req);
            }
            else {
                sendError("Bad request");
            }
        }
    }

    private void handleRunFile(Map<String,Object> req) throws IOException {
        try {
            String file = (String) req.get("runFile");
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                String deletedStopId = null;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    Object obj;
                    try {
                        obj = new JSONReader(new StringReader(line)).read();
                    } catch (Exception e) {
                        writer.getWriter().write("{\"error\":\"Bad JSON input\"}\n");
                        continue;
                    }
                    if (!(obj instanceof Map)) {
                        writer.getWriter().write("{\"error\":\"Bad request\"}\n");
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    Map<String,Object> subReq = (Map<String,Object>) obj;
                    if (subReq.containsKey("load")) {
                        if(subReq.containsKey("deletedStopId")){
                            deletedStopId = (String) subReq.get("deletedStopId");
                        }
                        else{
                            deletedStopId = null;
                        }
                        try {
                            String path = (String) subReq.get("load");
                                if(deletedStopId != null) loader.setDeletedStopId(deletedStopId);
                                else loader.setDeletedStopNull();
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
                    } else if (subReq.containsKey("batchRoutes")) {
                        handleBatch(subReq);
                    } 
                }
            }
        } catch (ClassCastException | NullPointerException e) {
            sendError("Bad request");
        } catch (Exception e) {
            sendError(e.getMessage());
        }
    }

    private void handleBatch(Map<String,Object> req) throws IOException {
        if (planner == null) {
            sendError("No GTFS data loaded");
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String,Object> br = (Map<String,Object>) req.get("batchRoutes");
            String inputFile = (String) br.get("input");
            String outputFile = (String) br.get("output");
            processBatch(inputFile, outputFile);
            sendOk();
        } catch (ClassCastException | NullPointerException e) {
            sendError("Bad request");
        } catch (Exception e) {
            sendError(e.getMessage());
        }
    }

    private void processBatch(String inputPath, String outputPath) throws IOException {
        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<String>> results = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(inputPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String reqLine = line.trim();
                if (reqLine.isEmpty()) continue;
                results.add(pool.submit(() -> handleSingleRequest(reqLine)));
            }
        }
        pool.shutdown();
        try { pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS); } catch (InterruptedException ignored) {}

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath))) {
            for (Future<String> f : results) {
                try {
                    bw.write(f.get());
                } catch (Exception e) {
                    bw.write("{\"error\":\"" + e.getMessage() + "\"}");
                }
                bw.newLine();
            }
        }
    }

    private String handleSingleRequest(String reqLine) {
        try {
            JSONReader lr = new JSONReader(new StringReader(reqLine));
            Object o = lr.read();
            if (!(o instanceof Map)) return "{\"error\":\"Bad request\"}";
            @SuppressWarnings("unchecked")
            Map<String,Object> req = (Map<String,Object>) o;
            if (!req.containsKey("routeFrom")) return "{\"error\":\"Missing routeFrom\"}";
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
            return serializeJourney(journey);
        } catch (Exception e) {
            String msg = e.getMessage().replace("\"", "\\\"");
            return "{\"error\":\"" + msg + "\"}";
        }
    }

    private String serializeJourney(List<JourneyStep> steps) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"ok\":[");
        for (int i = 0; i < steps.size(); i++) {
            JourneyStep js = steps.get(i);
            sb.append("{\"mode\":\"").append(js.mode).append("\"");
            sb.append(",\"to\":{\"lat\":").append(js.to[0]).append(",\"lon\":").append(js.to[1]).append('}');
            sb.append(",\"duration\":").append(js.durationMin);
            sb.append(",\"startTime\":\"").append(js.startTime).append("\"");
            if ("ride".equals(js.mode)) {
                sb.append(",\"stop\":\"").append(js.stop).append("\"");
                sb.append(",\"route\":{\"operator\":\"")
                  .append(js.routeInfo.operator)
                  .append("\",\"shortName\":\"")
                  .append(js.routeInfo.shortName)
                  .append("\",\"longName\":\"")
                  .append(js.routeInfo.longName)
                  .append("\",\"headSign\":\"")
                  .append(js.routeInfo.headSign).append("\"}");
            }
            sb.append('}');
            if (i < steps.size() - 1) sb.append(',');
        }
        sb.append("]}");
        return sb.toString();
    }

    private int parsetime(String hhmm) {
        String[] parts = hhmm.split(":");
        try {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            if (minutes < 0 || minutes >= 60 || hours < 0) {
                throw new IllegalArgumentException("Time values out of range: " + hhmm);
            }
            return hours * 3600 + minutes * 60;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric value in time: " + hhmm, e);
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

    public static void sendJourney(List<JourneyStep> steps) throws IOException {
        Writer out = new JSONWriter<>(new OutputStreamWriter(System.out)).getWriter();
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
