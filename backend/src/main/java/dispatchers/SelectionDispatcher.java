package dispatchers;

import event.EventCalendar;
import request.Request;
import simulation.Buffer;
import simulation.Device;
import statistics.StatisticsCollector;

import java.util.List;
import java.util.Random;

public class SelectionDispatcher {
    private final Buffer buffer;
    private final List<Device> devices;
    private final EventCalendar calendar;
    private final StatisticsCollector statistics;
    private final double serviceMin;
    private final double serviceMax;
    private final Random random;

    public SelectionDispatcher(Buffer buffer, List<Device> devices,
                               EventCalendar calendar, StatisticsCollector statistics,
                               double serviceMin, double serviceMax, Random random) {
        this.buffer = buffer;
        this.devices = devices;
        this.calendar = calendar;
        this.statistics = statistics;
        this.serviceMin = serviceMin;
        this.serviceMax = serviceMax;
        this.random = random;
    }

    // Вызывается после постановки заявки в буфер или после освобождения прибора.
    // Д2П1: первый свободный прибор по номеру. Д2Б3: выбор заявки по кольцу.
    public void dispatch(double time) {
        while (buffer.hasRequests()) {
            Device freeDevice = findFreeDevice();
            if (freeDevice == null) {
                return;
            }

            Request request = buffer.selectByRing();
            if (request == null) return;

            double finishTime = freeDevice.startProcessing(
                    request, time, serviceMin, serviceMax, random
            );

            statistics.recordServiceStart(request, freeDevice, time);
            calendar.addDeviceRelease(finishTime, freeDevice.id, request);
        }
    }

    private Device findFreeDevice() {
        // Д2П1: каждый поиск начинается с П1.
        for (Device device : devices) {
            if (device.isFree()) return device;
        }
        return null;
    }
}