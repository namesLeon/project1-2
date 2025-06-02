package structure;

import gtfsparsingzip.data.Trip;
import structure.edges.DynamicEdge;
import structure.edges.Edge;
import structure.edges.StaticEdge;
import structure.stops.DynamicStop;
import structure.stops.StaticStop;
import structure.stops.StopStructure;

import javax.swing.plaf.nimbus.State;
import javax.xml.transform.Result;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Graph {
    private final String START_ID = "0";
    private final String END_ID = "1";
    private final HashMap<String, StopStructure> stopMap;
    private final HashMap<StopStructure, HashMap<StopStructure, Edge>> adjMap;

    private Connection conn;

    private final ArrayList<StopStructure> stopList;
    private final double speed = 5;

    public Graph(/*Connection conn*/){
        this.stopMap = new HashMap<>();
        this.adjMap = new HashMap<>();
        this.stopList = new ArrayList<>();
        this.conn = conn;
    }

    // Possible exception here
    public StopStructure getStopById(String id){
        return stopMap.getOrDefault(id, null);
    }

    public boolean isAdjacent(StopStructure start, StopStructure end){
        if(!stopMap.containsValue(start)) return false;
        return adjMap.get(start).containsKey(end);
    }

    public void addStop(StopStructure s){
        stopMap.putIfAbsent(s.getStopId(), s);
        adjMap.putIfAbsent(s, new HashMap<>());
        stopList.add(s);
    }

    public void addDynamicConnection(StopStructure start, StopStructure end, LocalTime time, Trip trip, int rideCostMinutes){
        if(!isAdjacent(start, end)){
            //addDynamicEdge(start, end, GraphUtilities.calculateStaticCostMinutes(start, end, speed));
            adjMap.get(start).putIfAbsent(end, new DynamicEdge(start, end, rideCostMinutes));
        }
        Edge edge= getEdge(start, end);
        if(edge instanceof DynamicEdge){
            ((DynamicEdge)edge).addConnection(time, trip, rideCostMinutes);
        }
    }

//    private void addDynamicEdge(StopStructure start, StopStructure end, int walkingCostMinutes){
//        adjMap.get(start).putIfAbsent(end, new DynamicEdge(start, end, walkingCostMinutes));
//    }

    public void addStaticConnection( StopStructure start, StopStructure end){
        if(!isAdjacent(start, end)){
            addStaticEdge(start, end, GraphUtilities.calculateStaticCostMinutes(start, end, speed));
        }
    }

    private void addStaticEdge(StopStructure start, StopStructure end, int walkingCostMinutes){
        adjMap.get(start).putIfAbsent(end, new StaticEdge(start, end, walkingCostMinutes));
    }

    public Edge getEdge(String startId, String endId){
        return getEdge(getStopById(startId), getStopById(endId));
    }

    public Edge getEdge(StopStructure start, StopStructure end){
        return adjMap.get(start).get(end);
    }

    public HashMap<StopStructure, Edge> getAdjacentStops(StopStructure start){
        adjMap.putIfAbsent(start, new HashMap<>());
        return adjMap.get(start);
    }

    public HashMap<String, StopStructure> getStopMap() {
        return stopMap;
    }

    public HashMap<StopStructure, HashMap<StopStructure, Edge>> getAdjMap() {
        return adjMap;
    }

    public ArrayList<StopStructure> getStopList(){
        return stopList;
    }

    public void connectNeighbouringVertices(int walkingTimeLimit, double speed){
        for(Map.Entry<StopStructure, HashMap<StopStructure, Edge>> e1 : adjMap.entrySet()){
            StopStructure start = e1.getKey();
            HashMap<StopStructure, Edge> adj = e1.getValue();
            for(Map.Entry<StopStructure, HashMap<StopStructure, Edge>> e2 : adjMap.entrySet()){
                StopStructure end = e2.getKey();

                if(!start.equals(end) && !isAdjacent(start, end)){
                    int walkingCostMinutes = GraphUtilities.calculateStaticCostMinutes(start, end, speed);
                    if(walkingCostMinutes <= walkingTimeLimit) {
                        addStaticEdge(start, end, walkingCostMinutes);
                    }
                }

            }
        }
    }

    public void addEndNode(double lat, double longt){
        StaticStop endStop = new StaticStop(lat, longt, END_ID);
        addStop(endStop);
//        for(Map.Entry<StopStructure, HashMap<StopStructure, Edge>> e : adjMap.entrySet()){
//            StopStructure stop1 = e.getKey();
//            if(!endStop.equals(stop1)){
//                int walkingCostMinutes = GraphUtilities.calculateStaticCostMinutes(stop1, endStop, speed);
//                stop1.setDistanceToGoal(walkingCostMinutes);
//                addStaticEdge(stop1, endStop, walkingCostMinutes);
//            }
//        }
    }

    public void setHeuristic(StopStructure endStop){
        for(Map.Entry<StopStructure, HashMap<StopStructure, Edge>> e : adjMap.entrySet()){
            StopStructure stop1 = e.getKey();
            if(!endStop.equals(stop1)){
                int walkingCostMinutes = GraphUtilities.calculateStaticCostMinutes(stop1, endStop, speed);
                stop1.setDistanceToGoal(walkingCostMinutes);
                addStaticEdge(stop1, endStop, walkingCostMinutes);
            }
        }
    }

    public void addStartNode(double lat, double longt){
        StaticStop startStop = new StaticStop(lat, longt, START_ID);
        addStop(startStop);
        for(Map.Entry<StopStructure, HashMap<StopStructure, Edge>> e : adjMap.entrySet()){
            StopStructure stop2 = e.getKey();
            if(!startStop.equals(stop2)){
                int walkingCostMinutes = GraphUtilities.calculateStaticCostMinutes(startStop, stop2, speed);
                addStaticEdge(startStop, stop2, walkingCostMinutes);
            }
        }
    }

    private void createGraphFromDB(){

    }

    private void createStopsFromDB() throws SQLException {
        Statement st = conn.createStatement();
        String fetchStopsFromDB = """
                SELECT * FROM stops
                """;
        ResultSet rs = st.executeQuery(fetchStopsFromDB);
        while(rs.next()){
            String stopId = rs.getString(1);
            double stopLatitude = rs.getDouble(3);
            double stopLongitude = rs.getDouble(4);
            stopList.add(new DynamicStop(stopLatitude, stopLongitude ,stopId));
        }
    }

//    private void createEdgesFromDB() throws SQLException{
//        Statement st = conn.createStatement();
//        String fetchEdgesFromDB = """
//                SELECT * FROM edges
//                """;
//        ResultSet rs = st.executeQuery(fetchEdgesFromDB);
//        while(rs.next()){
//
//        }
//    }
}
