package gtfsparsingzip;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        GTFSLoader loader = new GTFSLoader("C:\\Users\\larry\\OneDrive\\Year 1\\Project1_2\\GTFS\\GT"); // Put your gtfs.zip path here
        loader.filterBerlinStops();
    }
}