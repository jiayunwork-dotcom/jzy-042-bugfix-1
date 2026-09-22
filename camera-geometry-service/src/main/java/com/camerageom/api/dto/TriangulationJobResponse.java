package com.camerageom.api.dto;

import java.util.List;

public record TriangulationJobResponse(
        String jobId,
        int matchCount,
        double maxReprojectionErrorPixels,
        double meanReprojectionErrorPixels,
        List<TriangulatedMatch> matches) {
}
