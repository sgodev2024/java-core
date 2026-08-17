# Java Core Platform

Java Core Platform is a business-neutral modular application foundation for building independent customer solutions. The repository contains the approved architecture documents, a Java 21/Spring Boot runtime slice, PostgreSQL migrations, the Control Plane frontend and Ubuntu deployment assets.

## Repository layout

- `backend/` — Spring Boot runtime, local identity, MFA, hashed sessions, audit and Control Plane API
- `frontend/` — Core Platform Control Plane UI
- `deploy/ubuntu20/` — systemd, Nginx, environment and deployment/rollback assets
- `technical-delivery-pack-v1.0/` — implementation specification, backlog and quality gates
- `core-platform-*-v1.0.md` — approved BA, runtime and database architecture documents
- `docs/navigation-registry.md` — Workspace, dynamic menu manifest, security and extension contract

## Local start

```text
docker compose up --build
```

Ba service sẽ chạy: PostgreSQL 17 (`:5432`), backend (`:8080`), Control Plane UI (`:3000`).

Database dùng hai credential (E2): `core_admin` cho migration/DDL (`DB_MIGRATION_USER`) và `core_app` cho runtime (`DB_USER`, chỉ DML + chịu RLS theo tenant). Thay đổi roles/seed xong cần `docker compose down -v` để tạo lại volume.

- UI: `http://localhost:3000` — đăng nhập `admin@core.local` / `Core@2026`, mã MFA `123456` (chỉ dùng cho môi trường demo local).
- Backend readiness: `http://localhost:8080/actuator/health/readiness`
- Backend OpenAPI: `http://localhost:8080/swagger-ui`

Lưu ý build frontend: image frontend chỉ chứa bản build (`dist/`), nên phải build trước với API URL đúng môi trường rồi mới `docker compose build frontend`:

```text
cd frontend
NEXT_PUBLIC_CORE_API_URL=http://localhost:8080 npm run build
```

Trên Windows, hãy chạy frontend qua Docker Compose thay vì `node dist/standalone/server.js` trực tiếp: `StaticFileCache` của vinext tạo cache key bằng path separator của OS (`\`), khiến mọi request `/assets/*` trả 404 khi chạy server standalone trên Windows. Trong container Linux asset phục vụ bình thường.

## Build and test

Backend (Java 21, PostgreSQL qua Testcontainers; máy không có Docker xem `backend/README.md`):

```text
cd backend && ./mvnw verify
```

Frontend (Node 22+, build + SSR smoke test):

```text
cd frontend && npm test
```

CI chạy cả hai trên mọi pull request (`.github/workflows/`).

Frontend:

```text
cd frontend
npm install
npm run dev
```

Set `NEXT_PUBLIC_CORE_API_URL=http://localhost:8080` for local API integration.

Demo-only credentials are documented in `backend/README.md`. Never enable the `demo` Spring profile for customer production environments.
