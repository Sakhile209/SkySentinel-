# Phase 1 API

## GET /api/health

No authentication. Readiness only; discloses no tenant or operational information.

HTTP 200, `Content-Type: application/json`:

```json
{"status":"UP","service":"skysentinel-security","database":"UP"}
```

HTTP 503 when the running backend cannot query PostgreSQL:

```json
{"status":"DOWN","service":"skysentinel-security","database":"DOWN"}
```

A stopped backend is unreachable; the frontend reports a connection error. It must
never display invented healthy state. Other API paths are denied in this phase.
Authentication, validated workflow requests and the operational error contract
will be added with their respective features.
