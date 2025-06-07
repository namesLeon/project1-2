package planner;

import DB.DBManager;
import storage.TransitGraph;
import structs.*;

import java.util.*;

public class RoutePlanner {
    private final TransitGraph graph;
    private String deletedStopId = null;

    public RoutePlanner(TransitGraph graph) {
        this.graph = graph;
    }

    private static class Label implements Comparable<Label> {
        String stopId;
        int arrivalSec;    //g‐cost so far
        int fVal;          //= g + h (heuristic)
        Label prev;
        DepartureEvent viaEvent;
        WalkEdge viaWalk;

        @Override
        public int compareTo(Label o) {
            int cmp = Integer.compare(this.fVal, o.fVal);
            if (cmp != 0) return cmp;
            return Integer.compare(this.arrivalSec, o.arrivalSec);
        }
    }

    public List<JourneyStep> computeRoute(
            double srcLat, double srcLon,
            double dstLat, double dstLon,
            String startTime) {
        int startSec = parsetime(startTime);

        //detect if departure point coincides with stop
        String srcStop = null;
        for (Stop s : graph.stops.values()) {
            if (s.lat == srcLat && s.lon == srcLon) srcStop = s.stopId;
        }
        //System.out.println("[LOG] srcStop=" + srcStop + ", dstStop=" + dstStop);

        //direct walk baseline
        double directDist = TransitGraph.haversine(srcLat, srcLon, dstLat, dstLon);
        int directSecs = (int)Math.ceil(directDist / 5000.0 * 3600.0);
        int bestDest = startSec + directSecs; 
        int finalWalkSec = directSecs;
        Label bestLabelAtDest = null;

        //for each stopId, the best g‐cost (arrivalSec) seen so far
        Map<String,Integer> best = new HashMap<>();
        PriorityQueue<Label> pq = new PriorityQueue<>();

        //src coincides with a stop, push exactly that stop with g = startSec
        if (srcStop != null) {
            Label init = new Label();
            init.stopId = srcStop;
            init.arrivalSec = startSec;
            init.fVal = startSec + heuristic(srcStop, dstLat, dstLon);
            best.put(srcStop, startSec);
            pq.add(init);
            //System.out.println("[LOG] seeded stop " + srcStop + "@" + startTime);
        } else {
            //push all stops with a walk from src to each stopID
            for (Stop s : graph.stops.values()) {
                double d = TransitGraph.haversine(srcLat, srcLon, s.lat, s.lon);
                int secs = (int)Math.ceil(d / 5000.0 * 3600.0);
                Label init = new Label();
                init.stopId = s.stopId;
                init.arrivalSec = startSec + secs;
                init.fVal = init.arrivalSec + heuristic(s.stopId, dstLat, dstLon);
                init.viaWalk = new WalkEdge("__SRC__", s.stopId, secs);
                best.put(s.stopId, init.arrivalSec);
                pq.add(init);
            }
            //System.out.println("[LOG] seeded all stops via walk");
        }

        while (!pq.isEmpty()) {
            Label cur = pq.poll();
            if (cur.stopId.equals(graph.deletedStop) || cur.arrivalSec > best.getOrDefault(cur.stopId, Integer.MAX_VALUE)) {
                continue;
            }
            //System.out.println("[LOG] visit " + cur.stopId + " @sec=" + cur.arrivalSec);

            //attempt final walk from this stop to the dest
            Stop s = graph.stops.get(cur.stopId);
            double distToDest = TransitGraph.haversine(s.lat, s.lon, dstLat, dstLon);
            int wsecs = (int)Math.ceil(distToDest / 5000.0 * 3600.0);
            int arrivalAtDest = cur.arrivalSec + wsecs;
            if (arrivalAtDest < bestDest) {
                bestDest = arrivalAtDest;
                finalWalkSec = wsecs;
                bestLabelAtDest = cur;
            }

            //since f is a lower bound, we can safely terminate if it is greater the best known arrival time
            if (cur.fVal >= bestDest) {
                break;
            }

            //walk edges
            for (WalkEdge we : graph.walkEdges) {
                if (!we.fromStopId.equals(cur.stopId)) continue;
                int g2 = cur.arrivalSec + we.walkDurationSec;
                if (g2 < best.getOrDefault(we.toStopId, Integer.MAX_VALUE)) {
                    Label nxt = new Label();
                    nxt.stopId = we.toStopId;
                    nxt.arrivalSec = g2;
                    nxt.fVal = g2 + heuristic(we.toStopId, dstLat, dstLon);
                    nxt.prev = cur;
                    nxt.viaWalk = we;
                    best.put(nxt.stopId, g2);
                    pq.add(nxt);
                    //System.out.println("[LOG] relax walk " + we.fromStopId + "->" + we.toStopId + " arr=" + g2);
                }
            }

            //transit edges
            List<DepartureEvent> deps = graph.departures.getOrDefault(cur.stopId, Collections.emptyList());
            for (DepartureEvent ev : deps) {
                if (ev.departureSec < cur.arrivalSec) continue;  
                int g2 = ev.arrivalSec;
                if (g2 < best.getOrDefault(ev.toStopId, Integer.MAX_VALUE)) {
                    Label nxt = new Label();
                    nxt.stopId = ev.toStopId;
                    nxt.arrivalSec = g2;
                    nxt.fVal = g2 + heuristic(ev.toStopId, dstLat, dstLon);
                    nxt.prev = cur;
                    nxt.viaEvent = ev;
                    best.put(nxt.stopId, g2);
                    pq.add(nxt);
                    //System.out.println("[LOG] relax ride " + cur.stopId + "->" + ev.toStopId + " dep=" + ev.departureSec + " arr=" + g2);
                }
            }
        }

        List<JourneyStep> steps = new ArrayList<>();
        if (bestLabelAtDest == null) {
            //System.out.println("[LOG] direct walk fallback");
            steps.add(new JourneyStep(dstLat, dstLon, (int)Math.ceil(Double.valueOf(finalWalkSec)/60.0), formatTime(startSec)));
            return steps;
        }

        //backtrack from bestLabelAtDest, add final walk if needed
        List<JourneyStep> rev = new ArrayList<>();

        //final walk
        if (finalWalkSec > 0) {                  
            rev.add(new JourneyStep(dstLat, dstLon, (int)Math.ceil(Double.valueOf(finalWalkSec)/60.0), formatTime(bestDest - finalWalkSec)));
        }
        //backtrack the chain of labels
        for (Label cur = bestLabelAtDest; cur != null; cur = cur.prev) {
            if (cur.viaEvent != null) {
                //we came here via a transit‐ride
                Stop st = graph.stops.get(cur.stopId);
                int dur = (int)Math.ceil(Double.valueOf(cur.arrivalSec - cur.viaEvent.departureSec)/60.0);
                rev.add(new JourneyStep(
                        st.lat, st.lon, dur,
                        formatTime(cur.viaEvent.departureSec),
                        st.stopName, cur.viaEvent.routeInfo));
            }
            else if (cur.viaWalk != null) {
                //we arrived here by walking from viaWalk.fromStopId to viaWalk.toStopId
                Stop toStop = graph.stops.get(cur.viaWalk.toStopId);
                rev.add(new JourneyStep(
                        toStop.lat, toStop.lon,
                        (int)Math.ceil(Double.valueOf(cur.viaWalk.walkDurationSec)/60.0),
                        formatTime(cur.arrivalSec - cur.viaWalk.walkDurationSec)));
            }
            //if both viaEvent and viaWalk are null, that was the initial label
        }
        Collections.reverse(rev);
        steps.addAll(rev);
        return steps;
    }

    private int heuristic(String stopId, double dstLat, double dstLon) {
        Stop st = graph.stops.get(stopId);
        double d = TransitGraph.haversine(st.lat, st.lon, dstLat, dstLon);
        //assume a straight as-the-crow-flies path from stopID to the destination
        //assume an absurdly high average transit speed
        //return a lower bound on the remaining journey time from stopID
        return (int)Math.ceil((d / 25000.0 * 3600.0));
    }

    private static String formatTime(int sec) {
        sec %= 86400;
        if (sec < 0) sec += 86400;
        int hh = sec / 3600;
        int mm = (int)Math.ceil(Double.valueOf(sec % 3600) / 60.0);
        if(mm == 60){
            if(hh == 23){
                hh = 0;
                mm = 0;
            }
            else{
                hh += 1;
                mm = 0;
            }
        }
        return String.format("%02d:%02d", hh, mm);
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

    public void deleteStop(String stopId){
        graph.deleteStop(stopId);
        deletedStopId = String.valueOf(stopId);
    }

    public void reloadDeletedStop(){
        graph.reloadDeletedStop();
        deletedStopId = null;
    }
}
