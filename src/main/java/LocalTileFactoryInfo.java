
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.viewer.TileFactoryInfo;

public class LocalTileFactoryInfo extends TileFactoryInfo {
    private static final int MAX_ZOOM = 16;

    public LocalTileFactoryInfo(String baseURL) {
        super("Local Tile Factory",3, 6, 19, 256, true, true, baseURL, "x", "y", "z");
    }

    @Override
    public String getTileUrl(int x, int y, int zoom) {
        int invZoom = 19 - zoom;
        int flippedY = (1 << invZoom) - 1 - y;
        return this.baseURL+ "/" + invZoom + "/" + x + "/" + y + ".png";
    }

}
