# Java Core Platform

Java Core Platform is a business-neutral modular application foundation for building independent customer solutions. The repository contains the approved architecture documents, a Java 21/Spring Boot runtime slice, PostgreSQL migrations, the Control Plane frontend and Ubuntu deployment assets.

## Repository layout

- `backend/` — Spring Boot runtime, local identity, MFA, hashed sessions, audit and Control Plane API
- `frontend/` — Core Platform Control Plane UI
- `deploy/ubuntu20/` — systemd, Nginx, environment and deployment/rollback assets
- `technical-delivery-pack-v1.0/` — implementation specification, backlog and quality gates
- `core-platform-*-v1.0.md` — approved BA, runtime and database architecture documents

## Local start

```text
docker compose up --build
```

Backend readiness: `http://localhost:8080/actuator/health/readiness`  
Backend OpenAPI: `http://localhost:8080/swagger-ui`

Frontend:

```text
cd frontend
npm install
npm run dev
```

Set `NEXT_PUBLIC_CORE_API_URL=http://localhost:8080` for local API integration.

Demo-only credentials are documented in `backend/README.md`. Never enable the `demo` Spring profile for customer production environments.
