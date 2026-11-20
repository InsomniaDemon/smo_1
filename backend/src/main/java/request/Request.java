package request;

public class Request {
    public final int sourceId;
    public final int sequenceNumber;
    public final double createdAt;
    public RequestStatus status = RequestStatus.CREATED;
    public double serviceStartedAt = Double.NaN;
    public double completedAt = Double.NaN;
    public double serviceDuration;

    public Request(int sourceId, int sequenceNumber, double createdAt) {
        this.sourceId = sourceId;
        this.sequenceNumber = sequenceNumber;
        this.createdAt = createdAt;
    }

    public String id() {
        return "(" + sourceId + "," + sequenceNumber + ")";
    }

    public void markRejected() {
        status = RequestStatus.REJECTED;
    }
}