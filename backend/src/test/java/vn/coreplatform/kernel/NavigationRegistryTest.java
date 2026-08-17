package vn.coreplatform.kernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class NavigationRegistryTest {
  @Test void validatesAndOrdersWorkspaceManifest() {
    var validated = NavigationRegistry.validate(List.of(contributor("kernel", List.of(
        workspace("business"), workspace("core-admin")), List.of(
        page("core.home", "business", "", "home", 10),
        group("core.runtime", "core-admin", 20),
        page("core.modules", "core-admin", "core.runtime", "modules", 21)))));
    assertThat(validated.workspaces()).extracting(x -> x.descriptor().key()).containsExactly("business", "core-admin");
    assertThat(validated.items()).hasSize(3);
  }

  @Test void rejectsDuplicateAndForeignModuleNamespace() {
    var duplicate = List.of(
        contributor("kernel", List.of(workspace("business")), List.of(page("core.home", "business", "", "home", 10))),
        contributor("other", List.of(), List.of(page("core.home", "business", "", "home", 20))));
    assertThatThrownBy(() -> NavigationRegistry.validate(duplicate)).hasMessageContaining("Duplicate navigation item");

    var foreign = List.of(contributor("sales", List.of(workspace("business")),
        List.of(page("module.inventory.items", "business", "", "items", 10))));
    assertThatThrownBy(() -> NavigationRegistry.validate(foreign)).hasMessageContaining("namespace không thuộc module");
  }

  @Test void rejectsMissingParentAndUnsafeRoute() {
    var missing = List.of(contributor("kernel", List.of(workspace("business")),
        List.of(page("core.home", "business", "core.missing", "home", 10))));
    assertThatThrownBy(() -> NavigationRegistry.validate(missing)).hasMessageContaining("parent không tồn tại");

    var unsafe = new NavigationItemDescriptor("core.home", "business", "", "Home", "nav.home", "⌂",
        "home", "https://outside.example", 10, "", "", "", List.of());
    assertThatThrownBy(() -> NavigationRegistry.validate(List.of(contributor("kernel", List.of(workspace("business")), List.of(unsafe)))))
        .hasMessageContaining("hash route nội bộ");
  }

  private static ModuleContributor contributor(String key,List<NavigationWorkspaceDescriptor> workspaces,List<NavigationItemDescriptor> items) {
    return new ModuleContributor() {
      public ModuleDescriptor descriptor(){return new ModuleDescriptor(key,"Test","1.0.0",List.of(),List.of(),"");}
      public List<NavigationWorkspaceDescriptor> navigationWorkspaces(){return workspaces;}
      public List<NavigationItemDescriptor> navigationItems(){return items;}
    };
  }
  private static NavigationWorkspaceDescriptor workspace(String key) {
    return new NavigationWorkspaceDescriptor(key,key,"workspace."+key,"W",key.equals("core-admin")?"ADMIN":"BUSINESS",key.equals("business")?10:20,"");
  }
  private static NavigationItemDescriptor page(String key,String workspace,String parent,String view,int order) {
    return new NavigationItemDescriptor(key,workspace,parent,key,"nav."+view,"P",view,"/#/"+workspace+"/"+view,order,"","","",List.of());
  }
  private static NavigationItemDescriptor group(String key,String workspace,int order) {
    return new NavigationItemDescriptor(key,workspace,"",key,"nav.group","G","","",order,"","","",List.of());
  }
}
