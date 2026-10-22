package com.laundry.modules.service;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.laundry.common.security.JwtUtil;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// Chạy trên DB thật; @Transactional rollback mọi dịch vụ tạo trong test
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ServiceTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtUtil jwtUtil;

    String admin;
    String staff;

    @BeforeEach
    void tokens() {
        admin = "Bearer " + jwtUtil.generateToken("admin", List.of("ADMIN"));
        staff = "Bearer " + jwtUtil.generateToken("admin", List.of("STAFF"));
    }

    private String uniqueName() {
        return "Test " + UUID.randomUUID().toString().substring(0, 8);
    }

    private long create(String name) throws Exception {
        String body = """
                {"name":"%s","unit":"KG","currentPrice":25000,"standardHours":24,
                 "expressSupported":true,"expressSurcharge":12500,"expressHours":6}
                """.formatted(name);
        String json = mvc.perform(post("/api/services").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    @Test
    void listIsPagedFilteredAndReturnsUnitCode() throws Exception {
        String name = uniqueName();
        create(name);

        mvc.perform(get("/api/services").header("Authorization", staff)
                        .param("search", name.toLowerCase()).param("status", "ACTIVE").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value(name))
                .andExpect(jsonPath("$.content[0].unit").value("KG"))
                .andExpect(jsonPath("$..totalElements").value(1));
    }

    @Test
    void activeEndpointReturnsOnlyActiveServices() throws Exception {
        create(uniqueName());

        mvc.perform(get("/api/services/active").header("Authorization", staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.status != 'ACTIVE')]").isEmpty())
                .andExpect(jsonPath("$[0].unit").exists());
    }

    @Test
    void updateCanChangeStatusInSameRequest() throws Exception {
        long id = create(uniqueName());

        mvc.perform(put("/api/services/{id}", id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPrice\":28000,\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPrice").value(28000))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void expressLongerThanStandardIsRejectedInVietnamese() throws Exception {
        String body = """
                {"name":"%s","unit":"KG","currentPrice":25000,"standardHours":4,
                 "expressSupported":true,"expressSurcharge":1000,"expressHours":6}
                """.formatted(uniqueName());

        mvc.perform(post("/api/services").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("phải ngắn hơn thời gian xử lý chuẩn")));
    }

    @Test
    void unknownServiceReturns404InVietnamese() throws Exception {
        mvc.perform(get("/api/services/{id}", 999_999_999L).header("Authorization", staff))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString("Dịch vụ không tồn tại")));
    }

    @Test
    void staffCannotUpdateService() throws Exception {
        long id = create(uniqueName());

        mvc.perform(put("/api/services/{id}", id).header("Authorization", staff)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"currentPrice\":1}"))
                .andExpect(status().isForbidden());
    }
}
