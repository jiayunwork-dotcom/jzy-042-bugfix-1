package com.camerageom.validation;

import com.camerageom.api.dto.CameraDto;
import com.camerageom.api.dto.DistortionDto;
import com.camerageom.api.dto.ImageSizeDto;
import com.camerageom.api.dto.IntrinsicsDto;
import com.camerageom.api.dto.MatchDto;
import com.camerageom.api.dto.Point3DDto;
import com.camerageom.api.dto.PoseDto;
import com.camerageom.model.CameraPose;
import com.camerageom.model.CameraView;
import com.camerageom.model.Distortion;
import com.camerageom.model.ImageSize;
import com.camerageom.model.Intrinsics;
import com.camerageom.model.Pixel;
import com.camerageom.model.Point3D;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fail-fast validation, applied to every job BEFORE any computation starts.
 *
 * Service-wide policy: a job either validates completely and is fully
 * processed, or it is rejected as a whole with a typed error. Jobs never
 * partially succeed, and no endpoint ever returns an empty result set in
 * place of an error.
 */
public final class JobRequestValidator {

    private JobRequestValidator() {
    }

    public static Intrinsics requireIntrinsics(IntrinsicsDto dto, String path) {
        if (dto == null) {
            throw new JobValidationException(ErrorCode.MISSING_INTRINSICS_FIELD,
                    "Missing intrinsics object at " + path, Map.of("path", path));
        }
        List<String> missing = new ArrayList<>();
        if (dto.fx() == null) missing.add("fx");
        if (dto.fy() == null) missing.add("fy");
        if (dto.cx() == null) missing.add("cx");
        if (dto.cy() == null) missing.add("cy");
        if (!missing.isEmpty()) {
            throw new JobValidationException(ErrorCode.MISSING_INTRINSICS_FIELD,
                    "Missing intrinsics field(s) " + missing + " at " + path,
                    Map.of("path", path, "missingFields", missing));
        }
        requireFinite(dto.fx(), path + ".fx", ErrorCode.MISSING_INTRINSICS_FIELD);
        requireFinite(dto.fy(), path + ".fy", ErrorCode.MISSING_INTRINSICS_FIELD);
        requireFinite(dto.cx(), path + ".cx", ErrorCode.MISSING_INTRINSICS_FIELD);
        requireFinite(dto.cy(), path + ".cy", ErrorCode.MISSING_INTRINSICS_FIELD);
        if (dto.fx() <= 0.0 || dto.fy() <= 0.0) {
            throw new JobValidationException(ErrorCode.NON_POSITIVE_FOCAL_LENGTH,
                    "Focal lengths must be positive at " + path,
                    Map.of("path", path, "fx", dto.fx(), "fy", dto.fy()));
        }
        return new Intrinsics(dto.fx(), dto.fy(), dto.cx(), dto.cy());
    }

    /** Distortion is optional and defaults to all-zero; if present it must be complete. */
    public static Distortion requireDistortion(DistortionDto dto, String path) {
        if (dto == null) {
            return Distortion.ZERO;
        }
        List<String> missing = new ArrayList<>();
        if (dto.k1() == null) missing.add("k1");
        if (dto.k2() == null) missing.add("k2");
        if (dto.p1() == null) missing.add("p1");
        if (dto.p2() == null) missing.add("p2");
        if (!missing.isEmpty()) {
            throw new JobValidationException(ErrorCode.MISSING_DISTORTION_FIELD,
                    "Missing distortion field(s) " + missing + " at " + path,
                    Map.of("path", path, "missingFields", missing));
        }
        requireFinite(dto.k1(), path + ".k1", ErrorCode.MISSING_DISTORTION_FIELD);
        requireFinite(dto.k2(), path + ".k2", ErrorCode.MISSING_DISTORTION_FIELD);
        requireFinite(dto.p1(), path + ".p1", ErrorCode.MISSING_DISTORTION_FIELD);
        requireFinite(dto.p2(), path + ".p2", ErrorCode.MISSING_DISTORTION_FIELD);
        return new Distortion(dto.k1(), dto.k2(), dto.p1(), dto.p2());
    }

    public static ImageSize requireImageSize(ImageSizeDto dto, String path) {
        if (dto == null || dto.width() == null || dto.height() == null
                || dto.width() <= 0 || dto.height() <= 0) {
            // details must not contain null values (they are copied into an immutable map)
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("path", path);
            if (dto != null && dto.width() != null) {
                details.put("width", dto.width());
            }
            if (dto != null && dto.height() != null) {
                details.put("height", dto.height());
            }
            throw new JobValidationException(ErrorCode.INVALID_IMAGE_SIZE,
                    "Image width and height must be positive integers at " + path, details);
        }
        return new ImageSize(dto.width(), dto.height());
    }

    public static List<Point3D> requirePoints(List<Point3DDto> dtos, String path) {
        if (dtos == null || dtos.isEmpty()) {
            throw new JobValidationException(ErrorCode.EMPTY_POINT_LIST,
                    "Point list must not be empty at " + path, Map.of("path", path));
        }
        List<Integer> malformed = new ArrayList<>();
        List<Integer> behindCamera = new ArrayList<>();
        List<Point3D> points = new ArrayList<>(dtos.size());
        for (int i = 0; i < dtos.size(); i++) {
            Point3DDto p = dtos.get(i);
            if (p == null || p.x() == null || p.y() == null || p.z() == null
                    || !Double.isFinite(p.x()) || !Double.isFinite(p.y()) || !Double.isFinite(p.z())) {
                malformed.add(i);
                continue;
            }
            if (p.z() <= 0.0) {
                behindCamera.add(i);
            }
            points.add(new Point3D(p.x(), p.y(), p.z()));
        }
        if (!malformed.isEmpty()) {
            throw new JobValidationException(ErrorCode.INVALID_POINT_COORDINATE,
                    "Points with missing or non-finite coordinates at " + path,
                    Map.of("path", path, "pointIndices", malformed));
        }
        if (!behindCamera.isEmpty()) {
            throw new JobValidationException(ErrorCode.POINT_BEHIND_CAMERA,
                    "Points with z <= 0 cannot be projected (point is on or behind the camera plane)",
                    Map.of("path", path, "pointIndices", behindCamera));
        }
        return points;
    }

    public static CameraView requireCamera(CameraDto dto, String path) {
        if (dto == null) {
            throw new JobValidationException(ErrorCode.MISSING_CAMERA,
                    "Missing camera at " + path, Map.of("path", path));
        }
        Intrinsics intrinsics = requireIntrinsics(dto.intrinsics(), path + ".intrinsics");
        Distortion distortion = requireDistortion(dto.distortion(), path + ".distortion");
        CameraPose pose = requirePose(dto.pose(), path + ".pose");
        return new CameraView(intrinsics, distortion, pose);
    }

    public static CameraPose requirePose(PoseDto dto, String path) {
        if (dto == null || dto.rotation() == null || dto.translation() == null) {
            throw new JobValidationException(ErrorCode.MISSING_CAMERA_POSE,
                    "Missing camera pose (rotation/translation) at " + path, Map.of("path", path));
        }
        List<List<Double>> r = dto.rotation();
        List<Double> t = dto.translation();
        boolean shapeOk = r.size() == 3 && t.size() == 3;
        if (shapeOk) {
            for (List<Double> row : r) {
                if (row == null || row.size() != 3) {
                    shapeOk = false;
                    break;
                }
            }
        }
        if (!shapeOk) {
            throw new JobValidationException(ErrorCode.INVALID_POSE,
                    "Pose must be a 3x3 rotation and a 3-vector translation at " + path,
                    Map.of("path", path));
        }
        double[][] rotation = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Double v = r.get(i).get(j);
                if (v == null || !Double.isFinite(v)) {
                    throw new JobValidationException(ErrorCode.INVALID_POSE,
                            "Rotation contains a missing or non-finite entry at " + path,
                            Map.of("path", path, "row", i, "column", j));
                }
                rotation[i][j] = v;
            }
        }
        double[] translation = new double[3];
        for (int i = 0; i < 3; i++) {
            Double v = t.get(i);
            if (v == null || !Double.isFinite(v)) {
                throw new JobValidationException(ErrorCode.INVALID_POSE,
                        "Translation contains a missing or non-finite entry at " + path,
                        Map.of("path", path, "index", i));
            }
            translation[i] = v;
        }
        return new CameraPose(rotation, translation);
    }

    /** Returns matched pixel pairs as [view1, view2] arrays, in job order. */
    public static List<Pixel[]> requireMatches(List<MatchDto> dtos, String path) {
        if (dtos == null || dtos.isEmpty()) {
            throw new JobValidationException(ErrorCode.EMPTY_MATCH_LIST,
                    "Match list must not be empty at " + path, Map.of("path", path));
        }
        List<Integer> invalid = new ArrayList<>();
        List<Pixel[]> matches = new ArrayList<>(dtos.size());
        for (int i = 0; i < dtos.size(); i++) {
            MatchDto m = dtos.get(i);
            if (m == null || m.u1() == null || m.v1() == null || m.u2() == null || m.v2() == null
                    || !Double.isFinite(m.u1()) || !Double.isFinite(m.v1())
                    || !Double.isFinite(m.u2()) || !Double.isFinite(m.v2())) {
                invalid.add(i);
                continue;
            }
            matches.add(new Pixel[]{new Pixel(m.u1(), m.v1()), new Pixel(m.u2(), m.v2())});
        }
        if (!invalid.isEmpty()) {
            throw new JobValidationException(ErrorCode.INVALID_MATCH,
                    "Matches with missing or non-finite coordinates at " + path,
                    Map.of("path", path, "matchIndices", invalid));
        }
        return matches;
    }

    private static void requireFinite(double value, String field, ErrorCode code) {
        if (!Double.isFinite(value)) {
            throw new JobValidationException(code,
                    "Non-finite value for " + field, Map.of("field", field));
        }
    }
}
