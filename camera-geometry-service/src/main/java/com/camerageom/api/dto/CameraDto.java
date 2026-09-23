package com.camerageom.api.dto;

/**
 * One camera of a triangulation rig. Distortion is optional: a null object
 * means "no distortion" (all zeros); a present object must be complete,
 * exactly like the projection endpoints' distortion field.
 */
public record CameraDto(IntrinsicsDto intrinsics, DistortionDto distortion, PoseDto pose) {
}
