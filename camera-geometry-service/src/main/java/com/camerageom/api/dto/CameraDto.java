package com.camerageom.api.dto;

/**
 * One camera of a triangulation rig. Distortion is optional: a null object (or
 * a request that omits the field entirely) means all-zero coefficients.
 */
public record CameraDto(IntrinsicsDto intrinsics, DistortionDto distortion, PoseDto pose) {

    /** Backwards-compatible view of a camera without explicit distortion coefficients. */
    public CameraDto(IntrinsicsDto intrinsics, PoseDto pose) {
        this(intrinsics, null, pose);
    }
}
