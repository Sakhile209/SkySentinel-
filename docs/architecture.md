# Architecture

The React client uses relative `/api` URLs. Vite proxies them locally; Nginx proxies
them in Docker. Spring controllers translate HTTP into service calls and return
response DTOs. Future domain repositories will own persistence through JPA.

- `SkySentinelApplication`: starts Spring and discovers application components.
- `SecurityConfig`: permits only GET `/api/health`; other requests are denied.
  No generated development user is created. CSRF protection remains enabled.
- `HealthController`: chooses HTTP 200 or 503 from the service result.
- `HealthService`: queries PostgreSQL and suppresses internal connection details.
- `HealthResponse`: immutable JSON contract, independent of persistence entities.
- Flyway: runs ordered SQL migrations from `database/migrations`, packaged by
  Maven into the backend JAR. Never edit applied migrations; add a new version.
- Hibernate validates schema; it must not silently create or update domain tables.

Only the health feature exists. Avoid empty speculative service/repository/entity
classes. Add them when their phase introduces tested behaviour. Future company
boundaries must be enforced in services/repositories as well as HTTP authorization.

Container dependencies use readiness healthchecks. PostgreSQL keeps data in a
named volume. Application containers run as non-root users. Host ports bind to
loopback. TLS termination and trusted forwarded-header configuration belong to
Phase 11; relative API URLs allow deployment behind HTTPS without client changes.

## Visual contract

[Approved dashboard](design/approved-dashboard.png): compact dark navy panels,
thin borders, cyan/blue highlights, red urgent incidents, amber warnings and green
healthy states. Preserve its sidebar, header, KPI strip, three-column incidents/map/
details row, fleet/mission/response/feed row and bottom information panels in Phase 8.
Do not mistake screenshot sample labels or geographic distances for domain data
or approved coverage. The user requested this layout early. The frontend now presents it with demo
fixtures and an illustrative SVG map/feed; only the health indicator is connected
to the backend. Incident selection and detail tabs work locally. Mission, dispatch
and configuration actions remain disabled. No operational backend scope changed.
