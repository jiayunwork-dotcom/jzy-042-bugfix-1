package com.camerageom.jobs;

import com.camerageom.api.dto.CameraDto;
import com.camerageom.api.dto.IntrinsicsDto;
import com.camerageom.api.dto.MatchDto;
import com.camerageom.api.dto.PoseDto;
import com.camerageom.api.dto.TriangulationJobRequest;
import com.camerageom.api.dto.TriangulationJobResponse;
import com.camerageom.geometry.MidpointTriangulator;
import com.camerageom.geometry.PinholeProjector;
import com.camerageom.model.CameraPose;
import com.camerageom.model.Distortion;
import com.camerageom.model.Intrinsics;
import com.camerageom.model.Pixel;
import com.camerageom.model.Point3D;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cube round-trip: project known cube corners into two views, triangulate the
 * matches, and require the reprojections to land back on the original pixels.
 */
class TriangulationJobServiceTest {

    private static final Intrinsics K = new Intrinsics(1000.0, 1000.0, 960.0, 540.0);
    private static final double[][] IDENTITY = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
    private static final CameraPose POSE_1 = new CameraPose(IDENTITY, new double[]{0, 0, 0});
    private static final CameraPose POSE_2 = new CameraPose(IDENTITY, new double[]{-1, 0, 0}); // center at (1,0,0)

    private final PinholeProjector projector = new PinholeProjector();
    private final TriangulationJobService service =
            new TriangulationJobService(new MidpointTriangulator(), projector, new JobMetrics());

    @Test
    void cubeCornersTriangulateAndReprojectBackOntoOriginalPixels() {
        List<Point3D> corners = cubeCorners(0.0, 0.0, 5.0, 0.5);
        TriangulationJobResponse response = service.process(buildJob(corners, POSE_1, POSE_2));

        assertEquals(8, response.matchCount());
        assertTrue(response.maxReprojectionErrorPixels() < 1e-6,
                "max reprojection error must be < 1e-6 px, was " + response.maxReprojectionErrorPixels());
        assertTrue(response.meanReprojectionErrorPixels() < 1e-6);
        for (int i = 0; i < corners.size(); i++) {
            Point3D truth = corners.get(i);
            var recovered = response.matches().get(i);
            assertEquals(truth.x(), recovered.point().x(), 1e-6);
            assertEquals(truth.y(), recovered.point().y(), 1e-6);
            assertEquals(truth.z(), recovered.point().z(), 1e-6);
            assertTrue(recovered.reprojectionErrorView1() < 1e-6);
            assertTrue(recovered.reprojectionErrorView2() < 1e-6);
        }
    }

    @Test
    void triangulationWorksForShiftedAndRotatedSecondView() {
        // Second camera rotated 10 degrees about Y and translated; rays must still intersect the truth.
        double angle = Math.toRadians(10.0);
        double[][] r = {
                {Math.cos(angle), 0, Math.sin(angle)},
                {0, 1, 0},
                {-Math.sin(angle), 0, Math.cos(angle)}};
        CameraPose pose2 = new CameraPose(r, new double[]{-1.2, 0.3, 0.2});
        List<Point3D> corners = cubeCorners(0.2, -0.1, 6.0, 0.4);
        TriangulationJobResponse response = service.process(buildJob(corners, POSE_1, pose2));

        assertTrue(response.maxReprojectionErrorPixels() < 1e-6,
                "max reprojection error must be < 1e-6 px, was " + response.maxReprojectionErrorPixels());
        for (int i = 0; i < corners.size(); i++) {
            assertEquals(corners.get(i).z(), response.matches().get(i).point().z(), 1e-6);
        }
    }

    static List<Point3D> cubeCorners(double cx, double cy, double cz, double halfSide) {
        List<Point3D> corners = new ArrayList<>(8);
        for (double dz : new double[]{-halfSide, halfSide}) {
            for (double dy : new double[]{-halfSide, halfSide}) {
                for (double dx : new double[]{-halfSide, halfSide}) {
                    corners.add(new Point3D(cx + dx, cy + dy, cz + dz));
                }
            }
        }
        return corners;
    }

    private TriangulationJobRequest buildJob(List<Point3D> worldPoints, CameraPose pose1, CameraPose pose2) {
        List<MatchDto> matches = new ArrayList<>(worldPoints.size());
        for (Point3D world : worldPoints) {
            Pixel p1 = projector.project(K, Distortion.ZERO, pose1.toCameraCoordinates(world));
            Pixel p2 = projector.project(K, Distortion.ZERO, pose2.toCameraCoordinates(world));
            matches.add(new MatchDto(p1.u(), p1.v(), p2.u(), p2.v()));
        }
        return new TriangulationJobRequest(cameraDto(pose1), cameraDto(pose2), matches);
    }

    static CameraDto cameraDto(CameraPose pose) {
        List<List<Double>> rotation = List.of(
                List.of(pose.rotation()[0][0], pose.rotation()[0][1], pose.rotation()[0][2]),
                List.of(pose.rotation()[1][0], pose.rotation()[1][1], pose.rotation()[1][2]),
                List.of(pose.rotation()[2][0], pose.rotation()[2][1], pose.rotation()[2][2]));
        List<Double> translation = List.of(
                pose.translation()[0], pose.translation()[1], pose.translation()[2]);
        return new CameraDto(
                new IntrinsicsDto(K.fx(), K.fy(), K.cx(), K.cy()),
                new PoseDto(rotation, translation));
    }
}
