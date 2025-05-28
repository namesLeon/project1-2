package planner;

import storage.TransitGraph;
import structs.*;

import java.util.*;

public class RoutePlanner {
    private final TransitGraph graph;

    public RoutePlanner(TransitGraph graph) {
        this.graph = graph;
    }

    private static class Label implements Comparable<Label> {
        String stopId;
        int arrivalSec;
        Label prev;
        DepartureEvent viaEvent;
        WalkEdge viaWalk;

        @Override
        public int compareTo(Label o) {
            return Integer.compare(this.arrivalSec, o.arrivalSec);
        }
    }

    /**
     * Computes the earliest-arrival route (possibly absurdly walking) from arbitrary source
     * coordinates to arbitrary destination coordinates, allowing direct walks or any combination
     * of transit and walking edges.
     */
    public List<JourneyStep> computeRoute(
            double srcLat, double srcLon,
            double dstLat, double dstLon,
            String startTime) {
        int startSec = parsetime(startTime);

        //detect if departure/destination coincide exactly with stops
        String srcStop = null, dstStop = null;
        for (Stop s : graph.stops.values()) {
            if (s.lat == srcLat && s.lon == srcLon) srcStop = s.stopId;
            if (s.lat == dstLat && s.lon == dstLon) dstStop = s.stopId;
        }
        //System.out.println("[LOG] srcStop=" + srcStop + ", dstStop=" + dstStop);

        //direct walk baseline
        double directDist = TransitGraph.haversine(srcLat, srcLon, dstLat, dstLon);
        int directSecs = (int)Math.round(directDist / 5000.0 * 3600.0);
        int bestDest = startSec + directSecs;
        int finalWalkSec = directSecs;
        Label destLabel = null;

        Map<String,Integer> best = new HashMap<>();
        PriorityQueue<Label> pq = new PriorityQueue<>();

        //seed PQ
        if (srcStop != null) {
            Label init = new Label(); init.stopId = srcStop; init.arrivalSec = startSec;
            best.put(srcStop, startSec);
            pq.add(init);
            //System.out.println("[LOG] seeded stop " + srcStop + "@" + startTime);
        } else {
            for (Stop s : graph.stops.values()) {
                double d = TransitGraph.haversine(srcLat, srcLon, s.lat, s.lon);
                int secs = (int)Math.round(d/5000.0*3600.0);
                Label init = new Label();
                init.stopId = s.stopId;
                init.arrivalSec = startSec + secs;
                init.viaWalk = new WalkEdge("__SRC__", s.stopId, secs);
                best.put(s.stopId, init.arrivalSec);
                pq.add(init);
            }
            //System.out.println("[LOG] seeded all stops via walk");
        }

        while (!pq.isEmpty()) {
            Label cur = pq.poll();
            if (cur.arrivalSec > best.getOrDefault(cur.stopId, Integer.MAX_VALUE)) continue;
            if (cur.arrivalSec >= bestDest) continue;
            //System.out.println("[LOG] visit " + cur.stopId + " @sec=" + cur.arrivalSec);

            //always attempt final walk
            Stop s = graph.stops.get(cur.stopId);
            double d2 = TransitGraph.haversine(s.lat, s.lon, dstLat, dstLon);
            int wsecs = (int)Math.round(d2/5000.0*3600.0);
            int arrivalAtDst = cur.arrivalSec + wsecs;
            if (arrivalAtDst < bestDest) {
                bestDest = arrivalAtDst;
                destLabel = cur;
                finalWalkSec = wsecs;
                //System.out.println("[LOG] final-walk via " + cur.stopId + " arrives=" + arrivalAtDst);
            }

            //walk edges
            for (WalkEdge we : graph.walkEdges) if (we.fromStopId.equals(cur.stopId)) {
                int arr = cur.arrivalSec + we.walkDurationSec;
                if (arr < best.getOrDefault(we.toStopId, Integer.MAX_VALUE)) {
                    Label nxt = new Label();
                    nxt.stopId = we.toStopId;
                    nxt.arrivalSec = arr;
                    nxt.prev = cur;
                    nxt.viaWalk = we;
                    best.put(nxt.stopId, arr);
                    pq.add(nxt);
                    //System.out.println("[LOG] relax walk " + we.fromStopId + "->" + we.toStopId + " arr=" + arr);
                }
            }

            //transit edges
            List<DepartureEvent> deps = graph.departures.getOrDefault(cur.stopId, Collections.emptyList());
            //System.out.println("[LOG] deps@" + cur.stopId + " size=" + deps.size());
            for (DepartureEvent ev : deps) {
                if (ev.departureSec < cur.arrivalSec) continue;
                int arr = ev.arrivalSec;
                if (arr < best.getOrDefault(ev.toStopId, Integer.MAX_VALUE)) {
                    Label nxt = new Label();
                    nxt.stopId = ev.toStopId;
                    nxt.arrivalSec = arr;
                    nxt.prev = cur;
                    nxt.viaEvent = ev;
                    best.put(nxt.stopId, arr);
                    pq.add(nxt);
                    //System.out.println("[LOG] relax ride " + cur.stopId + "->" + ev.toStopId + " dep=" + ev.departureSec + " arr=" + arr);
                }
            }
        }

        List<JourneyStep> steps = new ArrayList<>();
        if (destLabel == null) {
            //System.out.println("[LOG] direct walk fallback");
            steps.add(new JourneyStep(dstLat, dstLon, finalWalkSec/60, formatTime(startSec)));
            return steps;
        }

        
        List<JourneyStep> rev = new ArrayList<>();
        //add final walk if non-zero
        if (finalWalkSec > 0) {
            rev.add(new JourneyStep(dstLat, dstLon, finalWalkSec / 60,
                    formatTime(bestDest - finalWalkSec)));
        }
        //unwind/backtrack labels
        for (Label cur = destLabel; cur != null; cur = cur.prev) {
            if (cur.viaEvent != null) {
                Stop st = graph.stops.get(cur.stopId);
                int dur = (cur.arrivalSec - cur.viaEvent.departureSec) / 60;
                rev.add(new JourneyStep(st.lat, st.lon, dur,
                        formatTime(cur.viaEvent.departureSec),
                        st.stopName, cur.viaEvent.routeInfo));
            } else if (cur.viaWalk != null) {
                // Walking leg
                Stop toStop = graph.stops.get(cur.viaWalk.toStopId);
                rev.add(new JourneyStep(toStop.lat, toStop.lon,
                        cur.viaWalk.walkDurationSec / 60,
                        formatTime(cur.arrivalSec - cur.viaWalk.walkDurationSec)));
            }
        }
        Collections.reverse(rev);
        steps.addAll(rev);
        return steps;
    }

    private static String formatTime(int sec) {
        sec %= 86400;
        if (sec < 0) sec += 86400;
        int hh = sec/3600;
        int mm = (sec%3600)/60;
        //int ss = sec%60;
        return String.format("%02d:%02d", hh, mm);
    }

    private int parsetime(String hhmm) {
        String[] parts = hhmm.split(":");
        try {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            //int seconds = Integer.parseInt(parts[2]);
            if (minutes < 0 || minutes >= 60 ||  hours < 0) {
                throw new IllegalArgumentException("Time values out of range: " + hhmm);
            }
            return hours * 3600 + minutes * 60;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric value in time: " + hhmm, e);
        }
    }
}
