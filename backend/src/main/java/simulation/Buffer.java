package simulation;

import request.Request;
import request.RequestStatus;

public class Buffer {
    private final Request[] slots;
    private int putPointer;
    private int selectPointer;

    Buffer(int capacity) {
        slots = new Request[capacity];
    }

    // Д1ОЗ1: поиск свободной позиции по кольцу от putPointer.
    public int findFreeSlot() {
        for (int offset = 0; offset < slots.length; offset++) {
            int index = (putPointer + offset) % slots.length;
            if (slots[index] == null) return index;
        }
        return -1;
    }

    public void put(Request request, int position) {
        slots[position] = request;
        request.status = RequestStatus.IN_BUFFER;
        putPointer = (position + 1) % slots.length;
    }

    // Д1ОО3: выбирается заявка с самым ранним временем поступления.
    public int findOldestSlot() {
        int oldestIndex = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null) continue;
            if (oldestIndex == -1
                    || slots[i].createdAt < slots[oldestIndex].createdAt
                    || (Double.compare(slots[i].createdAt, slots[oldestIndex].createdAt) == 0
                    && slots[i].sequenceNumber < slots[oldestIndex].sequenceNumber)) {
                oldestIndex = i;
            }
        }
        return oldestIndex;
    }

    public Request eject(int position) {
        Request removed = slots[position];
        slots[position] = null;
        if (removed != null) removed.markRejected();
        return removed;
    }

    // Д2Б3: поиск первой занятой позиции по кольцу от selectPointer.
    public Request selectByRing() {
        for (int offset = 0; offset < slots.length; offset++) {
            int index = (selectPointer + offset) % slots.length;
            if (slots[index] != null) {
                Request selected = slots[index];
                slots[index] = null;
                selectPointer = (index + 1) % slots.length;
                return selected;
            }
        }
        return null;
    }

    public boolean hasRequests() {
        for (Request request : slots) {
            if (request != null) return true;
        }
        return false;
    }

    int size() {
        int count = 0;
        for (Request request : slots) if (request != null) count++;
        return count;
    }

    String describe() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < slots.length; i++) {
            sb.append(String.format("%2d: ", i + 1));
            sb.append(slots[i] == null ? "—" : slots[i].id());
            if (i < slots.length - 1) sb.append(" | ");
        }
        return sb.toString();
    }

    int getCapacity() {
        return slots.length;
    }

    public int getPutPointer() {
        return putPointer;
    }

    public int getSelectPointer() {
        return selectPointer;
    }
}