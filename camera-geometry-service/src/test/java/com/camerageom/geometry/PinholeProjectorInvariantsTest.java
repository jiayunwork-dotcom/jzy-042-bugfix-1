package com.camerageom.geometry;

import com.camerageom.model.Distortion;
import com.camerageom.model.Intrinsics;
import com.camerageom.model.Pixel;
import com.camerageom.model.Point3D;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Projection invariants that must hold for every job, batch or single-point.
 */
class PinholeProjectorInvariantsTest {

    private final PinholeProjector projector = new PinholeProjector();

    private static final Intrinsics K = new Intrinsics(800.0, 820.0, 640.0, 360.0);
    private static final List<Point3D> SAMPLE_POINTS = List.of(
            new Point3D(0.3, -0.2, 2.0),
            new Point3D(-1.1, 0.7, 3.5),
            new Point3D(0.0, 0.0, 1.0),
            new Point3D(2.5, -1.5, 0.8),
            new Point3D(-0.4, -0.6, 5.0));

    @Test
    void zeroDistortionCoefficientsLeavePixelsUntouched() {
        for (Point3D p : SAMPLE_POINTS) {
            Pixel px = projector.project(K, Distortion.ZERO, p);
            // With all coefficients zero the pixel must equal the pure pinhole projection.
            assertEquals(K.cx() + K.fx() * p.x() / p.z(), px.u(), 1e-9);
            assertEquals(K.cy() + K.fy() * p.y() / p.z(), px.v(), 1e-9);
            // The distortion step itself is an exact identity under zero coefficients.
            double[] d = BrownConradyDistortion.distort(p.x() / p.z(), p.y() / p.z(), Distortion.ZERO);
            assertEquals(p.x() / p.z(), d[0], 0.0);
            assertEquals(p.y() / p.z(), d[1], 0.0);
        }
    }

    @Test
    void normalizationIsXOverZNotZOverX() {
        // Guards against swapping the normalization or skipping it entirely.
        Point3D p = new Point3D(1.0, 0.0, 2.0);
        Pixel px = projector.project(K, Distortion.ZERO, p);
        assertEquals(K.cx() + K.fx() * 0.5, px.u(), 1e-9);
        assertEquals(K.cy(), px.v(), 1e-9);
    }

    @Test
    void pointsMovedAwayAlongZShrinkTowardPrincipalPoint() {
        for (Point3D p : SAMPLE_POINTS) {
            if (p.x() == 0.0 && p.y() == 0.0) {
                continue; // on the principal axis: already at the principal point
            }
            Point3D farther = new Point3D(p.x(), p.y(), p.z() + 4.0);
            Pixel near = projector.project(K, Distortion.ZERO, p);
            Pixel far = projector.project(K, Distortion.ZERO, farther);
            double rNear = Math.hypot(near.u() - K.cx(), near.v() - K.cy());
            double rFar = Math.hypot(far.u() - K.cx(), far.v() - K.cy());
            assertTrue(rFar < rNear, "moving away along Z must shrink the radius toward the principal point");
            // Zero distortion: the radius scales exactly by z / (z + delta).
            assertEquals(rNear * p.z() / (p.z() + 4.0), rFar, 1e-9);
        }
    }

    @Test
    void pointsMovedAwayAlongZShrinkWithDistortionToo() {
        Distortion d = new Distortion(-0.12, 0.015, 0.001, -0.0005);
        for (Point3D p : SAMPLE_POINTS) {
            if (p.x() == 0.0 && p.y() == 0.0) {
                continue;
            }
            Point3D farther = new Point3D(p.x(), p.y(), p.z() + 4.0);
            Pixel near = projector.project(K, d, p);
            Pixel far = projector.project(K, d, farther);
            double rNear = Math.hypot(near.u() - K.cx(), near.v() - K.cy());
            double rFar = Math.hypot(far.u() - K.cx(), far.v() - K.cy());
            assertTrue(rFar < rNear);
        }
    }

    @Test
    void doublingFocalLengthsDoublesRadiusFromPrincipalPoint() {
        Intrinsics doubled = new Intrinsics(K.fx() * 2.0, K.fy() * 2.0, K.cx(), K.cy());
        for (Distortion d : List.of(Distortion.ZERO, new Distortion(-0.12, 0.015, 0.001, -0.0005))) {
            for (Point3D p : SAMPLE_POINTS) {
                Pixel base = projector.project(K, d, p);
                Pixel scaled = projector.project(doubled, d, p);
                assertEquals(2.0 * (base.u() - K.cx()), scaled.u() - K.cx(), 1e-9);
                assertEquals(2.0 * (base.v() - K.cy()), scaled.v() - K.cy(), 1e-9);
            }
        }
    }

    @Test
    void nonZeroDistortionActuallyBendsOffAxisPixels() {
        Distortion d = new Distortion(-0.12, 0.015, 0.001, -0.0005);
        Point3D offAxis = new Point3D(0.6, 0.4, 2.0);
        Pixel distorted = projector.project(K, d, offAxis);
        Pixel clean = projector.project(K, Distortion.ZERO, offAxis);
        assertNotEquals(clean.u(), distorted.u());
        assertNotEquals(clean.v(), distorted.v());
    }
}
