package statistics;

import request.Request;

public class SourceStatistics {
    long generated;
    long rejected;
    long completed;
    private double waitingSum;
    private double waitingSquareSum;
    private double serviceSum;
    private double serviceSquareSum;
    private double systemTimeSum;

    void recordGenerated() {
        generated++;
    }

    void recordRejected() {
        rejected++;
    }

    void recordServiceStart(Request request, double time) {
        double waiting = time - request.createdAt;
        waitingSum += waiting;
        waitingSquareSum += waiting * waiting;
    }

    void recordCompleted(Request request, double time) {
        double service = time - request.serviceStartedAt;
        double systemTime = time - request.createdAt;
        serviceSum += service;
        serviceSquareSum += service * service;
        systemTimeSum += systemTime;
        completed++;
    }

    double rejectionProbability() {
        return generated == 0 ? 0 : (double) rejected / generated;
    }

    double averageWaiting() {
        return completed == 0 ? 0 : waitingSum / completed;
    }

    double averageService() {
        return completed == 0 ? 0 : serviceSum / completed;
    }

    double averageSystemTime() {
        return completed == 0 ? 0 : systemTimeSum / completed;
    }

    double waitingVariance() {
        return completed == 0 ? 0 : Math.max(0,
                waitingSquareSum / completed - averageWaiting() * averageWaiting());
    }

    double serviceVariance() {
        return completed == 0 ? 0 : Math.max(0,
                serviceSquareSum / completed - averageService() * averageService());
    }
}