package com.camerageom.model;

/**
 * One camera of a triangulation rig: intrinsics, Brown-Conrady distortion and
 * extrinsics. The distortion is applied on the normalized image plane and is
 * part of both back-projection (ray) and forward reprojection.
 */
public record CameraView(Intrinsics intrinsics, Distortion distortion, CameraPose pose) {
}
