package vn.coreplatform.controlplane;

import java.util.List;
import org.springframework.stereotype.Component;
import vn.coreplatform.kernel.ModuleContributor;
import vn.coreplatform.kernel.ModuleDescriptor;

@Component
public class ControlPlaneModule implements ModuleContributor {
  @Override public ModuleDescriptor descriptor() {
    return new ModuleDescriptor("control-plane", "Control Plane", "1.0.0",
        List.of("local-identity", "permission", "dynamic-resource", "file-management"),
        List.of("admin-console"), "API quản trị cho admin console");
  }
}
