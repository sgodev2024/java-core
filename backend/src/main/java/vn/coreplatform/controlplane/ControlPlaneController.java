package vn.coreplatform.controlplane;

import java.time.Instant; import java.util.*; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/control-plane")
public class ControlPlaneController {
  private final JdbcTemplate jdbc; public ControlPlaneController(JdbcTemplate jdbc){this.jdbc=jdbc;}
  record Summary(long resources,long modules,long pendingOutbox,long runningJobs,long files,double storageGb,String coreVersion,String environment){}
  record Module(UUID id,String name,String moduleKey,String version,String status,String description,String metric){}
  record Resource(UUID id,String name,String storageMode,String ownerModule,long records,String schemaVersion,Instant updatedAt){}
  record Activity(UUID id,String kind,String name,String metadata,String status,Instant occurredAt){}
  record Role(UUID id,String name,int users,int policies,String scope){}
  record FileItem(UUID id,String name,String mediaType,long sizeBytes,String classification,String status,Instant updatedAt){}
  record Bootstrap(Summary summary,List<Module> modules,List<Resource> resources,List<Activity> activities,List<Role> roles,List<FileItem> files){}

  @GetMapping("/bootstrap") Bootstrap bootstrap(){
    var summary=jdbc.queryForObject("select (select coalesce(sum(record_count),0) from platform.resource_descriptor) resources,(select count(*) from platform.module) modules,(select count(*) from async.outbox_event where status='PENDING') outbox,(select count(*) from async.job where status='RUNNING') jobs,(select count(*) from files.file_object) files,(select coalesce(sum(size_bytes),0)/1073741824.0 from files.file_object) storage_gb",(r,n)->new Summary(r.getLong("resources"),r.getLong("modules"),r.getLong("outbox"),r.getLong("jobs"),r.getLong("files"),r.getDouble("storage_gb"),"1.0.0-rc.4","core-production-vn"));
    var modules=jdbc.query("select * from platform.module order by sort_order",(r,n)->new Module(r.getObject("id",UUID.class),r.getString("name"),r.getString("module_key"),r.getString("version"),r.getString("status"),r.getString("description"),r.getString("metric")));
    var resources=jdbc.query("select * from platform.resource_descriptor order by updated_at desc",(r,n)->new Resource(r.getObject("id",UUID.class),r.getString("name"),r.getString("storage_mode"),r.getString("owner_module"),r.getLong("record_count"),r.getString("schema_version"),r.getTimestamp("updated_at").toInstant()));
    var activities=jdbc.query("select * from platform.activity order by occurred_at desc limit 20",(r,n)->new Activity(r.getObject("id",UUID.class),r.getString("kind"),r.getString("name"),r.getString("metadata"),r.getString("status"),r.getTimestamp("occurred_at").toInstant()));
    var roles=jdbc.query("select * from identity.role_summary order by name",(r,n)->new Role(r.getObject("id",UUID.class),r.getString("name"),r.getInt("user_count"),r.getInt("policy_count"),r.getString("scope")));
    var files=jdbc.query("select * from files.file_object order by updated_at desc",(r,n)->new FileItem(r.getObject("id",UUID.class),r.getString("name"),r.getString("media_type"),r.getLong("size_bytes"),r.getString("classification"),r.getString("status"),r.getTimestamp("updated_at").toInstant()));
    return new Bootstrap(summary,modules,resources,activities,roles,files);
  }
}
