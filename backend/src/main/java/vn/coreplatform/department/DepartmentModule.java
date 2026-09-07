package vn.coreplatform.department;

import java.util.List;
import org.springframework.stereotype.Component;
import vn.coreplatform.kernel.ModuleContributor;
import vn.coreplatform.kernel.ModuleDescriptor;
import vn.coreplatform.kernel.NavigationItemDescriptor;

@Component
public class DepartmentModule implements ModuleContributor {

    // 1. Đăng ký module
    @Override
    public ModuleDescriptor descriptor() {
        return new ModuleDescriptor(
            "department",                              // key
            "Department Management",                   // name
            "1.0.0",                                   // version
            List.of("permission", "audit-store"),      // dependencies
            List.of("department-management"),           // capabilities
            "Quản lý phòng ban: CRUD, cây phân cấp, gán nhân sự"
        );
    }

    // 2. Đăng ký navigation menu
    @Override
    public List<NavigationItemDescriptor> navigationItems() {
        return List.of(
            // Group: nhóm menu cha (không có route)
            new NavigationItemDescriptor(
                "module.department.hr-group",           // key
                "system-administration",                // workspaceKey
                "",                                     // parentKey (top-level)
                "Nhân sự & Tổ chức",                    // label
                "navigation.hrOrganization",            // labelKey (i18n)
                "users",                                // icon
                "",                                     // viewKey (rỗng = GROUP)
                "",                                     // route (rỗng = GROUP)
                30,                                     // sortOrder
                "",                                     // requiredAuthority
                "",                                     // permissionResource
                "",                                     // permissionAction
                List.of("phòng ban", "nhân sự", "tổ chức")
            ),
            // Page: trang quản lý phòng ban
            new NavigationItemDescriptor(
                "module.department.departments",        // key
                "system-administration",                // workspaceKey
                "module.department.hr-group",           // parentKey → con của group trên
                "Quản lý phòng ban",                    // label
                "navigation.departments",               // labelKey
                "building",                             // icon
                "departments",                          // viewKey
                "/administration/departments",          // route
                31,                                     // sortOrder
                "",                                     // requiredAuthority
                "DEPARTMENT",                           // permissionResource
                "READ",                                 // permissionAction
                List.of("department", "phòng ban", "quản lý")
            )
        );
    }
}
