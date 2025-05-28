import java.io.IOException;
import java.util.List;
import java.util.Map;

import data.*;
import loaders.GTFSLoader;

public class Main {
    public static void main(String[] args) throws IOException {
        GTFSLoader loader = new GTFSLoader("C:\\Users\\xxxx\\OneDrive\\Project1_2\\GTFS\\GTFS.zip"); // Put your gtfs.zip path here
        loader.filterBerlinStops();
    }
}