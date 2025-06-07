package storage;

import java.util.*;
import structs.*;
import DB.*;


//holds all stops, departure events, and walking edges for routing

public class TransitGraph {
    public final Map<String, Stop> stops = new HashMap<>();
    public final Map<String, List<DepartureEvent>> departures = new HashMap<>();
    public final List<WalkEdge> walkEdges = new ArrayList<>();
    public String deletedStop = null;
    public final List<DepartureEvent> additionalDepartureEvents = new ArrayList<>();

    public void computeAllWalkEdges() {
        List<Stop> stopList = new ArrayList<>(stops.values());
        for (int i = 0; i < stopList.size(); i++) {
            Stop a = stopList.get(i);
            for (int j = i + 1; j < stopList.size(); j++) {
                Stop b = stopList.get(j);
                double dist = haversine(a.lat, a.lon, b.lat, b.lon);
                if(dist <= 5000){
                    int secs = (int) Math.ceil(dist / 5000.0 * 3600.0);
                    walkEdges.add(new WalkEdge(a.stopId, b.stopId, secs));
                    walkEdges.add(new WalkEdge(b.stopId, a.stopId, secs));
                }
            }
        }
    }
    //legacy method, might be useful in the future
    public List<WalkEdge> getNearestWalkEdges(double lat, double lon, int maxResults) {
        PriorityQueue<WalkEdge> pq = new PriorityQueue<>(Comparator.comparingInt(e->e.walkDurationSec));
        for (Stop s : stops.values()) {
            double dist = haversine(lat, lon, s.lat, s.lon);
            if (dist <= 5000.0) {
                int secs = (int) Math.round(dist / 5000.0 * 3600.0);
                pq.offer(new WalkEdge("__SRC__", s.stopId, secs));
            }
        }
        List<WalkEdge> out = new ArrayList<>();
        for (int i = 0; i < maxResults && !pq.isEmpty(); i++) out.add(pq.poll());
        return out;
    }

    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000; 
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                 + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon/2)*Math.sin(dLon/2);
        double c = 2*Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    public void deleteStop(String stopId){
        DBManager dbManager = new DBManager();
        for(DepartureEvent event : dbManager.getDepartureEventsAfterStopDeletion(stopId)){
            departures.get(event.fromStopId).add(event);
            additionalDepartureEvents.add(event);
        }
        deletedStop = String.valueOf(stopId);
    }

    public void reloadDeletedStop(){
        for(DepartureEvent event : additionalDepartureEvents){
            departures.get(event.fromStopId).remove(event);
        }
        additionalDepartureEvents.clear();
        deletedStop = null;
    }
}