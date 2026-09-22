package com.camerageom.model;

/**
 * One camera of a triangulation rig: intrinsics plus extrinsics.
 */
public record CameraView(Intrinsics intrinsics, CameraPose pose) {
}
