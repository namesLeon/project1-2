package structs;

public class StopTime {
    public final String tripId;
    public final String stopId;
    public final int arrivalSec;   
    public final int departureSec; 
    public final int stopSequence; 

    public StopTime(String tripId,
                    String stopId,
                    int arrivalSec,
                    int departureSec,
                    int stopSequence) {
        this.tripId = tripId;
        this.stopId = stopId;
        this.arrivalSec = arrivalSec;
        this.departureSec = departureSec;
        this.stopSequence = stopSequence;
    }

    public static int parseTime(String hhmmss) {
        String[] parts = hhmmss.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid time format: " + hhmmss);
        }
        try {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = Integer.parseInt(parts[2]);
            if (minutes < 0 || minutes >= 60 || seconds < 0 || seconds >= 60 || hours < 0) {
                throw new IllegalArgumentException("Time values out of range: " + hhmmss);
            }
            return hours * 3600 + minutes * 60 + seconds;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric value in time: " + hhmmss, e);
        }
    }

    @Override
    public String toString() {
        return String.format("StopTime{tripId='%s', stopId='%s', arr=%d, dep=%d, seq=%d}",
                             tripId, stopId, arrivalSec, departureSec, stopSequence);
    }
}