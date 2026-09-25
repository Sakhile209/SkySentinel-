package com.skysentinel.security.health;

import com.skysentinel.security.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = HealthController.class,
    excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import(SecurityConfig.class)
class HealthControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean HealthService service;

    @Test
    void healthIsPublicAndReturnsJson() throws Exception {
        when(service.check()).thenReturn(new HealthResponse("UP", "skysentinel-security", "UP"));
        mvc.perform(get("/api/health")).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.database").value("UP"));
    }

    @Test
    void databaseOutageReturnsServiceUnavailable() throws Exception {
        when(service.check()).thenReturn(new HealthResponse("DOWN", "skysentinel-security", "DOWN"));
        mvc.perform(get("/api/health")).andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.database").value("DOWN"));
    }

    @Test
    void otherPathsAreDenied() throws Exception {
        mvc.perform(get("/api/sites")).andExpect(status().isForbidden());
    }
}
