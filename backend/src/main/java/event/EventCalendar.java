package event;

import request.Request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public class EventCalendar {
    private final PriorityQueue<Event> events = new PriorityQueue<>();
    private long nextOrder;

    public void addGeneration(double time, int sourceId) {
        events.add(new Event(time, nextOrder++, EventType.REQUEST_GENERATED,
                sourceId, -1, null));
    }

    public void addDeviceRelease(double time, int deviceId, Request request) {
        events.add(new Event(time, nextOrder++, EventType.DEVICE_RELEASED,
                -1, deviceId, request));
    }

    public Event next() {
        return events.poll();
    }

    public boolean isEmpty() {
        return events.isEmpty();
    }

    public List<Event> snapshot() {
        List<Event> result = new ArrayList<>(events);
        Collections.sort(result);
        return result;
    }
}