package com.camerageom.geometry;

import com.camerageom.model.Distortion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The undistortion inverse must exactly undo the forward distortion map on the
 * normalized plane, so a pixel lifted back into a ray meets the same 3D point
 * the projector pushed it out from.
 */
class BrownConradyDistortionTest {

    private static final List<Distortion> DISTORTIONS = List.of(
            new Distortion(-0.28, 0.07, 0.0015, -0.0010),
            new Distortion(-0.18, 0.04, -0.0008, 0.0012),
            new Distortion(-0.12, 0.015, 0.001, -0.0005),
            new Distortion(0.20, -0.05, 0.002, 0.0007));

    private static final double[][] POINTS = {
            {0.30, -0.20}, {-0.45, 0.40}, {0.05, 0.05},
            {-0.10, -0.30}, {0.50, 0.10}, {0.0, 0.0}};

    @Test
    void undistortIsExactIdentityForZeroCoefficients() {
        for (double[] p : POINTS) {
            double[] u = BrownConradyDistortion.undistort(p[0], p[1], Distortion.ZERO);
            assertEquals(p[0], u[0], 0.0);
            assertEquals(p[1], u[1], 0.0);
        }
    }

    @Test
    void undistortInvertsDistortOnTheNormalizedPlane() {
        for (Distortion d : DISTORTIONS) {
            for (double[] p : POINTS) {
                double[] distorted = BrownConradyDistortion.distort(p[0], p[1], d);
                double[] back = BrownConradyDistortion.undistort(distorted[0], distorted[1], d);
                assertEquals(p[0], back[0], 1e-9,
                        "x must round-trip for distortion " + d);
                assertEquals(p[1], back[1], 1e-9,
                        "y must round-trip for distortion " + d);
            }
        }
    }

    @Test
    void distortOfAnUndistortedObservationReproducesTheObservation() {
        // Forward-inverse consistency from the observed (distorted) side: this
        // is exactly the triangulate-then-reproject direction.
        for (Distortion d : DISTORTIONS) {
            for (double[] p : POINTS) {
                double[] observed = BrownConradyDistortion.distort(p[0], p[1], d);
                double[] undistorted = BrownConradyDistortion.undistort(observed[0], observed[1], d);
                double[] reprojected = BrownConradyDistortion.distort(undistorted[0], undistorted[1], d);
                assertEquals(observed[0], reprojected[0], 1e-11);
                assertEquals(observed[1], reprojected[1], 1e-11);
            }
        }
    }
}
