import simulation.SimulationConfig;
import simulation.SimulationEngine;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {
    private static final int DEFAULT_SOURCE_COUNT = 1;
    private static final int DEFAULT_DEVICE_COUNT = 1;
    private static final int DEFAULT_BUFFER_CAPACITY = 1;
    private static final int DEFAULT_REQUESTS_PER_SOURCE = 1;
    private static final double DEFAULT_LAMBDA = 0.8;
    private static final double DEFAULT_SERVICE_MIN = 0.8;
    private static final double DEFAULT_SERVICE_MAX = 1.2;

    public static void main(String[] args) {

        try {
            Map<String, String> params = parseArgs(args);

            int sourceCount = readInt(
                    params, "source-count",
                    DEFAULT_SOURCE_COUNT, 1, 100
            );

            int deviceCount = readInt(
                    params, "device-count",
                    DEFAULT_DEVICE_COUNT, 1, 100
            );

            int bufferCapacity = readInt(
                    params, "buffer-capacity",
                    DEFAULT_BUFFER_CAPACITY, 1, 10000
            );

            int requestsPerSource = readInt(
                    params, "requests-per-source",
                    DEFAULT_REQUESTS_PER_SOURCE, 1, 1_000_000
            );

            double lambda = readDouble(
                    params, "lambda",
                    DEFAULT_LAMBDA, 0.000001, Double.MAX_VALUE
            );

            double serviceMin = readDouble(
                    params, "service-min",
                    DEFAULT_SERVICE_MIN, 0.000001, Double.MAX_VALUE
            );

            double serviceMax = readDouble(
                    params, "service-max",
                    DEFAULT_SERVICE_MAX, serviceMin, Double.MAX_VALUE
            );

            System.out.println("=== СИСТЕМА МОНИТОРИНГА И ОБРАБОТКИ ЛОГОВ ===");
            System.out.println(
                    "Вариант 3: ИБ ИЗ1 ПЗ2 Д1ОЗ1 Д1ОО3 Д2П1 Д2Б3 ОР1 ОД3\n"
            );

            System.out.println("Используемые параметры:");
            System.out.println("Количество источников: " + sourceCount);
            System.out.println("Количество worker-процессов: " + deviceCount);
            System.out.println("Вместимость буфера: " + bufferCapacity);
            System.out.println("Заявок от каждого источника: " + requestsPerSource);
            System.out.println("Интенсивность lambda: " + lambda);
            System.out.println("Минимальное время обслуживания: " + serviceMin);
            System.out.println("Максимальное время обслуживания: " + serviceMax + "\n");

            SimulationConfig config = new SimulationConfig(
                    sourceCount,
                    deviceCount,
                    bufferCapacity,
                    requestsPerSource,
                    lambda,
                    serviceMin,
                    serviceMax
            );

            Scanner scanner = new Scanner(System.in);
            SimulationEngine engine = new SimulationEngine(config, scanner);
            engine.run();

        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка: " + e.getMessage());
            System.exit(1);
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> params = new HashMap<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException(
                        "Ожидался именованный аргумент, получено: " + arg
                );
            }

            String key = arg.substring(2);

            if (key.isEmpty()) {
                throw new IllegalArgumentException("Пустое имя параметра.");
            }

            if (!isKnownParameter(key)) {
                throw new IllegalArgumentException(
                        "Неизвестный параметр: --" + key
                );
            }

            if (params.containsKey(key)) {
                throw new IllegalArgumentException(
                        "Параметр --" + key + " указан несколько раз."
                );
            }

            if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                throw new IllegalArgumentException(
                        "Не указано значение для параметра --" + key
                );
            }

            params.put(key, args[++i]);
        }

        return params;
    }

    private static boolean isKnownParameter(String key) {
        return switch (key) {
            case "source-count",
                 "device-count",
                 "buffer-capacity",
                 "requests-per-source",
                 "lambda",
                 "service-min",
                 "service-max" -> true;
            default -> false;
        };
    }

    private static int readInt(
            Map<String, String> params,
            String key,
            int defaultValue,
            int min,
            int max
    ) {
        if (!params.containsKey(key)) {
            return defaultValue;
        }

        int value;

        try {
            value = Integer.parseInt(params.get(key));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Параметр --" + key + " должен быть целым числом."
            );
        }

        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    "Параметр --" + key + " должен находиться в диапазоне от "
                            + min + " до " + max + "."
            );
        }

        return value;
    }

    private static double readDouble(
            Map<String, String> params,
            String key,
            double defaultValue,
            double min,
            double max
    ) {
        if (!params.containsKey(key)) {
            return defaultValue;
        }

        double value;

        try {
            value = Double.parseDouble(
                    params.get(key).replace(',', '.')
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Параметр --" + key + " должен быть числом."
            );
        }

        if (!Double.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException(
                    "Параметр --" + key + " должен находиться в диапазоне от "
                            + min + " до " + max + "."
            );
        }

        return value;
    }
}
