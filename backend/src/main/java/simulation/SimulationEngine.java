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
    private static final int WIDTH = 100;
    private static final double WINDOW_DURATION = 2.5;
    private static final double CURRENT_TIME_POSITION = 0.75;

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
        for (Source source : sources) {
            double firstTime = source.getNextGenerationTime(0, random);
            calendar.addGeneration(firstTime, source.id);
        }

        boolean batch = config.automatic();

        if (!batch) {
            System.out.println("НАЧАЛЬНОЕ СОСТОЯНИЕ");
            printState();
        }

        while (!calendar.isEmpty()) {
            Event event = calendar.next();
            currentTime = event.time;

            if (event.type == EventType.REQUEST_GENERATED) {
                processGeneration(event);
            } else {
                processDeviceRelease(event);
            }

            if (!batch) {
                System.out.println("============================================================");
                System.out.println("Enter - следующий шаг, а - выполнить все оставшиеся шаги автоматически");
                if(scanner.nextLine().equals("a")) {
                    batch = true;
                    continue;
                }

                printState();
            }
        }

        if (!batch) {
            System.out.println("\nКалендарь событий пуст. Моделирование завершено.\n");
        }
        statistics.printSummary(devices, currentTime);
    }

    private void processGeneration(Event event) {
        Source source = sources.get(event.sourceId - 1);
        Request request = source.generate(currentTime);
        statistics.recordGenerated(request);

        if (source.generatedCount < config.requestsPerSource()) {
            double nextTime = source.getNextGenerationTime(currentTime, random);
            calendar.addGeneration(nextTime, source.id);
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

        selectionDispatcher.dispatch(currentTime);
    }

    private void printState() {
        System.out.printf("Текущее модельное время: %.4f%n", currentTime);
        printTemporalDiagram();

        System.out.println("\nИсточники:");
        for (Source source : sources) {
            System.out.printf("И%d: сгенерировано %d / %d%n",
                    source.id, source.generatedCount, config.requestsPerSource());
        }

        System.out.println("\nБуфер:");
        System.out.println(buffer.describe());
        System.out.printf("Занято: %d/%d | Указатель постановки: %d | " +
                        "Указатель выборки: %d%n",
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
    }

    private void printTemporalDiagram() {
        final double windowDuration = WINDOW_DURATION;

        double windowStart = currentTime - windowDuration * CURRENT_TIME_POSITION;
        double windowEnd = windowStart + windowDuration;

        int currentTimePosition = (int) (WIDTH * CURRENT_TIME_POSITION);

        System.out.printf(
                "Окно: %.4f .. %.4f | Текущее время: %.4f%n",
                windowStart, windowEnd, currentTime
        );

        for (Source source : sources) {
            char[] line = blankTimeline();

            markCurrentTime(line, windowStart, windowEnd);

            for (double eventTime : source.generationTimes) {
                if (eventTime < windowStart || eventTime > windowEnd) {
                    continue;
                }

                int x = timelinePosition(
                        eventTime, windowStart, windowEnd
                );

                line[x] = '*';
            }

            System.out.printf("И%-3d %s%n", source.id, new String(line));
        }

        for (Device device : devices) {
            char[] line = blankTimeline();

            for (ServiceInterval interval : device.serviceHistory) {
                double visibleStart = Math.max(
                        interval.start(), windowStart
                );

                double visibleFinish = Math.min(
                        interval.finish(), windowEnd
                );

                if (visibleStart >= visibleFinish) {
                    continue;
                }

                int from = timelinePosition(
                        visibleStart, windowStart, windowEnd
                );

                int to = timelinePosition(
                        visibleFinish, windowStart, windowEnd
                );

                // Отображаем интервал обслуживания.
                for (int x = from; x <= to; x++) {
                    if (x < currentTimePosition && interval.start() <= currentTime && currentTime < interval.finish()) {
                        line[x] = '>';
                    } else {
                        line[x] = '=';
                    }
                }

                // Возвращаем ID заявки в начало интервала.
                if (interval.start() >= windowStart
                        && interval.start() <= windowEnd) {

                    String id = interval.request().id();

                    for (int j = 0;
                         j < id.length() && from + j <= to && from + j < WIDTH;
                         j++) {
                        line[from + j] = id.charAt(j);
                    }
                }
            }

            markCurrentTime(line, windowStart, windowEnd);

            System.out.printf("П%-3d %s%n", device.id, new String(line));
        }

        System.out.print("     ");
        System.out.printf("%.2f", windowStart);

        System.out.print(" ".repeat(Math.max(1, WIDTH - 8)));
        System.out.printf("%.2f%n", windowEnd);

        int currentPosition = timelinePosition(
                currentTime, windowStart, windowEnd
        );

        System.out.println("     " + " ".repeat(currentPosition) + "^ текущее время " + (int) (currentTime * 100) / 100.0d);
    }

    private int timelinePosition(double time, double windowStart, double windowEnd) {
        double relativeTime =
                (time - windowStart) / (windowEnd - windowStart);

        return Math.clamp((int) Math.round(relativeTime * (WIDTH - 1)), 0, WIDTH - 1);
    }

    private void markCurrentTime(char[] line, double windowStart, double windowEnd) {
        int x = timelinePosition(
                currentTime, windowStart, windowEnd
        );

        line[x] = '|';
    }

    private char[] blankTimeline() {
        char[] line = new char[WIDTH];
        Arrays.fill(line, '.');
        return line;
    }
}