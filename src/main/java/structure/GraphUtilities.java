package structure;

import structure.stops.StopStructure;

public class GraphUtilities {
    private static double radiusKM = 6378.1370;

    public static double distanceBetweenPoints(StopStructure s1, StopStructure s2){
        double latRadians1 = Math.toRadians(s1.getLatitude());
        double latRadians2 = Math.toRadians(s2.getLatitude());
        double latDiff = latRadians2 - latRadians1;
        double longDiff = Math.toRadians(s2.getLongitude()) - Math.toRadians(s1.getLongitude());

        double a = Math.pow(Math.sin(latDiff / 2.0), 2) + Math.cos(latRadians1)*Math.cos(latRadians2) + Math.pow(Math.sin(longDiff / 2.0), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return Math.abs(radiusKM * c);
    }

    public static int calculateStaticCostMinutes(StopStructure s1, StopStructure s2, double speed){
        return (int) Math.round(distanceBetweenPoints(s1, s2) / speed);
    }

//    private double latitudeToKilometers(double latitudeDiff){
//        return latitudeDiff * latitudeToKilometerConstant;
//    }
//
//    private double longitudeToKilometers(double longitudeDiff, double latitude){
//        return longitudeToKilometerConstant * Math.cos(Math.toRadians(latitude)) * longitudeDiff;
//    }
}
