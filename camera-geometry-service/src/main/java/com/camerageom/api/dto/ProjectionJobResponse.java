package com.camerageom.api.dto;

import java.util.List;

public record ProjectionJobResponse(
        String jobId,
        int pointCount,
        int outOfBoundsCount,
        List<ProjectedPoint> points) {
}
