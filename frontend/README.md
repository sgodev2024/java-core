# Core Platform Control Plane — Next.js

Frontend quản trị và Workspace nghiệp vụ của Java Core Platform. Runtime sử dụng Next.js App Router chính thức; không dùng vinext, Vite, Cloudflare Worker hoặc Sites hosting.

## Yêu cầu

- Node.js 22+
- Backend Core Platform có thể truy cập từ trình duyệt

## Chạy local

```bash
npm ci
NEXT_PUBLIC_CORE_API_URL=http://localhost:8080 npm run dev
```

Mở `http://localhost:3000`.

## Build và kiểm thử

```bash
npm run lint
npm test
```

`npm test` chạy `next build`, chuẩn bị static/public asset cho standalone output, khởi động `.next/standalone/server.js` và kiểm tra SSR login shell.

## Docker production

```bash
docker build \
  --build-arg NEXT_PUBLIC_CORE_API_URL=https://api.corejava.sgodata.com \
  -t core-platform-frontend:release .
```

Image cuối chạy non-root bằng `node server.js`, lắng nghe cổng `3000` và chỉ chứa output standalone cần thiết.

## Authentication contract

- `CORE_MFA_ENABLED=true`: login trả challenge, UI hiển thị màn hình TOTP.
- `CORE_MFA_ENABLED=false`: login trả `mfaRequired=false` cùng `session`, UI vào hệ thống ngay sau password.
- Frontend không tự quyết định bỏ MFA; backend là nguồn quyết định cuối cùng.

## Navigation contract

Frontend gọi `GET /api/v1/navigation/me` sau khi đăng nhập. Workspace, sidebar, favorites, recent items và Command Palette đều dựng từ manifest backend đã lọc quyền; không thêm menu cố định trong UI shell.

## Sổ thay đổi kỹ thuật

Chạy `npm run docs:changes` để tái tạo phần lịch sử trong `../docs/technical-change-register.md`. GitHub Actions tự chạy bước này sau mỗi push lên `main`.
