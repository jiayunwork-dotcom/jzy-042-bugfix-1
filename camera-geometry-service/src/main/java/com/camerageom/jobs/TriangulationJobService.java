package com.camerageom.jobs;

import com.camerageom.api.dto.TriangulatedMatch;
import com.camerageom.api.dto.TriangulationJobRequest;
import com.camerageom.api.dto.TriangulationJobResponse;
import com.camerageom.api.dto.Point3DDto;
import com.camerageom.geometry.MidpointTriangulator;
import com.camerageom.geometry.PinholeProjector;
import com.camerageom.model.CameraView;
import com.camerageom.model.Pixel;
import com.camerageom.model.Point3D;
import com.camerageom.validation.ErrorCode;
import com.camerageom.validation.JobRequestValidator;
import com.camerageom.validation.JobValidationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Triangulation job orchestration. Stateless: every job is validated and
 * computed in local state only, so concurrently submitted jobs are fully
 * isolated.
 *
 * Reprojection error is reported per match and per view; job-level max/mean
 * are computed over per-match combined errors (mean of the two views).
 */
@Service
public class TriangulationJobService {

    private final MidpointTriangulator triangulator;
    private final PinholeProjector projector;
    private final JobMetrics metrics;

    public TriangulationJobService(MidpointTriangulator triangulator,
                                   PinholeProjector projector,
                                   JobMetrics metrics) {
        this.triangulator = triangulator;
        this.projector = projector;
        this.metrics = metrics;
    }

    public TriangulationJobResponse process(TriangulationJobRequest request) {
        CameraView camera1 = JobRequestValidator.requireCamera(request.camera1(), "camera1");
        CameraView camera2 = JobRequestValidator.requireCamera(request.camera2(), "camera2");
        List<Pixel[]> matches = JobRequestValidator.requireMatches(request.matches(), "matches");

        List<TriangulatedMatch> results = new ArrayList<>(matches.size());
        double maxError = 0.0;
        double sumError = 0.0;
        for (int i = 0; i < matches.size(); i++) {
            Pixel px1 = matches.get(i)[0];
            Pixel px2 = matches.get(i)[1];
            Point3D point = triangulator.triangulate(
                    camera1.intrinsics(), camera1.distortion(), camera1.pose(), px1,
                    camera2.intrinsics(), camera2.distortion(), camera2.pose(), px2);
            double err1 = reprojectionError(camera1, point, px1, i);
            double err2 = reprojectionError(camera2, point, px2, i);
            double combined = (err1 + err2) / 2.0;
            maxError = Math.max(maxError, combined);
            sumError += combined;
            results.add(new TriangulatedMatch(
                    i,
                    new Point3DDto(point.x(), point.y(), point.z()),
                    err1,
                    err2));
        }
        metrics.triangulationJobCompleted();
        double meanError = sumError / matches.size();
        return new TriangulationJobResponse(
                UUID.randomUUID().toString(),
                matches.size(),
                maxError,
                meanError,
                results);
    }

    /**
     * Reprojects a world point into a view through the SAME projection path as
     * the projection endpoints (Brown-Conrady distortion honoured) and measures
     * pixel distance against the observed (distorted) match pixel.
     */
    private double reprojectionError(CameraView camera, Point3D worldPoint, Pixel observed, int matchIndex) {
        Point3D cameraPoint = camera.pose().toCameraCoordinates(worldPoint);
        if (cameraPoint.z() <= 0.0) {
            throw new JobValidationException(ErrorCode.TRIANGULATION_DEGENERATE,
                    "Triangulated point lies behind a camera and cannot be reprojected",
                    Map.of("matchIndex", matchIndex));
        }
        Pixel reprojected = projector.project(camera.intrinsics(), camera.distortion(), cameraPoint);
        return Math.hypot(reprojected.u() - observed.u(), reprojected.v() - observed.v());
    }
}
