package com.camerageom.api.dto;

import java.util.List;

/**
 * A triangulation job: both cameras (intrinsics + extrinsics each) and the
 * matched pixel pairs between the two views.
 */
public record TriangulationJobRequest(
        CameraDto camera1,
        CameraDto camera2,
        List<MatchDto> matches) {
}
