package event;

import request.Request;

public class Event implements Comparable<Event> {
    public final double time;
    private final long order;
    public final EventType type;
    public final int sourceId;
    public final int deviceId;

    public Event(double time, long order, EventType type, int sourceId, int deviceId) {
        this.time = time;
        this.order = order;
        this.type = type;
        this.sourceId = sourceId;
        this.deviceId = deviceId;
    }

    @Override
    public int compareTo(Event other) {
        int byTime = Double.compare(time, other.time);
        return byTime != 0 ? byTime : Long.compare(order, other.order);
    }
}