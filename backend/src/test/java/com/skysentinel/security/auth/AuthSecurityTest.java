package com.skysentinel.security.auth;

import com.skysentinel.security.config.JwtAuthenticationFilter;
import com.skysentinel.security.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
class AuthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void forgotPasswordEndpointDoesNotRequireAuthentication() throws Exception {
        given(authService.forgotPassword(any(ForgotPasswordRequest.class)))
                .willReturn(new OtpChallengeResponse("challenge-1", "A 6-digit verification code has been sent to your registered phone number.", Instant.now().plusSeconds(300)));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ops@skysentinel.test\",\"cellphoneNumber\":\"0821234567\"}"))
                .andExpect(status().isOk());
    }
}
