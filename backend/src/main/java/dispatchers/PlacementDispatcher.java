package dispatchers;

import request.Request;
import simulation.Buffer;
import statistics.StatisticsCollector;

public class PlacementDispatcher {
    private final Buffer buffer;
    private final StatisticsCollector statistics;

    public PlacementDispatcher(Buffer buffer, StatisticsCollector statistics) {
        this.buffer = buffer;
        this.statistics = statistics;
    }

    public void placeRequest(Request request, double time) {
        int position = buffer.findFreeSlot();

        if (position >= 0) {
            buffer.put(request, position);
        } else {
            int oldestPosition = buffer.findOldestSlot();
            Request rejected = buffer.eject(oldestPosition);
            if (rejected != null) {
                statistics.recordRejected(rejected);
            }
            buffer.put(request, oldestPosition);
        }
    }
}