package com.camerageom.api.dto;

/**
 * Single-point projection. Shares the exact same projection function as batch
 * jobs, so a point projected here and in a job yields identical pixels.
 */
public record SingleProjectionRequest(
        IntrinsicsDto intrinsics,
        DistortionDto distortion,
        ImageSizeDto image,
        Point3DDto point) {
}
