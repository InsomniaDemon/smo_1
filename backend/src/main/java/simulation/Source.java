package simulation;

import request.Request;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Source {
    final int id;
    private final double intensity;
    int generatedCount;
    final List<Double> generationTimes = new ArrayList<>();

    public Source(int id, double intensity) {
        this.id = id;
        this.intensity = intensity;
    }

    public Request generate(double time) {
        generatedCount++;
        generationTimes.add(time);
        return new Request(id, generatedCount, time);
    }

    // ИЗ1: интервалы между заявками имеют экспоненциальное распределение.
    public double getNextGenerationTime(double currentTime, Random random) {
        double u = random.nextDouble();
        double interval = -Math.log(1.0 - u) / intensity;
        return currentTime + interval;
    }
}