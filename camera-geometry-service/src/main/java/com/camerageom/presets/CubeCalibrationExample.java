package com.camerageom.presets;

import com.camerageom.model.Point3D;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Built-in calibration example: the 8 corners of a known unit cube centered at
 * (0, 0, 5) in camera coordinates, axis-aligned, entirely in front of the
 * camera. Projected with the "hd-1000" preset, every corner lands inside the
 * 1920x1080 image, so the example can be eyeballed for correctness.
 */
@Component
public class CubeCalibrationExample {

    public static final String PRESET_NAME = "hd-1000";

    private final List<Point3D> corners;

    public CubeCalibrationExample() {
        List<Point3D> c = new ArrayList<>(8);
        for (double z : new double[]{4.5, 5.5}) {
            for (double y : new double[]{-0.5, 0.5}) {
                for (double x : new double[]{-0.5, 0.5}) {
                    c.add(new Point3D(x, y, z));
                }
            }
        }
        this.corners = List.copyOf(c);
    }

    public String presetName() {
        return PRESET_NAME;
    }

    public List<Point3D> corners() {
        return corners;
    }

    public String description() {
        return "Unit cube centered at (0, 0, 5), side 1.0, axis-aligned; "
                + "all 8 corners project inside the " + PRESET_NAME + " image";
    }
}
