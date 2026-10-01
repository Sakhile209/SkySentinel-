package com.skysentinel.security.health;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

class HealthServiceTest {
    @Test
    void reportsHealthyDatabase() {
        HealthService service = new HealthService(new StubJdbcTemplate(1, null));
        assertThat(service.check()).isEqualTo(new HealthResponse("UP", "skysentinel-security", "UP"));
    }

    @Test
    void databaseFailureDoesNotExposeInternalDetails() {
        HealthService service = new HealthService(new StubJdbcTemplate(null, new DataAccessResourceFailureException("sensitive connection details")));
        assertThat(service.check()).isEqualTo(new HealthResponse("DOWN", "skysentinel-security", "DOWN"));
    }

    private static class StubJdbcTemplate extends JdbcTemplate {
        private final Integer value;
        private final DataAccessResourceFailureException failure;

        private StubJdbcTemplate(Integer value, DataAccessResourceFailureException failure) {
            this.value = value;
            this.failure = failure;
        }

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType) {
            if (failure != null) {
                throw failure;
            }
            return requiredType.cast(value);
        }
    }
}
