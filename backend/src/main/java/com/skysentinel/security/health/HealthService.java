package com.skysentinel.security.health;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class HealthService {
    private final JdbcTemplate jdbc;

    public HealthService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public HealthResponse check() {
        try {
            boolean connected = Integer.valueOf(1).equals(jdbc.queryForObject("SELECT 1", Integer.class));
            return new HealthResponse(connected ? "UP" : "DOWN", "skysentinel-security", connected ? "UP" : "DOWN");
        } catch (DataAccessException exception) {
            // Do not expose connection strings, credentials or database errors to clients.
            return new HealthResponse("DOWN", "skysentinel-security", "DOWN");
        }
    }
}
