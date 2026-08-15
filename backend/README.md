# Core Platform Runtime

Java 21 / Spring Boot runtime implementing the first production slice of the approved Core Platform specification.

## Run locally

From the workspace root:

```text
docker compose up --build
```

Backend: `http://localhost:8080`  
OpenAPI: `http://localhost:8080/swagger-ui`  
Readiness: `http://localhost:8080/actuator/health/readiness`

Demo account: `admin@core.local` / `Core@2026`; MFA code: `123456`.

The `demo` profile is the only profile that provisions the known demo password. Do not enable it for customer or production deployments.

## Implemented slice

- PostgreSQL/Flyway baseline and owned schemas
- BCrypt local identity, expiring MFA challenge and hashed opaque sessions
- login, MFA, current-user and logout APIs
- security audit records
- authenticated Control Plane bootstrap API backed by demo records
- modules, resources, activity, roles, outbox/jobs and file metadata
- liveness/readiness, OpenAPI, CORS and Problem Details errors
- reproducible OCI image and Docker Compose environment

Further capabilities remain governed by `technical-delivery-pack-v1.0/02-implementation-backlog.md`.
