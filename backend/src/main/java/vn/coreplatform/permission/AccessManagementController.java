package vn.coreplatform.permission;

import static vn.coreplatform.shared.ApiExceptionHandler.ApiProblem;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/access")
public class AccessManagementController {
  private final JdbcTemplate jdbc;private final PasswordEncoder passwords;private final PermissionService permissions;
  public AccessManagementController(JdbcTemplate jdbc,PasswordEncoder passwords,PermissionService permissions){this.jdbc=jdbc;this.passwords=passwords;this.permissions=permissions;}
  public record UserItem(UUID id,String email,String displayName,boolean enabled,List<String> roles,Instant createdAt){}
  public record RoleItem(UUID id,String code,String name,boolean systemRole){}
  public record PolicyItem(UUID id,String code,String resourceType,String action,String effect,String condition,int version,boolean enabled){}
  public record UserCreate(@Email @NotBlank String email,@NotBlank @Size(max=160) String displayName,@Size(min=12,max=128) String password,@NotEmpty List<UUID> roleIds){}
  public record RoleCreate(@Pattern(regexp="[a-z][a-z0-9-]{2,99}") String code,@NotBlank @Size(max=160) String name){}
  public record PolicyCreate(@Pattern(regexp="[a-z][a-z0-9-]{2,119}") String code,@NotBlank String resourceType,@NotBlank String action,@Pattern(regexp="ALLOW|DENY") String effect,String condition){}
  public record Assignment(@NotEmpty List<UUID> ids){}

  @GetMapping("/users") List<UserItem> users(Authentication a){manage(a);var t=permissions.tenant(a);return jdbc.query("select a.id,a.email,a.display_name,a.enabled,a.created_at,coalesce(string_agg(r.code,',' order by r.code),'') roles from identity.account a left join identity.account_role ar on ar.tenant_id=a.tenant_id and ar.account_id=a.id left join identity.role r on r.id=ar.role_id where a.tenant_id=? group by a.id order by a.email",(r,n)->new UserItem(r.getObject("id",UUID.class),r.getString("email"),r.getString("display_name"),r.getBoolean("enabled"),r.getString("roles").isBlank()?List.of():List.of(r.getString("roles").split(",")),r.getTimestamp("created_at").toInstant()),t);}
  @GetMapping("/roles") List<RoleItem> roles(Authentication a){manage(a);return jdbc.query("select id,code,name,system_role from identity.role where tenant_id=? order by name",(r,n)->new RoleItem(r.getObject("id",UUID.class),r.getString("code"),r.getString("name"),r.getBoolean("system_role")),permissions.tenant(a));}
  @GetMapping("/policies") List<PolicyItem> policies(Authentication a){manage(a);return jdbc.query("select * from identity.policy where tenant_id=? order by code,version desc",(r,n)->new PolicyItem(r.getObject("id",UUID.class),r.getString("code"),r.getString("resource_type"),r.getString("action"),r.getString("effect"),r.getString("condition_json"),r.getInt("version"),r.getBoolean("enabled")),permissions.tenant(a));}
  @PostMapping("/users") @ResponseStatus(HttpStatus.CREATED) @Transactional UserItem createUser(@Valid @RequestBody UserCreate x,Authentication a){manage(a);var t=permissions.tenant(a);var id=UUID.randomUUID();try{jdbc.update("insert into identity.account(id,tenant_id,email,display_name,password_hash,role) values(?,?,?,?,?,'APPLICATION_USER')",id,t,x.email().trim().toLowerCase(),x.displayName().trim(),passwords.encode(x.password()));for(var role:x.roleIds()){int c=jdbc.update("insert into identity.account_role(tenant_id,account_id,role_id) select ?,?,id from identity.role where id=? and tenant_id=?",t,id,role,t);if(c==0)throw new ApiProblem(HttpStatus.BAD_REQUEST,"INVALID_ROLE","Role không thuộc tenant");}}catch(ApiProblem e){throw e;}catch(Exception e){throw new ApiProblem(HttpStatus.CONFLICT,"USER_EXISTS","Email đã tồn tại");}revision(t);audit(a,"USER_CREATED",id);return users(a).stream().filter(u->u.id().equals(id)).findFirst().orElseThrow();}
  @PatchMapping("/users/{id}/enabled") @Transactional void enabled(@PathVariable UUID id,@RequestBody Map<String,Boolean> body,Authentication a){manage(a);if(id.equals(permissions.account(a))&&!body.getOrDefault("enabled",true))throw new ApiProblem(HttpStatus.CONFLICT,"SELF_DISABLE","Không thể tự vô hiệu hóa tài khoản");int c=jdbc.update("update identity.account set enabled=? where id=? and tenant_id=?",body.getOrDefault("enabled",true),id,permissions.tenant(a));if(c==0)throw new ApiProblem(HttpStatus.NOT_FOUND,"USER_NOT_FOUND","User không tồn tại");jdbc.update("update identity.session set revoked_at=now() where account_id=? and revoked_at is null",id);audit(a,"USER_STATUS_CHANGED",id);}
  @PostMapping("/roles") @ResponseStatus(HttpStatus.CREATED) @Transactional RoleItem createRole(@Valid @RequestBody RoleCreate x,Authentication a){manage(a);var id=UUID.randomUUID();var t=permissions.tenant(a);try{jdbc.update("insert into identity.role(id,tenant_id,code,name) values(?,?,?,?)",id,t,x.code(),x.name());}catch(Exception e){throw new ApiProblem(HttpStatus.CONFLICT,"ROLE_EXISTS","Role code đã tồn tại");}revision(t);audit(a,"ROLE_CREATED",id);return new RoleItem(id,x.code(),x.name(),false);}
  @PostMapping("/policies") @ResponseStatus(HttpStatus.CREATED) @Transactional PolicyItem createPolicy(@Valid @RequestBody PolicyCreate x,Authentication a){manage(a);var id=UUID.randomUUID();var t=permissions.tenant(a);String condition=Optional.ofNullable(x.condition()).filter(s->!s.isBlank()).orElse("{}");try{new com.fasterxml.jackson.databind.ObjectMapper().readTree(condition);jdbc.update("insert into identity.policy(id,tenant_id,code,resource_type,action,effect,condition_json) values(?,?,?,?,?,?,?::jsonb)",id,t,x.code(),x.resourceType(),x.action(),x.effect(),condition);}catch(Exception e){throw new ApiProblem(HttpStatus.BAD_REQUEST,"INVALID_POLICY","Policy trùng hoặc condition JSON không hợp lệ");}revision(t);audit(a,"POLICY_CREATED",id);return policies(a).stream().filter(p->p.id().equals(id)).findFirst().orElseThrow();}
  @PutMapping("/roles/{roleId}/policies") @Transactional void bind(@PathVariable UUID roleId,@Valid @RequestBody Assignment x,Authentication a){manage(a);var t=permissions.tenant(a);if(jdbc.queryForObject("select count(*) from identity.role where id=? and tenant_id=?",Integer.class,roleId,t)==0)throw new ApiProblem(HttpStatus.NOT_FOUND,"ROLE_NOT_FOUND","Role không tồn tại");jdbc.update("delete from identity.role_policy where tenant_id=? and role_id=?",t,roleId);for(var id:x.ids()){int c=jdbc.update("insert into identity.role_policy(tenant_id,role_id,policy_id) select ?,?,id from identity.policy where id=? and tenant_id=?",t,roleId,id,t);if(c==0)throw new ApiProblem(HttpStatus.BAD_REQUEST,"INVALID_POLICY","Policy không thuộc tenant");}revision(t);audit(a,"ROLE_POLICIES_CHANGED",roleId);}
  private void manage(Authentication a){permissions.require(a,"ACCESS_ADMIN","MANAGE",null);}
  private void revision(UUID t){jdbc.update("update identity.permission_revision set revision=revision+1,updated_at=now() where tenant_id=?",t);}
  private void audit(Authentication a,String action,UUID id){jdbc.update("insert into audit.event(id,actor_id,actor_email,tenant_key,action,resource_type,resource_id,result,occurred_at) values(?,?,?,?,?,'ACCESS',?,'SUCCESS',now())",UUID.randomUUID(),permissions.account(a),a.getName(),permissions.tenant(a).toString(),action,id.toString());}
}
