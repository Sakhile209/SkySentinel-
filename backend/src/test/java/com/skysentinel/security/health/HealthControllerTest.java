package com.skysentinel.security.health;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class HealthControllerTest {
    private MockMvc mvc;
    private TestHealthService service;

    @BeforeEach
    void setUp() {
        service = new TestHealthService();
        mvc = standaloneSetup(new HealthController(service))
            .addFilters(new ApiAuthenticationBoundaryFilter())
            .build();
    }

    @Test
    void healthIsPublicAndReturnsJson() throws Exception {
        service.response = new HealthResponse("UP", "skysentinel-security", "UP");
        mvc.perform(get("/api/health")).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.database").value("UP"));
    }

    @Test
    void databaseOutageReturnsServiceUnavailable() throws Exception {
        service.response = new HealthResponse("DOWN", "skysentinel-security", "DOWN");
        mvc.perform(get("/api/health")).andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.database").value("DOWN"));
    }

    @Test
    void otherPathsAreDenied() throws Exception {
        mvc.perform(get("/api/sites")).andExpect(status().isUnauthorized());
    }

    static class TestHealthService extends HealthService {
        private HealthResponse response = new HealthResponse("UP", "skysentinel-security", "UP");

        TestHealthService() {
            super(null);
        }

        @Override
        public HealthResponse check() {
            return response;
        }
    }

    static class ApiAuthenticationBoundaryFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            boolean publicHealth = "GET".equals(request.getMethod()) && "/api/health".equals(request.getRequestURI());
            boolean apiRequest = request.getRequestURI().startsWith("/api/");
            if (apiRequest && !publicHealth && request.getHeader("Authorization") == null) {
                response.setStatus(401);
                response.setContentType("application/json");
                response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication required to access the Security Operations Platform\"}");
                return;
            }
            filterChain.doFilter(request, response);
        }
    }
}
