package com.laundry.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtUtil jwtUtil;

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void protectedPathWithoutTokenReturns401ProblemDetail() throws Exception {
        mvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        mvc.perform(get("/api/orders").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenPassesSecurity() throws Exception {
        String token = jwtUtil.generateToken("admin", List.of("ADMIN"));
        // Qua được security; route chưa tồn tại nên không phải 401/403
        mvc.perform(get("/api/orders").header("Authorization", "Bearer " + token))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    if (s == 401 || s == 403) throw new AssertionError("blocked by security: " + s);
                });
    }
}
