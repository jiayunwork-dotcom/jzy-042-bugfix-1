package com.camerageom.model;

/**
 * Camera extrinsics mapping world coordinates into camera coordinates:
 * X_cam = R * X_world + t. Rotation is a row-major 3x3 matrix, translation a 3-vector.
 */
public record CameraPose(double[][] rotation, double[] translation) {

    /** Computes R^T * v (world-frame direction from a camera-frame direction). */
    public double[] rotateTransposed(double[] v) {
        double[][] r = rotation;
        return new double[]{
                r[0][0] * v[0] + r[1][0] * v[1] + r[2][0] * v[2],
                r[0][1] * v[0] + r[1][1] * v[1] + r[2][1] * v[2],
                r[0][2] * v[0] + r[1][2] * v[1] + r[2][2] * v[2]
        };
    }

    /** Camera center in world coordinates: C = -R^T * t. */
    public double[] cameraCenter() {
        double[] rt = rotateTransposed(translation);
        return new double[]{-rt[0], -rt[1], -rt[2]};
    }

    /** Transforms a world point into camera coordinates: X_cam = R * X_world + t. */
    public Point3D toCameraCoordinates(Point3D world) {
        double[][] r = rotation;
        double[] t = translation;
        double x = r[0][0] * world.x() + r[0][1] * world.y() + r[0][2] * world.z() + t[0];
        double y = r[1][0] * world.x() + r[1][1] * world.y() + r[1][2] * world.z() + t[1];
        double z = r[2][0] * world.x() + r[2][1] * world.y() + r[2][2] * world.z() + t[2];
        return new Point3D(x, y, z);
    }
}
