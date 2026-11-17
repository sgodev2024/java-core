package vn.coreplatform.whatsapp;

import java.util.List;
import org.springframework.stereotype.Component;
import vn.coreplatform.kernel.ModuleContributor;
import vn.coreplatform.kernel.ModuleDescriptor;
import vn.coreplatform.kernel.NavigationItemDescriptor;

@Component
public class WhatsAppModule implements ModuleContributor {
  @Override public ModuleDescriptor descriptor() {
    return new ModuleDescriptor("whatsapp", "WhatsApp Business", "0.1.0", List.of("permission", "audit-store"),
        List.of("whatsapp-inbox", "whatsapp-template-preview", "whatsapp-connection-settings"),
        "Màn hình quản lý WhatsApp Business; dữ liệu preview, chưa kết nối Meta token");
  }

  @Override public List<NavigationItemDescriptor> navigationItems() {
    return List.of(
        new NavigationItemDescriptor("module.whatsapp.group", "business", "", "Chăm sóc khách hàng", "navigation.customerCare",
            "users", "", "", 40, "", "", "", List.of("khách hàng", "tin nhắn", "whatsapp")),
        new NavigationItemDescriptor("module.whatsapp.workspace", "business", "module.whatsapp.group", "WhatsApp", "navigation.whatsapp",
            "whatsapp", "whatsapp", "/business/whatsapp", 41, "", "", "",
            List.of("whatsapp", "hội thoại", "mẫu tin", "meta", "inbox")));
  }
}
