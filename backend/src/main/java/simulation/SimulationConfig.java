package simulation;

public record SimulationConfig(
        int sourceCount,
        int deviceCount,
        int bufferCapacity,
        int requestsPerSource,
        double lambda,
        double serviceMin,
        double serviceMax,
        boolean automatic
) {}
