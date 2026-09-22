package com.camerageom.model;

/**
 * A 3D point. In projection jobs it is expressed in camera coordinates;
 * in triangulation results it is expressed in world coordinates.
 */
public record Point3D(double x, double y, double z) {
}
