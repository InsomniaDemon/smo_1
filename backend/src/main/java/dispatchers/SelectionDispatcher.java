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

    // Вызывается после постановки заявки в буфер и после освобождения прибора.
    // Д2П1: первый свободный прибор по номеру. Д2Б3: выбор заявки по кольцу.
    public void dispatch(double time) {
        while (buffer.hasRequests()) {
            Device freeDevice = findFreeDevice();
            if (freeDevice == null) {
                System.out.println("ДВ: свободных приборов нет. Заявки остаются в буфере.");
                return;
            }

            Request request = buffer.selectByRing();
            if (request == null) return;

            System.out.printf("ДВ: по Д2Б3 выбрана заявка %s; указатель выборки теперь на позиции %d.%n",
                    request.id(), buffer.getSelectPointer() + 1);

            double finishTime = freeDevice.startProcessing(
                    request, time, serviceMin, serviceMax, random
            );

            statistics.recordServiceStart(request, freeDevice, time);
            calendar.addDeviceRelease(finishTime, freeDevice.id, request);

            System.out.printf("ДВ: заявка %s отправлена на П%d; обслуживание U[%.3f; %.3f], " +
                            "окончание в t=%.4f.%n",
                    request.id(), freeDevice.id, serviceMin, serviceMax, finishTime);
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