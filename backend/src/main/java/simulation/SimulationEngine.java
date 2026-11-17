package simulation;

import dispatchers.PlacementDispatcher;
import dispatchers.SelectionDispatcher;
import event.Event;
import event.EventCalendar;
import event.EventType;
import request.Request;
import statistics.StatisticsCollector;

import java.util.*;

public class SimulationEngine {
    private final SimulationConfig config;
    private final Scanner scanner;
    private final Random random = new Random();
    private final List<Source> sources = new ArrayList<>();
    private final List<Device> devices = new ArrayList<>();
    private final EventCalendar calendar = new EventCalendar();
    private final Buffer buffer;
    private final StatisticsCollector statistics;
    private final PlacementDispatcher placementDispatcher;
    private final SelectionDispatcher selectionDispatcher;
    private double currentTime;

    public SimulationEngine(SimulationConfig config, Scanner scanner) {
        this.config = config;
        this.scanner = scanner;

        for (int i = 1; i <= config.sourceCount(); i++) {
            sources.add(new Source(i, config.lambda()));
        }
        for (int i = 1; i <= config.deviceCount(); i++) {
            devices.add(new Device(i));
        }

        buffer = new Buffer(config.bufferCapacity());
        statistics = new StatisticsCollector(config.sourceCount());
        placementDispatcher = new PlacementDispatcher(buffer, statistics);
        selectionDispatcher = new SelectionDispatcher(
                buffer, devices, calendar, statistics,
                config.serviceMin(), config.serviceMax(), random
        );
    }

    public void run() {
        // Планируем первые поступления от всех бесконечных источников.
        for (Source source : sources) {
            double firstTime = source.getNextGenerationTime(0, random);
            calendar.addGeneration(firstTime, source.id);
        }

        printInitialState();

        while (!calendar.isEmpty()) {
            Event event = calendar.next();
            currentTime = event.time;

            System.out.println();
            System.out.println("============================================================");
            System.out.printf("СЛЕДУЮЩЕЕ СОБЫТИЕ: t = %.4f | %s%n",
                    currentTime, event.description());
            System.out.println("Нажмите Enter для обработки события...");
            scanner.nextLine();

            if (event.type == EventType.REQUEST_GENERATED) {
                processGeneration(event);
            } else {
                processDeviceRelease(event);
            }

            printState();
        }

        System.out.println();
        System.out.println("Календарь событий пуст. Моделирование завершено.");
        statistics.printSummary(devices, currentTime);
    }

    private void processGeneration(Event event) {
        Source source = sources.get(event.sourceId - 1);
        Request request = source.generate(currentTime);
        statistics.recordGenerated(request);

        System.out.printf("СОБЫТИЕ: источник И%d сгенерировал заявку %s.%n",
                source.id, request.id());

        // Бесконечный источник моделируется ограниченным числом генераций
        // для завершения учебного эксперимента.
        if (source.generatedCount < config.requestsPerSource()) {
            double nextTime = source.getNextGenerationTime(currentTime, random);
            calendar.addGeneration(nextTime, source.id);
            System.out.printf("И%d: следующая генерация запланирована на t=%.4f.%n",
                    source.id, nextTime);
        } else {
            System.out.printf("И%d: достигнут лимит генераций (%d).%n",
                    source.id, config.requestsPerSource());
        }

        placementDispatcher.placeRequest(request, currentTime);
        selectionDispatcher.dispatch(currentTime);
    }

    private void processDeviceRelease(Event event) {
        Device device = devices.get(event.deviceId - 1);
        Request completed = device.finishProcessing();

        if (completed == null) {
            throw new IllegalStateException("Событие освобождения для свободного прибора П"
                    + device.id);
        }

        statistics.recordCompleted(completed, device, currentTime);
        System.out.printf("СОБЫТИЕ: П%d завершил обработку заявки %s. " +
                        "Время обслуживания = %.4f.%n",
                device.id, completed.id(), completed.serviceDuration);

        selectionDispatcher.dispatch(currentTime);
    }

    private void printInitialState() {
        System.out.println("НАЧАЛЬНОЕ СОСТОЯНИЕ");
        printState();
    }

    private void printState() {
        System.out.println();
        System.out.printf("Текущее модельное время: %.4f%n", currentTime);
        printTemporalDiagram();

        System.out.println("\nИсточники:");
        for (Source source : sources) {
            System.out.printf("И%d: сгенерировано %d / %d%n",
                    source.id, source.generatedCount, config.requestsPerSource());
        }

        System.out.println("\nБуфер:");
        System.out.println(buffer.describe());
        System.out.printf("Занято: %d/%d | Указатель постановки Д1ОЗ1: %d | " +
                        "Указатель выборки Д2Б3: %d%n",
                buffer.size(), buffer.getCapacity(),
                buffer.getPutPointer() + 1, buffer.getSelectPointer() + 1);

        System.out.println("\nWorker-процессы:");
        for (Device device : devices) {
            if (device.isFree()) {
                System.out.printf("П%d: СВОБОДЕН%n", device.id);
            } else {
                System.out.printf("П%d: обрабатывает %s, завершение в t=%.4f%n",
                        device.id, device.currentRequest.id(), device.busyUntil);
            }
        }

        printCalendar();
    }

    /**
     * ОД3: временные диаграммы динамики работы системы.
     * По горизонтали отложено модельное время; строки показывают моменты
     * генерации заявок источниками и интервалы занятости приборов.
     */
    private void printTemporalDiagram() {
        final int width = 64;
        double diagramEnd = currentTime;
        for (Device device : devices) {
            diagramEnd = Math.max(diagramEnd, device.busyUntil);
        }
        if (diagramEnd <= 0.0) diagramEnd = 1.0;

        System.out.println("\nВРЕМЕННЫЕ ДИАГРАММЫ (ОД3)");
        System.out.printf("Интервал: t=0.0000 .. %.4f | текущий момент: %.4f%n",
                diagramEnd, currentTime);
        System.out.println("Метки источников: * — генерация заявки");
        System.out.println("Приборы: [номер заявки] — обслуживание; >>> — выполняется сейчас; " +
                "| — текущее модельное время");

        for (Source source : sources) {
            char[] line = blankTimeline(width);
            for (double eventTime : source.generationTimes) {
                int x = timelinePosition(eventTime, diagramEnd, width);
                line[x] = '*';
            }
            markCurrentTime(line, diagramEnd, width);
            System.out.printf("И%-3d %s%n", source.id, new String(line));
        }

        for (Device device : devices) {
            char[] line = blankTimeline(width);
            for (ServiceInterval interval : device.serviceHistory) {
                int from = timelinePosition(interval.start(), diagramEnd, width);
                int to = timelinePosition(interval.finish(), diagramEnd, width);
                if (to < from) to = from;
                for (int x = from; x <= to; x++) line[x] = '=';

                // Номер заявки ставится в начале интервала, если хватает места.
                String id = interval.request().id();
                for (int j = 0; j < id.length() && from + j < width && from + j <= to; j++) {
                    line[from + j] = id.charAt(j);
                }
                if (interval.start() <= currentTime && currentTime < interval.finish()) {
                    int activeFrom = Math.max(from, timelinePosition(interval.start(), diagramEnd, width));
                    int activeTo = Math.min(to, timelinePosition(currentTime, diagramEnd, width));
                    for (int x = activeFrom; x <= activeTo; x++) {
                        if (line[x] == '=') line[x] = '>';
                    }
                }
            }
            markCurrentTime(line, diagramEnd, width);
            System.out.printf("П%-3d %s%n", device.id, new String(line));
        }

        System.out.print("     ");
        System.out.printf("0%-" + Math.max(1, width - 12) + "s%.4f%n", "", diagramEnd);
        System.out.println("     " + " ".repeat(Math.min(width, timelinePosition(currentTime, diagramEnd, width))) + "^ текущее время");
    }

    private char[] blankTimeline(int width) {
        char[] line = new char[width];
        Arrays.fill(line, '.');
        return line;
    }

    private int timelinePosition(double time, double end, int width) {
        if (width <= 1 || end <= 0) return 0;
        return Math.max(0, Math.min(width - 1,
                (int) Math.round(time / end * (width - 1))));
    }

    private void markCurrentTime(char[] line, double end, int width) {
        int x = timelinePosition(currentTime, end, width);
        line[x] = '|';
    }

    private void printCalendar() {
        System.out.println("\nКалендарь будущих событий:");
        List<Event> events = calendar.snapshot();
        if (events.isEmpty()) {
            System.out.println("(пуст)");
            return;
        }
        System.out.printf("%-12s %-28s%n", "Время", "Событие");
        for (Event event : events) {
            System.out.printf("%-12.4f %-28s%n", event.time, event.description());
        }
    }
}