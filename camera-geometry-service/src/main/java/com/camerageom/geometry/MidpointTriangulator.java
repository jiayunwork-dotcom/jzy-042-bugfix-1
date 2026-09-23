package com.camerageom.geometry;

import com.camerageom.model.CameraPose;
import com.camerageom.model.CameraView;
import com.camerageom.model.Pixel;
import com.camerageom.model.Point3D;
import com.camerageom.validation.ErrorCode;
import com.camerageom.validation.JobValidationException;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Two-view triangulation via the MIDPOINT method (the service's fixed choice):
 * each matched pixel is back-projected to a world-space ray (camera center +
 * unit direction), and the 3D point is the midpoint of the shortest segment
 * between the two rays. Pixel coordinates of the two views are never averaged
 * to fake a 3D point.
 *
 * Back-projection is the exact inverse of {@link PinholeProjector}: the
 * observed pixel is lifted to the distorted normalized plane, undistorted with
 * {@link BrownConradyDistortion#undistort}, and only then turned into a ray, so
 * matches produced by distorted cameras triangulate onto the same points the
 * forward (distorting) path projects from.
 */
@Component
public class MidpointTriangulator {

    private static final double PARALLEL_RAYS_EPS = 1e-12;

    public Point3D triangulate(CameraView view1, Pixel px1, CameraView view2, Pixel px2) {
        double[] d1 = worldRayDirection(view1, px1);
        double[] d2 = worldRayDirection(view2, px2);
        double[] c1 = view1.pose().cameraCenter();
        double[] c2 = view2.pose().cameraCenter();

        // Closest points on lines c1 + s*d1 and c2 + t*d2 (unit directions).
        double b = dot(d1, d2);
        double[] w0 = subtract(c1, c2);
        double d = dot(w0, d1);
        double e = dot(w0, d2);
        double denom = 1.0 - b * b;
        if (Math.abs(denom) < PARALLEL_RAYS_EPS) {
            throw new JobValidationException(ErrorCode.TRIANGULATION_DEGENERATE,
                    "Viewing rays are (near-)parallel; triangulation is degenerate", Map.of());
        }
        double s = (b * e - d) / denom;
        double t = (e - b * d) / denom;

        double[] p1 = add(c1, scale(d1, s));
        double[] p2 = add(c2, scale(d2, t));
        return new Point3D(
                (p1[0] + p2[0]) / 2.0,
                (p1[1] + p2[1]) / 2.0,
                (p1[2] + p2[2]) / 2.0);
    }

    /**
     * Back-projects a pixel to a unit ray direction in world coordinates.
     * Pixel -> distorted normalized plane -> undistorted normalized point
     * {@code [x, y, 1]} -> {@code R^T * [x, y, 1]}.
     */
    private double[] worldRayDirection(CameraView view, Pixel px) {
        var k = view.intrinsics();
        double xd = (px.u() - k.cx()) / k.fx();
        double yd = (px.v() - k.cy()) / k.fy();
        double[] undistorted = BrownConradyDistortion.undistort(xd, yd, view.distortion());
        double[] rayCam = new double[]{undistorted[0], undistorted[1], 1.0};
        CameraPose pose = view.pose();
        return normalize(pose.rotateTransposed(rayCam));
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] subtract(double[] a, double[] b) {
        return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double[] add(double[] a, double[] b) {
        return new double[]{a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    private static double[] scale(double[] a, double s) {
        return new double[]{a[0] * s, a[1] * s, a[2] * s};
    }

    private static double[] normalize(double[] a) {
        double norm = Math.sqrt(dot(a, a));
        return new double[]{a[0] / norm, a[1] / norm, a[2] / norm};
    }
}
