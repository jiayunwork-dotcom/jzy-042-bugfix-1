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

    /** Convergence tolerance on the normalized plane (well below a pixel / focal length). */
    private static final double UNDISTORT_EPS = 1e-13;
    private static final int NEWTON_MAX_ITERS = 20;
    private static final int FALLBACK_MAX_ITERS = 100;

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
     * Inverse of {@link #distort}: given observed distorted normalized coordinates
     * (i.e. (u - cx)/fx, (v - cy)/fy), recover the undistorted normalized
     * coordinates of the viewing ray. Needed whenever a distorted pixel is
     * back-projected (triangulation); without this step the ray and the
     * triangulated point are wrong even though forward projection is correct.
     *
     * Solves f(x,y) = distort(x,y) - (xd, yd) = 0 with Newton iterations, using
     * the analytical 2x2 Jacobian. Newton converges quadratically for ordinary
     * lens coefficients; a residual-not-decreasing fallback to damped fixed-point
     * steps guards the (pathological) case where a Newton step overshoots.
     */
    public static double[] undistort(double xd, double yd, Distortion d) {
        double x = xd;
        double y = yd;
        double residual = Double.MAX_VALUE;

        for (int i = 0; i < NEWTON_MAX_ITERS; i++) {
            double[] predicted = distort(x, y, d);
            double fx = predicted[0] - xd;
            double fy = predicted[1] - yd;
            double current = Math.hypot(fx, fy);
            if (current < UNDISTORT_EPS) {
                return new double[]{x, y};
            }

            // Analytical Jacobian J = d(distort)/d(x,y); solve J * delta = -f.
            double r2 = x * x + y * y;
            double dr = d.k1() + 2.0 * d.k2() * r2;   // d(radial)/d(r^2)
            double radial = 1.0 + d.k1() * r2 + d.k2() * r2 * r2;

            double j00 = radial + 2.0 * x * x * dr + 2.0 * d.p1() * y + 6.0 * d.p2() * x;
            double j01 = 2.0 * x * y * dr + 2.0 * d.p1() * x + 2.0 * d.p2() * y;
            double j10 = 2.0 * x * y * dr + 2.0 * d.p1() * x + 2.0 * d.p2() * y;
            double j11 = radial + 2.0 * y * y * dr + 6.0 * d.p1() * y + 2.0 * d.p2() * x;

            double det = j00 * j11 - j01 * j10;
            if (det == 0.0) {
                break; // singular Jacobian: leave it to the fallback iteration
            }
            double dx = (j11 * fx - j01 * fy) / det;
            double dy = (-j10 * fx + j00 * fy) / det;

            double nx = x - dx;
            double ny = y - dy;
            double[] np = distort(nx, ny, d);
            double newResidual = Math.hypot(np[0] - xd, np[1] - yd);
            if (newResidual < current) {
                x = nx;
                y = ny;
                residual = newResidual;
            } else {
                // Newton overshot: fall back to damped contraction towards the
                // solution (fixed point x <- x + (xd - distort(x))) from here.
                break;
            }
        }

        for (int i = 0; i < FALLBACK_MAX_ITERS; i++) {
            double[] predicted = distort(x, y, d);
            double fx = xd - predicted[0];
            double fy = yd - predicted[1];
            if (Math.hypot(fx, fy) < UNDISTORT_EPS) {
                return new double[]{x, y};
            }
            // Distortion is contractive near typical solutions; halve the step
            // until it actually reduces the residual.
            double step = 1.0;
            for (int backtrack = 0; backtrack < 20; backtrack++) {
                double nx = x + step * fx;
                double ny = y + step * fy;
                double[] np = distort(nx, ny, d);
                double nr = Math.hypot(np[0] - xd, np[1] - yd);
                if (nr <= residual) {
                    x = nx;
                    y = ny;
                    residual = nr;
                    break;
                }
                step *= 0.5;
            }
        }
        return new double[]{x, y};
    }
}
