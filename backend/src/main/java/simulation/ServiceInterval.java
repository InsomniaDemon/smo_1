package simulation;

import request.Request;

public record ServiceInterval(Request request, double start, double finish) {}