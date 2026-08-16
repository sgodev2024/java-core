package vn.coreplatform.kernel;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class KernelModule implements ModuleContributor {
  @Override public ModuleDescriptor descriptor() {
    return new ModuleDescriptor("kernel", "Platform Kernel", "1.0.0", List.of(),
        List.of("module-registry", "migration-coordination", "tenant-context"), "Module runtime, migration coordination và tenant context");
  }
}
