# Phase 1 acceptance and verification

## Expected behaviour (defined before implementation)

1. PostgreSQL starts with environment-supplied credentials and persistent storage.
2. Spring Boot connects, applies migration 1, and GET `/api/health` returns 200
   with service and database `UP`.
3. If the running backend loses database access, the endpoint returns 503 with
   `DOWN` and no internal database details.
4. React renders a checking state, then the actual API result. Network and HTTP
   failures produce a visible unavailable state; retry can recover.
5. Other API routes remain denied until explicitly implemented and authorized.
6. No operational backend domain feature is created. At the user’s later request,
   the frontend presents a demo dashboard matching the approved layout.

## Automated verification on 2026-09-25

- Backend service and MVC tests: **5 passed**, executed on Java 21.0.11.
- Frontend component tests: **5 passed** using Vitest 4.1.11, including incident selection, tabs
  and disabled operational actions.
- Frontend production build: **passed** using Vite 7.3.6.
- Backend executable JAR packaging: **passed**; migration and configuration
  verified inside the archive.
- npm install audit after updating Vitest: **0 reported vulnerabilities**.
- Compose configuration validation: **passed**, using an ephemeral dummy password
  solely for syntax validation, not to start a database.
- Full Maven `verify`: **passed after starting Docker Desktop**, including all
  5 service/MVC tests and 1 PostgreSQL integration test. The initial attempt was
  blocked by the stopped Docker daemon.
- Live development startup: **passed**. PostgreSQL is healthy in Docker; the
  Java 21 backend and Vite frontend are running as local processes. HTTP requests
  to both `:8080/api/health` and `:5173/api/health` returned database `UP`.
- Frontend document returned HTTP 200. The dashboard preview was rendered and
  visually inspected in headless Chromium. The complete containerized
  backend/frontend builds remain unverified.

The environment has a Java 21 runtime without `javac`. Verification compiled with
JDK 25 using `--release 21` and explicitly ran tests on Java 21:

```bash
JAVA_HOME=/opt/java/jdk-25.0.2+10 mvn -f backend/pom.xml test \
  -Djvm=/usr/lib/jvm/java-21-openjdk-amd64/bin/java
```

The normal documented setup installs a full JDK 21. Docker builds also use JDK 21.

## Your manual acceptance run

Follow README setup, then:

```bash
docker compose up --build -d --wait --wait-timeout 240
docker compose ps
curl -i http://localhost:8080/api/health
curl -i http://localhost:5173/api/health
```

Expect HTTP 200 and `UP` for both calls. Open http://localhost:5173 and check the
connected status. To test failure recovery on this disposable local foundation:

```bash
docker compose stop postgres
curl -i http://localhost:8080/api/health
# Expect HTTP 503 with DOWN; click Check again in the frontend.
docker compose start postgres
# Wait for PostgreSQL to become healthy, then repeat curl and Check again.
docker compose ps
```

Run `mvn -f backend/pom.xml verify` with JDK 21 and Docker running to exercise the
real PostgreSQL integration test. It starts an isolated temporary database,
asserts the HTTP response and checks the Flyway history table.

Send the test output or error before Phase 2 begins.

Suggested commit: `feat: add Phase 1 application foundation`
