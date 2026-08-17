package vn.coreplatform.controlplane;

import java.util.List;
import org.springframework.stereotype.Component;
import vn.coreplatform.kernel.ModuleContributor;
import vn.coreplatform.kernel.ModuleDescriptor;
import vn.coreplatform.kernel.NavigationItemDescriptor;

@Component
public class ControlPlaneModule implements ModuleContributor {
  @Override public ModuleDescriptor descriptor() {
    return new ModuleDescriptor("control-plane", "Control Plane", "1.0.0",
        List.of("local-identity", "permission", "dynamic-resource", "file-management"),
        List.of("admin-console"), "API quản trị cho admin console");
  }

  @Override public List<NavigationItemDescriptor> navigationItems() {
    return List.of(
        new NavigationItemDescriptor("core.resources", "core-admin", "core.runtime", "Resources", "navigation.resources", "◇", "resources", "/#/core-admin/resources", 22, "ROLE_PLATFORM_ADMIN", "", "", List.of("resource", "dynamic", "schema")),
        new NavigationItemDescriptor("core.security", "core-admin", "", "Bảo mật & truy cập", "navigation.security", "◎", "", "", 30, "ROLE_PLATFORM_ADMIN", "", "", List.of("security", "access")),
        new NavigationItemDescriptor("core.access", "core-admin", "core.security", "Người dùng & phân quyền", "navigation.access", "◎", "access", "/#/core-admin/access", 31, "ROLE_PLATFORM_ADMIN", "", "", List.of("user", "role", "policy", "permission")),
        new NavigationItemDescriptor("core.operations", "core-admin", "", "Vận hành", "navigation.operations", "↯", "", "", 40, "ROLE_PLATFORM_ADMIN", "", "", List.of("operations", "job", "event")),
        new NavigationItemDescriptor("core.activity", "core-admin", "core.operations", "Events & Jobs", "navigation.activity", "↯", "activity", "/#/core-admin/activity", 41, "ROLE_PLATFORM_ADMIN", "", "", List.of("event", "outbox", "job", "schedule")),
        new NavigationItemDescriptor("core.files", "core-admin", "core.operations", "Tệp tin", "navigation.files", "▱", "files", "/#/core-admin/files", 42, "ROLE_PLATFORM_ADMIN", "", "", List.of("file", "storage", "upload")),
        new NavigationItemDescriptor("core.settings", "core-admin", "", "Cấu hình", "navigation.settings", "⚙", "settings", "/#/core-admin/settings", 90, "ROLE_PLATFORM_ADMIN", "", "", List.of("settings", "environment", "deployment")));
  }
}
