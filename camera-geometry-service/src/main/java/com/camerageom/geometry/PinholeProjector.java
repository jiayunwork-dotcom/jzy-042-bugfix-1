package com.camerageom.geometry;

import com.camerageom.model.Distortion;
import com.camerageom.model.ImageSize;
import com.camerageom.model.Intrinsics;
import com.camerageom.model.Pixel;
import com.camerageom.model.Point3D;
import org.springframework.stereotype.Component;

/**
 * The single projection path shared by the single-point endpoint and batch jobs.
 *
 * Pipeline: camera-space point -> normalized plane (x = X/Z, y = Y/Z) ->
 * Brown-Conrady distortion on the normalized plane -> pixel map
 * u = cx + fx*x_d, v = cy + fy*y_d.
 *
 * Callers must guarantee z > 0; the validation layer rejects z <= 0 before
 * this component is ever invoked.
 */
@Component
public class PinholeProjector {

    public Pixel project(Intrinsics intrinsics, Distortion distortion, Point3D cameraPoint) {
        double x = cameraPoint.x() / cameraPoint.z();
        double y = cameraPoint.y() / cameraPoint.z();
        double[] distorted = BrownConradyDistortion.distort(x, y, distortion);
        double u = intrinsics.cx() + intrinsics.fx() * distorted[0];
        double v = intrinsics.cy() + intrinsics.fy() * distorted[1];
        return new Pixel(u, v);
    }

    public boolean isInBounds(Pixel pixel, ImageSize imageSize) {
        return pixel.u() >= 0.0 && pixel.u() < imageSize.width()
                && pixel.v() >= 0.0 && pixel.v() < imageSize.height();
    }
}
