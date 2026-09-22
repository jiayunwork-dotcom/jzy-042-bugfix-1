package com.camerageom.model;

/**
 * Camera intrinsics: focal lengths in pixels and principal point in pixels.
 */
public record Intrinsics(double fx, double fy, double cx, double cy) {
}
