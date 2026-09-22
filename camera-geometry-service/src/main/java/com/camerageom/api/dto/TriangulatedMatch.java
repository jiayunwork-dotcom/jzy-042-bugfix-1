package com.camerageom.api.dto;

public record TriangulatedMatch(
        int index,
        Point3DDto point,
        double reprojectionErrorView1,
        double reprojectionErrorView2) {
}
