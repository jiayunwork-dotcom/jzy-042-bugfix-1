package com.camerageom.api;

import com.fasterxml.jackson.databind.JsonNode;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Single-point projection and batch jobs share one projection function: the
 * same point through both entries must yield the identical pixel.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConsistencyWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @Test
    void singlePointAndBatchJobProduceIdenticalPixels() throws Exception {
        Map<String, Object> intrinsics = obj("fx", 1100.0, "fy", 1080.0, "cx", 960.0, "cy", 540.0);
        Map<String, Object> distortion = obj("k1", -0.12, "k2", 0.015, "p1", 0.001, "p2", -0.0005);
        Map<String, Object> image = obj("width", 1920, "height", 1080);
        Map<String, Object> point = obj("x", 0.35, "y", -0.25, "z", 1.8);

        String singleBody = om.writeValueAsString(obj(
                "intrinsics", intrinsics, "distortion", distortion, "image", image, "point", point));
        JsonNode single = om.readTree(mvc.perform(post("/api/v1/projections/point")
                        .contentType(MediaType.APPLICATION_JSON).content(singleBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        String batchBody = om.writeValueAsString(obj(
                "intrinsics", intrinsics, "distortion", distortion, "image", image,
                "points", List.of(point)));
        JsonNode batch = om.readTree(mvc.perform(post("/api/v1/projections/jobs")
                        .contentType(MediaType.APPLICATION_JSON).content(batchBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        JsonNode batchPoint = batch.get("points").get(0);
        // Same code path, deterministic arithmetic: results must be bit-identical.
        assertEquals(single.get("u").asDouble(), batchPoint.get("u").asDouble(), 0.0);
        assertEquals(single.get("v").asDouble(), batchPoint.get("v").asDouble(), 0.0);
        assertEquals(single.get("inBounds").asBoolean(), batchPoint.get("inBounds").asBoolean());
    }
}
