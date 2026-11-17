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
        System.out.printf("ДП: постановка заявки %s в буфер (Д1ОЗ1).%n", request.id());
        int position = buffer.findFreeSlot();

        if (position >= 0) {
            buffer.put(request, position);
            System.out.printf("ДП: свободная позиция %d найдена, заявка размещена.%n", position + 1);
        } else {
            System.out.println("ДП: буфер заполнен. Применяется Д1ОО3.");
            int oldestPosition = buffer.findOldestSlot();
            Request rejected = buffer.eject(oldestPosition);
            if (rejected != null) {
                statistics.recordRejected(rejected);
                System.out.printf("ДП: выбита самая старая заявка %s (позиция %d).%n",
                        rejected.id(), oldestPosition + 1);
            }
            buffer.put(request, oldestPosition);
            System.out.printf("ДП: новая заявка %s заняла позицию %d.%n",
                    request.id(), oldestPosition + 1);
        }

        System.out.printf("ДП: указатель постановки теперь на позиции %d.%n",
                buffer.getPutPointer() + 1);
    }
}