package structs;

public class WalkEdge {
    public final String fromStopId;
    public final String toStopId;
    public final int walkDurationSec;

    public WalkEdge(String fromStopId,
                    String toStopId,
                    int walkDurationSec) {
        this.fromStopId = fromStopId;
        this.toStopId = toStopId;
        this.walkDurationSec = walkDurationSec;
    }
}