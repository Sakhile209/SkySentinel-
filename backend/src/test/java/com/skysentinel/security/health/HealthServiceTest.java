package com.skysentinel.security.health;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class HealthServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final HealthService service = new HealthService(jdbc);

    @Test
    void reportsHealthyDatabase() {
        when(jdbc.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        assertThat(service.check()).isEqualTo(new HealthResponse("UP", "skysentinel-security", "UP"));
    }

    @Test
    void databaseFailureDoesNotExposeInternalDetails() {
        when(jdbc.queryForObject("SELECT 1", Integer.class))
            .thenThrow(new DataAccessResourceFailureException("sensitive connection details"));
        assertThat(service.check()).isEqualTo(new HealthResponse("DOWN", "skysentinel-security", "DOWN"));
    }
}
