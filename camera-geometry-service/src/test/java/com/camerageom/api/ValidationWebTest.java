package com.camerageom.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static com.camerageom.api.TestJson.obj;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every invalid input must be rejected with a typed, structured error BEFORE
 * any computation — no uncaught exceptions, no empty results.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ValidationWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    private Map<String, Object> validProjectionJob() {
        return obj(
                "intrinsics", obj("fx", 800.0, "fy", 820.0, "cx", 640.0, "cy", 360.0),
                "image", obj("width", 1280, "height", 720),
                "points", List.of(obj("x", 0.3, "y", -0.2, "z", 2.0)));
    }

    private Map<String, Object> validTriangulationJob() {
        Map<String, Object> pose1 = obj(
                "rotation", List.of(List.of(1, 0, 0), List.of(0, 1, 0), List.of(0, 0, 1)),
                "translation", List.of(0, 0, 0));
        Map<String, Object> pose2 = obj(
                "rotation", List.of(List.of(1, 0, 0), List.of(0, 1, 0), List.of(0, 0, 1)),
                "translation", List.of(-1, 0, 0));
        Map<String, Object> intrinsics = obj("fx", 1000.0, "fy", 1000.0, "cx", 960.0, "cy", 540.0);
        return obj(
                "camera1", obj("intrinsics", intrinsics, "pose", pose1),
                "camera2", obj("intrinsics", intrinsics, "pose", pose2),
                "matches", List.of(obj("u1", 860.0, "v1", 490.0, "u2", 660.0, "v2", 490.0)));
    }

    @Test
    void rejectsPointBehindCamera() throws Exception {
        Map<String, Object> job = validProjectionJob();
        job.put("points", List.of(
                obj("x", 0.3, "y", -0.2, "z", 2.0),
                obj("x", 0.1, "y", 0.1, "z", 0.0),
                obj("x", 0.0, "y", 0.0, "z", -1.5)));
        mvc.perform(post("/api/v1/projections/jobs")
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(job)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("POINT_BEHIND_CAMERA"))
                .andExpect(jsonPath("$.details.pointIndices[0]").value(1))
                .andExpect(jsonPath("$.details.pointIndices[1]").value(2));
    }

    @Test
    void rejectsNonPositiveFocalLength() throws Exception {
        Map<String, Object> job = validProjectionJob();
        job.put("intrinsics", obj("fx", -800.0, "fy", 820.0, "cx", 640.0, "cy", 360.0));
        mvc.perform(post("/api/v1/projections/jobs")
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(job)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("NON_POSITIVE_FOCAL_LENGTH"));
    }

    @Test
    void rejectsZeroImageDimension() throws Exception {
        Map<String, Object> job = validProjectionJob();
        job.put("image", obj("width", 0, "height", 720));
        mvc.perform(post("/api/v1/projections/jobs")
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(job)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("INVALID_IMAGE_SIZE"));
    }

    @Test
    void rejectsMissingIntrinsicsField() throws Exception {
        Map<String, Object> job = validProjectionJob();
        job.put("intrinsics", obj("fx", 800.0, "cx", 640.0, "cy", 360.0)); // fy missing
        mvc.perform(post("/api/v1/projections/jobs")
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(job)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("MISSING_INTRINSICS_FIELD"))
                .andExpect(jsonPath("$.details.missingFields[0]").value("fy"));
    }

    @Test
    void rejectsEmptyPointList() throws Exception {
        Map<String, Object> job = validProjectionJob();
        job.put("points", List.of());
        mvc.perform(post("/api/v1/projections/jobs")
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(job)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("EMPTY_POINT_LIST"));
    }

    @Test
    void rejectsMissingCameraPoseInTriangulation() throws Exception {
        Map<String, Object> job = validTriangulationJob();
        // camera2 carries intrinsics but no pose at all
        job.put("camera2", obj("intrinsics", obj("fx", 1000.0, "fy", 1000.0, "cx", 960.0, "cy", 540.0)));
        mvc.perform(post("/api/v1/triangulations/jobs")
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(job)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("MISSING_CAMERA_POSE"));
    }

    @Test
    void rejectsEmptyMatchList() throws Exception {
        Map<String, Object> job = validTriangulationJob();
        job.put("matches", List.of());
        mvc.perform(post("/api/v1/triangulations/jobs")
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(job)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("EMPTY_MATCH_LIST"));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mvc.perform(post("/api/v1/projections/jobs")
                        .contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("MALFORMED_REQUEST"));
    }
}
