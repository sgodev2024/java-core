package vn.coreplatform.permission;

import static vn.coreplatform.shared.ApiExceptionHandler.ApiProblem;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {
  private final JdbcTemplate jdbc; public PermissionService(JdbcTemplate jdbc){this.jdbc=jdbc;}
  public record Decision(boolean allowed,String reason,boolean ownerOnly){}

  public Decision decide(Authentication auth,String resource,String action,UUID ownerId){
    if(auth==null)return new Decision(false,"AUTH_REQUIRED",false);
    var tenant=tenant(auth);var account=account(auth);
    var policies=jdbc.query("select p.effect,p.resource_type,p.action,p.condition_json from identity.account_role ar join identity.role_policy rp on rp.tenant_id=ar.tenant_id and rp.role_id=ar.role_id join identity.policy p on p.tenant_id=rp.tenant_id and p.id=rp.policy_id where ar.tenant_id=? and ar.account_id=? and p.enabled=true and (p.resource_type='*' or p.resource_type=?) and (p.action='*' or p.action=?)",(r,n)->Map.of("effect",r.getString("effect"),"condition",r.getString("condition_json")),tenant,account,resource,action);
    boolean allow=false,ownerOnly=false;
    for(var p:policies){JsonNode condition=read((String)p.get("condition"));boolean owner=condition.path("ownerOnly").asBoolean(false);boolean matches=!owner||Objects.equals(ownerId,account);if(matches&&"DENY".equals(p.get("effect")))return new Decision(false,"EXPLICIT_DENY",owner);if(matches&&"ALLOW".equals(p.get("effect"))){allow=true;ownerOnly|=owner;}}
    return new Decision(allow,allow?"POLICY_ALLOW":"NO_MATCHING_POLICY",ownerOnly);
  }
  public void require(Authentication auth,String resource,String action,UUID ownerId){var d=decide(auth,resource,action,ownerId);if(!d.allowed())throw new ApiProblem(HttpStatus.FORBIDDEN,"PERMISSION_DENIED","Không có quyền "+action+" trên "+resource);}
  public Decision scope(Authentication auth,String resource,String action){
    if(auth==null)return new Decision(false,"AUTH_REQUIRED",false);var tenant=tenant(auth);var account=account(auth);
    var policies=jdbc.query("select p.effect,p.condition_json from identity.account_role ar join identity.role_policy rp on rp.tenant_id=ar.tenant_id and rp.role_id=ar.role_id join identity.policy p on p.tenant_id=rp.tenant_id and p.id=rp.policy_id where ar.tenant_id=? and ar.account_id=? and p.enabled=true and (p.resource_type='*' or p.resource_type=?) and (p.action='*' or p.action=?)",(r,n)->Map.of("effect",r.getString("effect"),"condition",r.getString("condition_json")),tenant,account,resource,action);
    boolean allow=false,ownerOnly=false;for(var p:policies){boolean owner=read((String)p.get("condition")).path("ownerOnly").asBoolean(false);if("DENY".equals(p.get("effect"))&&!owner)return new Decision(false,"EXPLICIT_DENY",false);if("ALLOW".equals(p.get("effect"))){allow=true;ownerOnly|=owner;}}
    return new Decision(allow,allow?"POLICY_ALLOW":"NO_MATCHING_POLICY",ownerOnly);
  }
  @SuppressWarnings("unchecked") public UUID tenant(Authentication a){return (UUID)((Map<String,Object>)a.getDetails()).get("tenantId");}
  @SuppressWarnings("unchecked") public UUID account(Authentication a){return (UUID)((Map<String,Object>)a.getDetails()).get("accountId");}
  private JsonNode read(String value){try{return new com.fasterxml.jackson.databind.ObjectMapper().readTree(value);}catch(Exception e){throw new IllegalStateException(e);}}
}
