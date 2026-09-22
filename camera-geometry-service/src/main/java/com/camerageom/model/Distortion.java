package com.camerageom.model;

/**
 * Brown-Conrady distortion coefficients: radial (k1, k2) and tangential (p1, p2).
 * Always applied on the normalized image plane, never on pixel coordinates.
 */
public record Distortion(double k1, double k2, double p1, double p2) {

    public static final Distortion ZERO = new Distortion(0.0, 0.0, 0.0, 0.0);
}
