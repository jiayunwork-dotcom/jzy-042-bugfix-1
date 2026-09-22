package com.camerageom.jobs;

import com.camerageom.api.dto.ProjectedPoint;
import com.camerageom.api.dto.ProjectionJobRequest;
import com.camerageom.api.dto.ProjectionJobResponse;
import com.camerageom.api.dto.SingleProjectionRequest;
import com.camerageom.api.dto.SingleProjectionResponse;
import com.camerageom.geometry.PinholeProjector;
import com.camerageom.model.Distortion;
import com.camerageom.model.ImageSize;
import com.camerageom.model.Intrinsics;
import com.camerageom.model.Pixel;
import com.camerageom.model.Point3D;
import com.camerageom.validation.JobRequestValidator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Projection job orchestration. Stateless: every job is validated and computed
 * in local state only, so concurrently submitted jobs are fully isolated.
 */
@Service
public class ProjectionJobService {

    private final PinholeProjector projector;
    private final JobMetrics metrics;

    public ProjectionJobService(PinholeProjector projector, JobMetrics metrics) {
        this.projector = projector;
        this.metrics = metrics;
    }

    public ProjectionJobResponse process(ProjectionJobRequest request) {
        Intrinsics intrinsics = JobRequestValidator.requireIntrinsics(request.intrinsics(), "intrinsics");
        Distortion distortion = JobRequestValidator.requireDistortion(request.distortion(), "distortion");
        ImageSize imageSize = JobRequestValidator.requireImageSize(request.image(), "image");
        List<Point3D> points = JobRequestValidator.requirePoints(request.points(), "points");

        List<ProjectedPoint> results = new ArrayList<>(points.size());
        int outOfBounds = 0;
        for (int i = 0; i < points.size(); i++) {
            Pixel pixel = projector.project(intrinsics, distortion, points.get(i));
            boolean inBounds = projector.isInBounds(pixel, imageSize);
            if (!inBounds) {
                outOfBounds++;
            }
            results.add(new ProjectedPoint(i, pixel.u(), pixel.v(), inBounds));
        }
        metrics.projectionJobCompleted();
        return new ProjectionJobResponse(UUID.randomUUID().toString(), points.size(), outOfBounds, results);
    }

    /** Single-point projection; shares the exact same projector as batch jobs. */
    public SingleProjectionResponse processSingle(SingleProjectionRequest request) {
        Intrinsics intrinsics = JobRequestValidator.requireIntrinsics(request.intrinsics(), "intrinsics");
        Distortion distortion = JobRequestValidator.requireDistortion(request.distortion(), "distortion");
        ImageSize imageSize = JobRequestValidator.requireImageSize(request.image(), "image");
        // List.of rejects nulls, so a missing point arrives as an empty list and
        // is rejected by the validator with EMPTY_POINT_LIST.
        List<Point3D> points = JobRequestValidator.requirePoints(
                request.point() == null ? List.of() : List.of(request.point()), "point");

        Pixel pixel = projector.project(intrinsics, distortion, points.get(0));
        boolean inBounds = projector.isInBounds(pixel, imageSize);
        metrics.singleProjectionCompleted();
        return new SingleProjectionResponse(pixel.u(), pixel.v(), inBounds);
    }
}
