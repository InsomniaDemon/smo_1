package simulation;

import request.Request;
import request.RequestStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Device {
    public final int id;
    final List<ServiceInterval> serviceHistory = new ArrayList<>();
    Request currentRequest;
    double busyUntil;
    public double totalBusyTime;

    Device(int id) {
        this.id = id;
    }

    public boolean isFree() {
        return currentRequest == null;
    }

    // ПЗ2: равномерное время обслуживания U[a; b].
    public double startProcessing(Request request, double time, double serviceMin, double serviceMax, Random random) {
        double duration = serviceMin + random.nextDouble() * (serviceMax - serviceMin);
        currentRequest = request;
        request.status = RequestStatus.PROCESSING;
        request.serviceStartedAt = time;
        request.serviceDuration = duration;
        busyUntil = time + duration;
        totalBusyTime += duration;
        serviceHistory.add(new ServiceInterval(request, time, busyUntil));
        return busyUntil;
    }

    Request finishProcessing() {
        Request finished = currentRequest;
        currentRequest = null;
        return finished;
    }
}