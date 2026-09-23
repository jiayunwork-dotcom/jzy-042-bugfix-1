package com.camerageom.api;

import com.camerageom.geometry.PinholeProjector;
import com.camerageom.model.CameraPose;
import com.camerageom.model.Distortion;
import com.camerageom.model.Intrinsics;
import com.camerageom.model.Pixel;
import com.camerageom.model.Point3D;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.camerageom.api.TestJson.obj;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end reproduction of the distorted-camera round trip: cube corners are
 * projected through two views carrying non-zero Brown-Conrady coefficients,
 * the pixels are submitted as a triangulation job with the SAME per-camera
 * parameters, and the triangulated points must reproject back onto the
 * observed pixels as cleanly as in the zero-distortion case.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TriangulationDistortionWebTest {

    private static final Intrinsics K = new Intrinsics(1000.0, 1000.0, 960.0, 540.0);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    private final PinholeProjector projector = new PinholeProjector();

    @Test
    void perCameraDistortionIsParsedAndRoundTripClosesOverHttp() throws Exception {
        Distortion d1 = new Distortion(-0.28, 0.07, 0.0015, -0.0010);
        Distortion d2 = new Distortion(-0.18, 0.04, -0.0008, 0.0012);
        CameraPose pose1 = new CameraPose(
                new double[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}}, new double[]{0, 0, 0});
        CameraPose pose2 = new CameraPose(
                new double[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}}, new double[]{-1, 0, 0});

        List<Map<String, Object>> matches = new ArrayList<>();
        List<Point3D> truth = new ArrayList<>();
        for (double dz : new double[]{-0.5, 0.5}) {
            for (double dy : new double[]{-0.5, 0.5}) {
                for (double dx : new double[]{-0.5, 0.5}) {
                    Point3D world = new Point3D(dx, dy, 5.0 + dz);
                    truth.add(world);
                    Pixel p1 = projector.project(K, d1, pose1.toCameraCoordinates(world));
                    Pixel p2 = projector.project(K, d2, pose2.toCameraCoordinates(world));
                    matches.add(obj("u1", p1.u(), "v1", p1.v(), "u2", p2.u(), "v2", p2.v()));
                }
            }
        }

        Map<String, Object> job = obj(
                "camera1", obj(
                        "intrinsics", obj("fx", 1000.0, "fy", 1000.0, "cx", 960.0, "cy", 540.0),
                        "distortion", obj("k1", d1.k1(), "k2", d1.k2(), "p1", d1.p1(), "p2", d1.p2()),
                        "pose", obj(
                                "rotation", List.of(List.of(1, 0, 0), List.of(0, 1, 0), List.of(0, 0, 1)),
                                "translation", List.of(0, 0, 0))),
                "camera2", obj(
                        "intrinsics", obj("fx", 1000.0, "fy", 1000.0, "cx", 960.0, "cy", 540.0),
                        "distortion", obj("k1", d2.k1(), "k2", d2.k2(), "p1", d2.p1(), "p2", d2.p2()),
                        "pose", obj(
                                "rotation", List.of(List.of(1, 0, 0), List.of(0, 1, 0), List.of(0, 0, 1)),
                                "translation", List.of(-1, 0, 0))),
                "matches", matches);

        JsonNode response = om.readTree(mvc.perform(post("/api/v1/triangulations/jobs")
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(job)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        assertEquals(8, response.get("matchCount").asInt());
        assertTrue(response.get("maxReprojectionErrorPixels").asDouble() < 1e-6,
                "max reprojection error was " + response.get("maxReprojectionErrorPixels").asDouble());
        assertTrue(response.get("meanReprojectionErrorPixels").asDouble() < 1e-6);
        for (int i = 0; i < truth.size(); i++) {
            JsonNode point = response.get("matches").get(i).get("point");
            assertEquals(truth.get(i).x(), point.get("x").asDouble(), 1e-6);
            assertEquals(truth.get(i).y(), point.get("y").asDouble(), 1e-6);
            assertEquals(truth.get(i).z(), point.get("z").asDouble(), 1e-6);
        }
    }

    @Test
    void partialCameraDistortionObjectIsRejectedWithTypedError() throws Exception {
        Map<String, Object> job = obj(
                "camera1", obj(
                        "intrinsics", obj("fx", 1000.0, "fy", 1000.0, "cx", 960.0, "cy", 540.0),
                        "distortion", obj("k1", -0.1, "k2", 0.0, "p1", 0.0), // p2 missing
                        "pose", obj(
                                "rotation", List.of(List.of(1, 0, 0), List.of(0, 1, 0), List.of(0, 0, 1)),
                                "translation", List.of(0, 0, 0))),
                "camera2", obj(
                        "intrinsics", obj("fx", 1000.0, "fy", 1000.0, "cx", 960.0, "cy", 540.0),
                        "pose", obj(
                                "rotation", List.of(List.of(1, 0, 0), List.of(0, 1, 0), List.of(0, 0, 1)),
                                "translation", List.of(-1, 0, 0))),
                "matches", List.of(obj("u1", 860.0, "v1", 490.0, "u2", 660.0, "v2", 490.0)));

        mvc.perform(post("/api/v1/triangulations/jobs")
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(job)))
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertEquals("MISSING_DISTORTION_FIELD",
                                om.readTree(result.getResponse().getContentAsString()).get("type").asText()));
    }
}
