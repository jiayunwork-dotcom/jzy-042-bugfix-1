package com.camerageom.model;

/**
 * One camera of a triangulation rig: intrinsics, distortion coefficients and extrinsics.
 */
public record CameraView(Intrinsics intrinsics, Distortion distortion, CameraPose pose) {
}
