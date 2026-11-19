package vn.coreplatform.whatsapp;

import java.util.List;
import org.springframework.stereotype.Component;
import vn.coreplatform.kernel.ModuleContributor;
import vn.coreplatform.kernel.ModuleDescriptor;
import vn.coreplatform.kernel.NavigationItemDescriptor;

@Component
public class WhatsAppModule implements ModuleContributor {
  @Override public ModuleDescriptor descriptor() {
    return new ModuleDescriptor("whatsapp", "Facebook Messenger", "0.2.0", List.of("permission", "audit-store"),
        List.of("facebook-messenger-inbox", "facebook-webhook", "facebook-send-api"),
        "Quản lý hội thoại Facebook Messenger qua Fanpage, webhook và Meta Graph API");
  }

  @Override public List<NavigationItemDescriptor> navigationItems() {
    return List.of(
        new NavigationItemDescriptor("module.whatsapp.group", "business", "", "Chăm sóc khách hàng", "navigation.customerCare",
            "users", "", "", 40, "", "", "", List.of("khách hàng", "tin nhắn", "whatsapp")),
        new NavigationItemDescriptor("module.whatsapp.workspace", "business", "module.whatsapp.group", "Facebook Messenger", "navigation.whatsapp",
            "message-circle", "whatsapp", "/business/whatsapp", 41, "", "", "",
            List.of("facebook", "messenger", "hội thoại", "fanpage", "meta", "inbox")),
        new NavigationItemDescriptor("module.whatsapp.customers", "business", "module.whatsapp.group", "Khách hàng Messenger", "navigation.messengerCustomers",
            "phone", "messenger-customers", "/business/messenger-customers", 42, "", "", "",
            List.of("khách hàng", "số điện thoại", "phone", "psid", "facebook", "messenger")));
  }
}
