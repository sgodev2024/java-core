package vn.coreplatform.navigation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.coreplatform.audit.AuditService;
import vn.coreplatform.kernel.NavigationRegistry;
import vn.coreplatform.permission.PermissionService;

/** Navigation API theo người dùng: module status + authority + PDP + preferences. */
@RestController
@RequestMapping("/api/v1/navigation")
public class NavigationController {
  private final NavigationRegistry registry;
  private final PermissionService permissions;
  private final JdbcTemplate jdbc;
  private final ObjectMapper json;
  private final AuditService audits;

  public NavigationController(NavigationRegistry registry, PermissionService permissions, JdbcTemplate jdbc, ObjectMapper json, AuditService audits) {
    this.registry = registry; this.permissions = permissions; this.jdbc = jdbc; this.json = json; this.audits = audits;
  }

  public record NavigationItemView(String key,String parentKey,String ownerModule,String label,String labelKey,String icon,
      String type,String viewKey,String route,int sortOrder,List<String> keywords) {}
  public record WorkspaceView(String key,String label,String labelKey,String icon,String category,int sortOrder,List<NavigationItemView> items) {}
  public record NavigationResponse(String revision,String defaultWorkspaceKey,String currentWorkspaceKey,List<WorkspaceView> workspaces,
      List<String> favoriteKeys,List<String> recentKeys) {}
  public record PreferencesUpdate(
      @Size(max=20) List<@Pattern(regexp="(?:core|module|customer)\\.[a-z0-9][a-z0-9.-]{1,159}") String> favoriteKeys,
      @Size(max=10) List<@Pattern(regexp="(?:core|module|customer)\\.[a-z0-9][a-z0-9.-]{1,159}") String> recentKeys,
      @Pattern(regexp="[a-z][a-z0-9-]{1,79}") String lastWorkspaceKey) {}
  private record Preference(List<String> favorites,List<String> recents,String workspace) {}
  private record Effective(List<WorkspaceView> workspaces,Set<String> itemKeys,Set<String> workspaceKeys) {}

  @GetMapping("/me")
  NavigationResponse mine(Authentication auth) { return response(auth, effective(auth), preference(auth)); }

  @PutMapping("/me/preferences")
  @Transactional
  NavigationResponse updatePreferences(@Valid @RequestBody PreferencesUpdate request, Authentication auth) {
    var effective = effective(auth);
    var previous = preference(auth);
    var favorites = request.favoriteKeys() == null ? previous.favorites() : allowedDistinct(request.favoriteKeys(), effective.itemKeys(), 20);
    var recents = request.recentKeys() == null ? previous.recents() : allowedDistinct(request.recentKeys(), effective.itemKeys(), 10);
    var workspace = request.lastWorkspaceKey() == null ? previous.workspace()
        : (effective.workspaceKeys().contains(request.lastWorkspaceKey()) ? request.lastWorkspaceKey() : "");
    jdbc.update("""
        insert into platform.navigation_preference(tenant_id,account_id,favorite_keys,recent_keys,last_workspace_key,updated_at)
        values (?,?,?::jsonb,?::jsonb,?,now())
        on conflict(tenant_id,account_id) do update set favorite_keys=excluded.favorite_keys,recent_keys=excluded.recent_keys,
          last_workspace_key=excluded.last_workspace_key,updated_at=now()
        """, permissions.tenant(auth), permissions.account(auth), write(favorites), write(recents), workspace);
    audits.record(permissions.tenantKey(auth), permissions.account(auth), auth.getName(), "NAVIGATION_PREFERENCES_UPDATED",
        "NAVIGATION", null, "SUCCESS", null);
    return response(auth, effective, new Preference(favorites, recents, workspace));
  }

  private NavigationResponse response(Authentication auth, Effective effective, Preference preference) {
    var workspace = effective.workspaceKeys().contains(preference.workspace()) ? preference.workspace()
        : effective.workspaces().stream().map(WorkspaceView::key).findFirst().orElse("");
    var favoriteKeys = allowedDistinct(preference.favorites(), effective.itemKeys(), 20);
    var recentKeys = allowedDistinct(preference.recents(), effective.itemKeys(), 10);
    return new NavigationResponse(registry.revision(), workspace, workspace, effective.workspaces(), favoriteKeys, recentKeys);
  }

  private Effective effective(Authentication auth) {
    var admin = hasAuthority(auth, "ROLE_PLATFORM_ADMIN");
    var moduleEnabled = new HashMap<String,Boolean>();
    jdbc.query("select module_key,status from platform.module", r -> {
      moduleEnabled.put(r.getString(1), !"DISABLED".equals(r.getString(2)));
    });

    var visibleWorkspaces = new LinkedHashMap<String,NavigationRegistry.WorkspaceRegistration>();
    for (var workspace : registry.workspaces()) {
      var descriptor = workspace.descriptor();
      if (descriptor.requiredAuthority().isBlank() || hasAuthority(auth, descriptor.requiredAuthority()))
        visibleWorkspaces.put(descriptor.key(), workspace);
    }

    var accessiblePages = new LinkedHashMap<String,NavigationRegistry.ItemRegistration>();
    var groups = new LinkedHashMap<String,NavigationRegistry.ItemRegistration>();
    for (var item : registry.items()) {
      var descriptor = item.descriptor();
      if (!visibleWorkspaces.containsKey(descriptor.workspaceKey())) continue;
      if (!"kernel".equals(item.ownerModule()) && !moduleEnabled.getOrDefault(item.ownerModule(), false)) continue;
      if (!descriptor.requiredAuthority().isBlank() && !hasAuthority(auth, descriptor.requiredAuthority())) continue;
      if (descriptor.group()) { groups.put(descriptor.key(), item); continue; }
      if (!descriptor.permissionResource().isBlank() && !admin
          && !permissions.scope(auth, descriptor.permissionResource(), descriptor.permissionAction()).allowed()) continue;
      accessiblePages.put(descriptor.key(), item);
    }

    // Chỉ giữ group có ít nhất một page hiển thị; hỗ trợ nhiều cấp parent.
    var visibleKeys = new LinkedHashSet<>(accessiblePages.keySet());
    boolean changed;
    do {
      changed = false;
      for (var pageOrGroup : registry.items()) {
        var parent = pageOrGroup.descriptor().parentKey();
        if (!parent.isBlank() && visibleKeys.contains(pageOrGroup.descriptor().key()) && groups.containsKey(parent))
          changed |= visibleKeys.add(parent);
      }
    } while (changed);

    var workspaceViews = new ArrayList<WorkspaceView>();
    for (var workspace : visibleWorkspaces.values()) {
      var itemViews = registry.items().stream()
          .filter(item -> item.descriptor().workspaceKey().equals(workspace.descriptor().key()))
          .filter(item -> visibleKeys.contains(item.descriptor().key()))
          .map(this::view).toList();
      if (!itemViews.isEmpty()) {
        var descriptor = workspace.descriptor();
        workspaceViews.add(new WorkspaceView(descriptor.key(), descriptor.label(), descriptor.labelKey(), descriptor.icon(),
            descriptor.category(), descriptor.sortOrder(), itemViews));
      }
    }
    var itemKeys = new LinkedHashSet<String>();
    workspaceViews.forEach(w -> w.items().stream().filter(i -> "PAGE".equals(i.type())).forEach(i -> itemKeys.add(i.key())));
    var workspaceKeys = new LinkedHashSet<String>();
    workspaceViews.forEach(w -> workspaceKeys.add(w.key()));
    return new Effective(List.copyOf(workspaceViews), Set.copyOf(itemKeys), Set.copyOf(workspaceKeys));
  }

  private NavigationItemView view(NavigationRegistry.ItemRegistration item) {
    var d = item.descriptor();
    return new NavigationItemView(d.key(), d.parentKey(), item.ownerModule(), d.label(), d.labelKey(), d.icon(),
        d.group() ? "GROUP" : "PAGE", d.viewKey(), d.route(), d.sortOrder(), d.keywords());
  }

  private Preference preference(Authentication auth) {
    var rows = jdbc.query("select favorite_keys::text,recent_keys::text,last_workspace_key from platform.navigation_preference where tenant_id=? and account_id=?",
        (r,n) -> new Preference(read(r.getString(1)), read(r.getString(2)), Objects.toString(r.getString(3), "")),
        permissions.tenant(auth), permissions.account(auth));
    return rows.isEmpty() ? new Preference(List.of(), List.of(), "") : rows.getFirst();
  }

  private List<String> allowedDistinct(List<String> input, Set<String> allowed, int max) {
    if (input == null) return List.of();
    return input.stream().filter(Objects::nonNull).map(String::trim).filter(allowed::contains).distinct().limit(max).toList();
  }
  private boolean hasAuthority(Authentication auth,String authority) {
    return auth != null && auth.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
  }
  private String write(List<String> value) { try { return json.writeValueAsString(value); } catch (Exception e) { throw new IllegalStateException(e); } }
  private List<String> read(String value) {
    try { return value == null ? List.of() : json.readValue(value, new TypeReference<List<String>>(){}); }
    catch (Exception e) { return List.of(); }
  }
}
