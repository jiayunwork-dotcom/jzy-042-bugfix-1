package com.camerageom.geometry;

import com.camerageom.model.Distortion;

/**
 * Brown-Conrady distortion, applied strictly on the NORMALIZED image plane
 * (x = X/Z, y = Y/Z), never on pixel coordinates. Feeding pixel coordinates
 * into the r^2 term would blow the model up by orders of magnitude.
 *
 *   r^2    = x^2 + y^2
 *   radial = 1 + k1*r^2 + k2*r^4
 *   x_d    = x*radial + 2*p1*x*y + p2*(r^2 + 2*x^2)
 *   y_d    = y*radial + p1*(r^2 + 2*y^2) + 2*p2*x*y
 */
public final class BrownConradyDistortion {

    /** Newton iteration cap for the inverse map; calibration-range coefficients converge in well under it. */
    private static final int MAX_UNDISTORT_ITERATIONS = 30;

    /** Convergence threshold on the Newton step in normalized-plane units. */
    private static final double UNDISTORT_TOLERANCE = 1e-13;

    private BrownConradyDistortion() {
    }

    /**
     * @param x normalized image-plane coordinate x = X/Z
     * @param y normalized image-plane coordinate y = Y/Z
     * @param d distortion coefficients
     * @return distorted normalized coordinates {x_d, y_d}
     */
    public static double[] distort(double x, double y, Distortion d) {
        double r2 = x * x + y * y;
        double r4 = r2 * r2;
        double radial = 1.0 + d.k1() * r2 + d.k2() * r4;
        double xd = x * radial + 2.0 * d.p1() * x * y + d.p2() * (r2 + 2.0 * x * x);
        double yd = y * radial + d.p1() * (r2 + 2.0 * y * y) + 2.0 * d.p2() * x * y;
        return new double[]{xd, yd};
    }

    /**
     * Inverse of {@link #distort}: given distorted normalized coordinates taken
     * off an image, recover the undistorted normalized point lying on the
     * pinhole ray. Solved with Newton iteration on the analytic 2x2 Jacobian of
     * the forward map, so back-projected rays and forward projections share one
     * consistent model. Zero coefficients are an exact identity.
     *
     * @param xd distorted normalized coordinate x_d = (u - cx)/fx
     * @param yd distorted normalized coordinate y_d = (v - cy)/fy
     * @param d  distortion coefficients the observed pixel was produced with
     * @return undistorted normalized coordinates {x, y}
     */
    public static double[] undistort(double xd, double yd, Distortion d) {
        if (d.k1() == 0.0 && d.k2() == 0.0 && d.p1() == 0.0 && d.p2() == 0.0) {
            return new double[]{xd, yd};
        }
        double x = xd;
        double y = yd;
        for (int iter = 0; iter < MAX_UNDISTORT_ITERATIONS; iter++) {
            double r2 = x * x + y * y;
            double r4 = r2 * r2;
            double radial = 1.0 + d.k1() * r2 + d.k2() * r4;

            // Residual of the forward map: F(x,y) - (xd, yd).
            double f1 = x * radial + 2.0 * d.p1() * x * y + d.p2() * (r2 + 2.0 * x * x) - xd;
            double f2 = y * radial + d.p1() * (r2 + 2.0 * y * y) + 2.0 * d.p2() * x * y - yd;

            // Analytic Jacobian of the forward map (symmetric).
            double j11 = radial + 2.0 * d.k1() * x * x + 4.0 * d.k2() * r2 * x * x
                    + 2.0 * d.p1() * y + 6.0 * d.p2() * x;
            double j12 = 2.0 * d.k1() * x * y + 4.0 * d.k2() * r2 * x * y
                    + 2.0 * d.p1() * x + 2.0 * d.p2() * y;
            double j22 = radial + 2.0 * d.k1() * y * y + 4.0 * d.k2() * r2 * y * y
                    + 6.0 * d.p1() * y + 2.0 * d.p2() * x;

            // J * step = -F, solved by Cramer's rule.
            double det = j11 * j22 - j12 * j12;
            double stepX = (-f1 * j22 + j12 * f2) / det;
            double stepY = (j12 * f1 - j11 * f2) / det;
            x += stepX;
            y += stepY;
            if (Math.hypot(stepX, stepY) < UNDISTORT_TOLERANCE) {
                break;
            }
        }
        return new double[]{x, y};
    }
}
