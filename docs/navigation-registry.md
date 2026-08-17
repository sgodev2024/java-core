# Workspace & Navigation Registry

## Mục tiêu

Navigation Registry tách không gian nghiệp vụ khỏi Control Plane và cho phép module đăng ký menu mà không sửa Core shell. Backend là nguồn quyết định cuối cùng cho menu hiệu lực; frontend không chứa danh sách menu cố định.

## Runtime flow

1. Mỗi `ModuleContributor` khai báo `NavigationWorkspaceDescriptor` và/hoặc `NavigationItemDescriptor`.
2. `NavigationRegistry` thu thập và kiểm tra toàn bộ manifest trước khi application Ready.
3. `GET /api/v1/navigation/me` lọc manifest theo module đang bật, authority, policy resource/action và tenant/account hiện tại.
4. Frontend render workspace/sidebar/command palette từ response này.
5. `PUT /api/v1/navigation/me/preferences` lưu Workspace gần nhất, yêu thích và gần đây theo tài khoản.

Registry fail startup khi có duplicate key, namespace module sai, workspace/parent thiếu, parent khác workspace, parent cycle, route ngoài ứng dụng hoặc permission khai báo thiếu resource/action.

## Workspace chuẩn

| Key | Category | Đối tượng |
|---|---|---|
| `business` | `BUSINESS` | Người dùng nghiệp vụ và Platform Admin |
| `core-admin` | `ADMIN` | Chỉ `ROLE_PLATFORM_ADMIN` |

Control Plane không được trộn vào menu nghiệp vụ. Một workspace chỉ được trả về khi còn ít nhất một page người dùng được phép truy cập.

## Namespace

- Core platform: `core.*`
- Module tái sử dụng: `module.<module-key>.*`
- Extension khách hàng: `customer.<customer-key>.*`

Với namespace `module.*`, registry bắt buộc module sở hữu item phải trùng `<module-key>`.

## Ví dụ module manifest

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
      "/#/business/sales-orders",
      20,
      "",
      "SALES_ORDER",
      "READ",
      List.of("sales", "đơn hàng")));
}
```

Module không cần permission riêng có thể để trống cả `permissionResource` và `permissionAction`. Không được chỉ khai báo một trong hai.

## API contract

```http
GET /api/v1/navigation/me
Authorization: Bearer <access-token>
```

Response chứa `revision`, workspace hiệu lực, item phẳng với `parentKey`, `favoriteKeys` và `recentKeys`. Item loại `GROUP` không có `viewKey/route`; item loại `PAGE` luôn có hash route nội bộ.

```http
PUT /api/v1/navigation/me/preferences
Content-Type: application/json

{
  "favoriteKeys": ["core.modules"],
  "recentKeys": ["module.sales.orders", "core.modules"],
  "lastWorkspaceKey": "business"
}
```

Backend loại bỏ key không còn hiển thị, giới hạn 20 favorite và 10 recent. Dữ liệu được lưu trong `platform.navigation_preference`, áp dụng tenant RLS và ghi audit.

## Security invariants

- Ẩn menu không thay thế authorization tại endpoint.
- Navigation permission dùng cùng `PermissionService` với API nghiệp vụ và fail-closed.
- Platform Admin chỉ bypass permission cho việc dựng menu; endpoint nghiệp vụ vẫn tự kiểm tra quyền của endpoint.
- Module `DISABLED` không đóng góp menu. Kernel navigation luôn được giữ để Platform Admin có đường phục hồi module.
- Frontend chỉ tìm kiếm/hiển thị item backend đã cấp; không có fallback hard-code.

## Checklist khi thêm module

1. Khai báo module descriptor và navigation manifest.
2. Dùng namespace đúng owner module.
3. Chọn workspace và parent group đã tồn tại, hoặc đóng góp workspace mới có key duy nhất.
4. Gắn permission `READ` tương ứng với endpoint đích.
5. Bổ sung frontend view được đóng gói cùng module.
6. Viết test: manifest validation, user có quyền, user không có quyền, module disabled.
