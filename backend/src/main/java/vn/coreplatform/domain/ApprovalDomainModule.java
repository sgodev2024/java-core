package vn.coreplatform.domain;

import java.util.List;
import org.springframework.stereotype.Component;
import vn.coreplatform.kernel.ModuleContributor;
import vn.coreplatform.kernel.ModuleDescriptor;

@Component
public class ApprovalDomainModule implements ModuleContributor {
  @Override public ModuleDescriptor descriptor() {
    return new ModuleDescriptor("approval-domain", "Approval Domain", "1.0.0", List.of("permission", "event-outbox", "audit-store"),
        List.of("approval-aggregate", "sample-domain"), "Code-first typed aggregate (Plane 1) — không phụ thuộc Dynamic Resource");
  }
}
