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
}
