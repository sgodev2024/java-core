package vn.coreplatform.kernel;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class KernelModule implements ModuleContributor {
  @Override public ModuleDescriptor descriptor() {
    return new ModuleDescriptor("kernel", "Platform Kernel", "1.0.0", List.of(),
        List.of("module-registry", "migration-coordination", "tenant-context"), "Module runtime, migration coordination và tenant context");
  }

  @Override public List<NavigationWorkspaceDescriptor> navigationWorkspaces() {
    return List.of(
        new NavigationWorkspaceDescriptor("business", "Nghiệp vụ", "workspace.business", "▦", "BUSINESS", 10, ""),
        new NavigationWorkspaceDescriptor("core-admin", "Quản trị Core", "workspace.coreAdmin", "⚙", "ADMIN", 90, "ROLE_PLATFORM_ADMIN"));
  }

  @Override public List<NavigationItemDescriptor> navigationItems() {
    return List.of(
        new NavigationItemDescriptor("core.business-home", "business", "", "Trang chủ nghiệp vụ", "navigation.businessHome", "⌂", "business-home", "/#/business/home", 10, "", "", "", List.of("trang chủ", "nghiệp vụ", "workspace")),
        new NavigationItemDescriptor("core.admin-overview", "core-admin", "", "Tổng quan", "navigation.adminOverview", "⌂", "overview", "/#/core-admin/overview", 10, "ROLE_PLATFORM_ADMIN", "", "", List.of("dashboard", "health", "core")),
        new NavigationItemDescriptor("core.runtime", "core-admin", "", "Runtime", "navigation.runtime", "◫", "", "", 20, "ROLE_PLATFORM_ADMIN", "", "", List.of("runtime", "module")),
        new NavigationItemDescriptor("core.modules", "core-admin", "core.runtime", "Modules", "navigation.modules", "◫", "modules", "/#/core-admin/modules", 21, "ROLE_PLATFORM_ADMIN", "", "", List.of("module", "compatibility", "version")));
  }
}
