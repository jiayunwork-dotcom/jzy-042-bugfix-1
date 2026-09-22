package com.camerageom.api.dto;

import java.util.List;

/**
 * A projection job: one set of intrinsics + distortion + image size, plus the
 * whole batch of camera-space 3D points to project.
 */
public record ProjectionJobRequest(
        IntrinsicsDto intrinsics,
        DistortionDto distortion,
        ImageSizeDto image,
        List<Point3DDto> points) {
}
