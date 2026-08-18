# Core-to-Project Implementation Standard v1.0

- Mã: `SGO-ENG-CORE-PROJECT-001`
- Trạng thái: Baseline bắt buộc
- Áp dụng: mọi dự án khách hàng phát triển từ Java Core Platform
- Mục tiêu: dự án chứa full source chạy độc lập và bàn giao được; Core vẫn trung tính, có version và tái sử dụng được

## 1. Repository và quyền sở hữu

Mỗi dự án khách hàng phải có repository độc lập. Repository dự án chứa toàn bộ Core snapshot đã khóa, module nghiệp vụ, frontend, migration, deployment assets, test và tài liệu.

```text
java-core
   | baseline tag + compatibility contract
   v
customer-project
   +-- vn.coreplatform        Core snapshot
   +-- vn.<company>.<domain>  nghiệp vụ dự án
   +-- docs                   BA/contract/runbook
   +-- deploy                 compose/Nginx/backup
```

Remote chuẩn:

- `origin`: repository dự án, nhận toàn bộ code nghiệp vụ và là nguồn bàn giao.
- `upstream`: repository Core, chỉ dùng để xem và nâng baseline.
- `CORE_BASELINE`: ghi tag, SHA và ngày khóa Core.

Không phát triển nghiệp vụ khách hàng trên repository Core và không push code dự án vào `upstream`.

## 2. Phân loại thay đổi

| Thay đổi | Repository |
|---|---|
| Security, permission, module SPI, audit, outbox, file, observability dùng chung | Core |
| CRM, bán hàng, marketing, kho, workflow hoặc integration khách hàng | Dự án |
| Hotfix nghiệp vụ riêng | Dự án |
| Lỗi Core có ảnh hưởng nhiều dự án | Sửa Core, phát hành tag, sau đó nâng baseline dự án |

Một chức năng chỉ được đưa vào Core khi có ADR chứng minh tính trung lập nghiệp vụ và ít nhất hai use case dự án độc lập.

## 3. Khóa và nâng Core baseline

Khởi tạo dự án:

1. Core `main` sạch, CI đạt và không còn migration lỗi.
2. Gắn semantic tag dạng `core-vX.Y.Z-project-baseline`.
3. Export/clone từ đúng tag và ghi SHA vào `CORE_BASELINE`.
4. Tạo Git repository mới trước khi thêm nghiệp vụ.
5. Chạy lại regression Core trong repository dự án.

Nâng Core phải qua pull request riêng, có compatibility report, migration dry-run, regression, backup và rollback plan. Không tự động merge `upstream/main` và không tự nâng production.

## 4. Ranh giới module

- Core: namespace `vn.coreplatform`.
- Dự án: namespace riêng như `vn.sgodata.crm`.
- Module dự án chỉ dùng public contracts: kernel/module SPI, permission, audit, eventing và shared error contract.
- Cấm phụ thuộc controller, repository hoặc service nội bộ của module khác.
- ArchUnit kiểm tra boundary trên mọi pull request.
- Menu phải đăng ký qua Navigation Registry và lọc theo capability/permission.

Mỗi module nghiệp vụ phải có descriptor/version, Core compatibility range, navigation contribution, permission/resource descriptors, Flyway migration, API/error contract, audit/outbox event và test.

## 5. Database và dữ liệu

- Giai đoạn hiện tại: một khách hàng/một deployment/một database.
- Mỗi domain có schema riêng; không đặt bảng nghiệp vụ trong `public`.
- Bảng thuộc tổ chức phải có `tenant_id`, forced RLS và tenant policy.
- Migration dùng role DDL; runtime dùng role DML không có `BYPASSRLS`, `CREATEDB`, `CREATEROLE` hoặc quyền owner.
- Migration đã phát hành là bất biến; thay đổi bằng migration kế tiếp.
- Dữ liệu cá nhân phải phân loại, tối thiểu hóa, che/mã hóa/băm đúng mục đích.
- Backup và restore drill phải chứng minh RPO 15 phút, RTO 1 giờ trước production.

## 6. API, transaction và tích hợp

- API version `/api/v1`; lỗi theo `application/problem+json` kèm correlation ID.
- Server bắt buộc kiểm tra permission; ẩn menu không thay thế authorization.
- Transaction ghi dữ liệu bao gồm business state, audit và transactional outbox.
- Consumer bất đồng bộ và import phải idempotent.
- Import có checksum, batch status, lỗi theo dòng và reconciliation.
- Breaking contract phải tạo version mới và chính sách deprecation.

## 7. Frontend

- Dùng Next.js chuẩn và standalone production image.
- Ưu tiên same-origin: UI ở domain dự án, `/api/*` reverse proxy về backend.
- Không hard-code domain Core, credential, menu hoặc quyền.
- Mỗi màn hình có loading, empty, error, retry và responsive state.
- Production build, type-check và SSR smoke test là gate bắt buộc.

## 8. Secrets và cấu hình

- Chỉ commit `.env.example`; cấm commit `.env`, token, key, certificate và dump.
- Secret sinh ngẫu nhiên, lưu quyền `0600`, không xuất hiện trong log hoặc tài liệu.
- Tách password migration DB, runtime DB và bootstrap administrator.
- Bootstrap password phải đổi sau lần đăng nhập/bàn giao đầu tiên.
- MFA test/production tuân theo quyết định security được phê duyệt cho từng dự án.

## 9. Chuẩn triển khai

Mỗi dự án có Compose project name, container/image/volume/network, database, loopback ports, Nginx server block, file storage, backup path và log rotation riêng.

PostgreSQL không public Internet. Backend/frontend chỉ bind `127.0.0.1`; Nginx là ingress duy nhất. Cloudflare Flexible chỉ là cấu hình chuyển tiếp tạm thời; phải ghi risk và lập kế hoạch Full (Strict) với origin certificate.

## 10. Git, CI và release

- `main`: luôn có thể release.
- `codex/*` hoặc `feature/*`: thay đổi qua review.
- `hotfix/*`: lỗi production có incident reference.

Pull request chạy secret scan, backend regression, migration trên PostgreSQL sạch, architecture test, frontend build/test và dependency scan. Release gắn semantic tag và ghi commit SHA đang chạy.

Trình tự release:

1. Chốt BA, acceptance criteria và phạm vi.
2. Chốt commit/tag; CI và working tree sạch.
3. Backup database/file metadata và xác minh backup.
4. Fast-forward đúng repository dự án.
5. Validate Compose; build image theo commit SHA.
6. Chạy migration bằng role DDL.
7. Start và qua readiness/health gates.
8. Smoke test login, navigation, permission và critical business flow qua domain.
9. Ghi deployment report và known issues.
10. Giữ source/image release trước; không tự rollback migration phá hủy.

## 11. Definition of Done

Dự án chỉ sẵn sàng production khi BA/rules/contracts đã duyệt; không còn mock trên luồng chính; frontend gọi API thật; migration sạch/nâng cấp đều đạt; permission/RLS/audit/outbox đã test; backup/restore drill đạt; domain/TLS/monitoring/log/disk đã xác minh; repository/tag/runbook/full source đủ; credential mặc định đã thay.

## 12. Bộ tài liệu dự án bắt buộc

```text
docs/
  00-core-to-project-implementation-standard-v1.0.md
  01-business-analysis-v1.0.md
  02-data-discovery-plan-v1.0.md
  03-business-rules-v1.0.md
  04-data-and-integration-contracts-v1.0.md
  05-implementation-status-v1.0.md
  06-deployment-runbook-<environment>-v1.0.md
  adr/
CORE_BASELINE
README.md
```

Tài liệu thay đổi cùng pull request với code. Deployment report cập nhật sau mỗi lần triển khai và tuyệt đối không chứa password/token.

