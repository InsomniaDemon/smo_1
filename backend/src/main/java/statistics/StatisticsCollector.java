package statistics;

import request.Request;
import request.RequestStatus;
import simulation.Device;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StatisticsCollector {
    private final Map<Integer, SourceStatistics> bySource = new LinkedHashMap<>();
    public StatisticsCollector(int sourceCount) {
        for (int i = 1; i <= sourceCount; i++) bySource.put(i, new SourceStatistics());
    }

    public void recordGenerated(Request request) {
        bySource.get(request.sourceId).recordGenerated();
    }

    public void recordRejected(Request request) {
        bySource.get(request.sourceId).recordRejected();
    }

    public void recordServiceStart(Request request, Device device, double time) {
        bySource.get(request.sourceId).recordServiceStart(request, time);
    }

    public void recordCompleted(Request request, Device device, double time) {
        request.completedAt = time;
        request.status = RequestStatus.COMPLETED;
        bySource.get(request.sourceId).recordCompleted(request, time);
    }

    public void printSummary(List<Device> devices, double totalTime) {
        System.out.println();
        System.out.println("================ ИТОГОВАЯ СТАТИСТИКА (ОР1) ================");
        System.out.printf("%-9s %9s %9s %9s %11s %11s %11s %11s %11s%n",
                "Источник", "Создано", "Отказы", "P отк",
                "T ож ср.", "T обсл ср.", "Tсист ср.", "D ож", "D обсл");

        long generated = 0;
        long rejected = 0;
        long completed = 0;

        for (Map.Entry<Integer, SourceStatistics> entry : bySource.entrySet()) {
            int sourceId = entry.getKey();
            SourceStatistics s = entry.getValue();
            generated += s.generated;
            rejected += s.rejected;
            completed += s.completed;

            System.out.printf("%-9s %9d %9d %8.2f%% %11.4f %11.4f %11.4f %11.4f %11.4f%n",
                    "И" + sourceId, s.generated, s.rejected,
                    s.rejectionProbability() * 100,
                    s.averageWaiting(), s.averageService(), s.averageSystemTime(),
                    s.waitingVariance(), s.serviceVariance());
        }

        System.out.println("--------------------------------------------------------------------------");
        System.out.printf("Всего сгенерировано: %d%n", generated);
        System.out.printf("Всего обработано:    %d%n", completed);
        System.out.printf("Всего отказов:       %d%n", rejected);
        System.out.printf("Общая вероятность отказа: %.2f%%%n",
                generated == 0 ? 0 : 100.0 * rejected / generated);
        System.out.printf("Общее время реализации: %.4f%n", totalTime);
        System.out.println();
        System.out.println("Загрузка worker-процессов:");

        for (Device device : devices) {
            double utilization = totalTime == 0 ? 0 : device.totalBusyTime / totalTime;
            System.out.printf("П%d: занято %.4f, загрузка %.2f%%%n",
                    device.id, device.totalBusyTime, utilization * 100);
        }
        System.out.println("============================================================");
    }
}
