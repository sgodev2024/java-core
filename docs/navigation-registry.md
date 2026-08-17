# Unified Navigation Registry v1.1

## Mục tiêu

Navigation Registry cung cấp một cây điều hướng hợp nhất cho ứng dụng dedicated deployment. Module có thể đăng ký menu mà không sửa Core shell. Backend là nguồn quyết định cuối cùng cho manifest hiệu lực; frontend chỉ render các section/page đã được lọc.

Baseline này thay thế mô hình tách Workspace nghiệp vụ và Workspace quản trị. `NavigationWorkspaceDescriptor` tạm thời được giữ như adapter nội bộ cho contributor v1.0, nhưng API công khai dùng khái niệm **section**.

## Runtime flow

1. Mỗi `ModuleContributor` khai báo section adapter và `NavigationItemDescriptor`.
2. `NavigationRegistry` thu thập, chuẩn hóa và kiểm tra toàn bộ manifest trước khi application Ready.
3. `GET /api/v1/navigation/me` lọc theo module đang bật, authority, permission resource/action và tenant/account hiện tại.
4. Backend loại page không được phép, sau đó loại group/section rỗng.
5. Frontend render một sidebar, favorites và Command Palette từ `sections[]`.
6. `PUT /api/v1/navigation/me/preferences` lưu yêu thích và mục gần đây theo tài khoản.

Registry fail startup khi có duplicate key, namespace module sai, section/parent thiếu, parent khác section, group lồng group, route ngoài ứng dụng, parent cycle, visibility mode sai hoặc permission khai báo thiếu resource/action.

## Section chuẩn

| Key | Sort order | Đối tượng |
|---|---:|---|
| `business` | 20 | Người dùng nghiệp vụ và System Administrator |
| `system-administration` | 90 | `ROLE_PLATFORM_ADMIN` |

Section nghiệp vụ được đặt trước. Quản trị hệ thống luôn ở cuối sidebar. Không tạo một section/Workspace riêng cho từng module chỉ để chứa một vài page.

## Cấu trúc ba cấp

Cây hợp lệ duy nhất:

```text
Section
├── Page
└── Group
    └── Page
```

Group không được chứa group. Giới hạn này giữ sidebar ổn định khi số module tăng và giúp tìm kiếm/điều hướng không phụ thuộc cây lồng sâu.

## Namespace

- Core platform: `core.*`
- Module tái sử dụng: `module.<module-key>.*`
- Extension khách hàng: `customer.<customer-key>.*`

Với namespace `module.*`, registry bắt buộc module sở hữu item phải trùng `<module-key>`.

## Visibility mode

| Mode | Mục đích | Admin bypass khi dựng menu |
|---|---|---:|
| `ACCESS` | Page chức năng/quản trị thông thường | Có, nếu authority section/item cho phép |
| `ASSIGNMENT` | Hộp việc, tác vụ cá nhân, hàng đợi được giao | Không |

`ASSIGNMENT` bắt buộc có đủ `permissionResource` và `permissionAction`. Đây là triển khai FE-BA-13: vai trò System Administrator không tự làm xuất hiện “Công việc của tôi”. PDP dùng exact-policy gate; wildcard `*/*` của administrator không được tính là nhiệm vụ được giao. Chỉ policy đúng resource/action mới làm page này xuất hiện.

## Ví dụ page module

```java
@Override public List<NavigationItemDescriptor> navigationItems() {
  return List.of(new NavigationItemDescriptor(
      "module.sales.orders",
      "business",
      "",
      "Đơn bán hàng",
      "navigation.salesOrders",
      "□",
      "sales-orders",
      "/business/sales-orders",
      20,
      "",
      "SALES_ORDER",
      "READ",
      List.of("sales", "đơn hàng")));
}
```

Ví dụ task inbox:

```java
new NavigationItemDescriptor(
    "module.approval-domain.my-work",
    "business",
    "",
    "Công việc của tôi",
    "navigation.myWork",
    "▣",
    "my-work",
    "/business/my-work",
    15,
    "",
    "WORK_ITEM",
    "READ_ASSIGNED",
    "ASSIGNMENT",
    List.of("tác vụ", "được giao"));
```

Không đăng ký task inbox cho đến khi module có endpoint và permission kiểm chứng nhiệm vụ thực tế.

## API contract

```http
GET /api/v1/navigation/me
Authorization: Bearer <access-token>
```

```json
{
  "revision": "a1b2c3d4e5f6",
  "sections": [
    {
      "key": "business",
      "label": "Nghiệp vụ",
      "labelKey": "navigation.section.business",
      "icon": "▦",
      "sortOrder": 20,
      "items": [
        {
          "key": "core.home",
          "parentKey": "",
          "ownerModule": "kernel",
          "label": "Trang chủ",
          "type": "PAGE",
          "viewKey": "home",
          "route": "/home",
          "sortOrder": 10,
          "keywords": ["trang chủ", "tổng quan"]
        }
      ]
    }
  ],
  "favoriteKeys": [],
  "recentKeys": []
}
```

Cập nhật preference:

```http
PUT /api/v1/navigation/me/preferences
Content-Type: application/json

{
  "favoriteKeys": ["core.modules"],
  "recentKeys": ["module.sales.orders", "core.modules"]
}
```

Backend loại key không còn hiển thị, giới hạn 20 favorite và 10 recent. Cột `last_workspace_key` được giữ tương thích database và được reset thành chuỗi rỗng; API v1.1 không công bố hoặc sử dụng giá trị Workspace nữa.

## Security invariants

- Ẩn menu không thay thế authorization tại endpoint.
- Navigation permission dùng cùng `PermissionService` với API nghiệp vụ và fail closed.
- `ACCESS` có thể dùng admin bypass khi dựng menu; endpoint đích vẫn tự kiểm tra quyền.
- `ASSIGNMENT` không dùng admin bypass và không nhận wildcard `*/*` khi dựng menu.
- Module `DISABLED` không đóng góp menu.
- Frontend không có fallback menu hard-code và không suy diễn quyền từ role code.
- Page chỉ được mở nếu xuất hiện trong manifest hiệu lực của phiên hiện tại.

## Checklist khi thêm module

1. Khai báo module descriptor và navigation manifest.
2. Dùng namespace đúng owner module.
3. Chọn section/group đã tồn tại hoặc đóng góp section có key duy nhất.
4. Không tạo group lồng group.
5. Gắn permission tương ứng endpoint; dùng `ASSIGNMENT` cho task inbox.
6. Dùng route chuẩn `/business/...` hoặc `/administration/...`.
7. Đóng gói frontend view cùng module.
8. Viết test manifest hợp lệ, user có quyền, user không có quyền, admin với `ASSIGNMENT`, module disabled và direct route.
