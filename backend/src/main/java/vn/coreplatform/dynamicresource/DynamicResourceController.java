package vn.coreplatform.dynamicresource;

import static vn.coreplatform.shared.ApiExceptionHandler.ApiProblem;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.apache.commons.csv.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.coreplatform.permission.PermissionService;

@RestController @RequestMapping("/api/v1/dynamic")
public class DynamicResourceController {
  private final JdbcTemplate jdbc;private final PermissionService permissions; public DynamicResourceController(JdbcTemplate jdbc,PermissionService permissions){this.jdbc=jdbc;this.permissions=permissions;}
  public record Definition(UUID id,String resourceKey,String name,int version,JsonNode schema,String status,Instant updatedAt){}
  public record RecordItem(UUID id,String resourceKey,JsonNode data,int version,String status,UUID ownerSubjectId,Instant createdAt,Instant updatedAt){}
  public record Revision(UUID id,int version,String operation,JsonNode data,UUID actorId,Instant occurredAt){}
  public record DefinitionCreate(@NotBlank @Pattern(regexp="[a-z][a-z0-9-]{2,99}") String resourceKey,@NotBlank @Size(max=160) String name,@NotNull JsonNode schema){}
  public record PageResult(List<RecordItem> items,int page,int size,long total){}
  public record ImportResult(int imported,int failed,List<String> errors){}

  @GetMapping("/definitions") List<Definition> definitions(Authentication auth){permissions.require(auth,"DYNAMIC_DEFINITION","READ",null);var t=tenant(auth);return jdbc.query("select * from dynamic_resource.definition where tenant_id=? order by name",(r,n)->definition(r),t);}
  @PostMapping("/definitions") @ResponseStatus(HttpStatus.CREATED) @Transactional Definition createDefinition(@Valid @RequestBody DefinitionCreate request,Authentication auth){
    permissions.require(auth,"DYNAMIC_DEFINITION","CREATE",null); validateSchema(request.schema()); var id=UUID.randomUUID(); var t=tenant(auth); var actor=account(auth);
    try{jdbc.update("insert into dynamic_resource.definition(id,tenant_id,resource_key,name,schema_json,created_by) values(?,?,?,?,?::jsonb,?)",id,t,request.resourceKey(),request.name().trim(),request.schema().toString(),actor);}catch(Exception e){throw new ApiProblem(HttpStatus.CONFLICT,"DEFINITION_EXISTS","Resource key đã tồn tại");}
    jdbc.update("insert into platform.resource_descriptor(name,storage_mode,owner_module,record_count,schema_version) values(?,'DYNAMIC','dynamic-resource',0,'v1')",request.name().trim()); audit(auth,"DYNAMIC_DEFINITION_CREATED","DYNAMIC_DEFINITION",id); return getDefinition(id,t);
  }
  @GetMapping("/{resourceKey}/records") PageResult records(@PathVariable String resourceKey,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size,@RequestParam(defaultValue="") String q,Authentication auth){
    var scope=permissions.scope(auth,"DYNAMIC_RECORD","READ");if(!scope.allowed())throw new ApiProblem(HttpStatus.FORBIDDEN,"PERMISSION_DENIED","Không có quyền đọc record");var t=tenant(auth); var d=definitionByKey(resourceKey,t); int safeSize=Math.max(1,Math.min(size,100)),safePage=Math.max(page,0); String search="%"+q.toLowerCase(Locale.ROOT)+"%";UUID owner=scope.ownerOnly()?account(auth):null;
    long total=jdbc.queryForObject("select count(*) from dynamic_resource.record where tenant_id=? and definition_id=? and status='ACTIVE' and (?::uuid is null or owner_subject_id=?) and (?='' or lower(data::text) like ?)",Long.class,t,d.id(),owner,owner,q,search);
    var items=jdbc.query("select r.*,? resource_key from dynamic_resource.record r where tenant_id=? and definition_id=? and status='ACTIVE' and (?::uuid is null or owner_subject_id=?) and (?='' or lower(data::text) like ?) order by updated_at desc,id limit ? offset ?",(r,n)->record(r),resourceKey,t,d.id(),owner,owner,q,search,safeSize,safePage*safeSize);
    return new PageResult(items,safePage,safeSize,total);
  }
  @PostMapping("/{resourceKey}/records") @ResponseStatus(HttpStatus.CREATED) @Transactional RecordItem create(@PathVariable String resourceKey,@RequestBody JsonNode data,Authentication auth){
    var actor=account(auth);permissions.require(auth,"DYNAMIC_RECORD","CREATE",actor);var t=tenant(auth);var d=definitionByKey(resourceKey,t);validateData(d.schema(),data);var id=UUID.randomUUID();
    jdbc.update("insert into dynamic_resource.record(id,tenant_id,definition_id,data,owner_subject_id,created_by) values(?,?,?,?::jsonb,?,?)",id,t,d.id(),data.toString(),actor,actor);
    jdbc.update("insert into dynamic_resource.revision(tenant_id,record_id,record_version,operation,data,actor_id) values(?,?,1,'CREATE',?::jsonb,?)",t,id,data.toString(),actor);
    jdbc.update("update platform.resource_descriptor set record_count=record_count+1,updated_at=now() where owner_module='dynamic-resource' and name=?",d.name()); audit(auth,"DYNAMIC_RECORD_CREATED",resourceKey,id); return getRecord(id,resourceKey,t);
  }
  @GetMapping("/{resourceKey}/records/{id}") RecordItem get(@PathVariable String resourceKey,@PathVariable UUID id,Authentication auth){var item=getRecord(id,resourceKey,tenant(auth));permissions.require(auth,"DYNAMIC_RECORD","READ",item.ownerSubjectId());return item;}
  @PutMapping("/{resourceKey}/records/{id}") @Transactional RecordItem update(@PathVariable String resourceKey,@PathVariable UUID id,@RequestBody JsonNode data,@RequestHeader("If-Match") int expectedVersion,Authentication auth){
    var current=getRecord(id,resourceKey,tenant(auth));permissions.require(auth,"DYNAMIC_RECORD","UPDATE",current.ownerSubjectId());var t=tenant(auth);var d=definitionByKey(resourceKey,t);validateData(d.schema(),data);int next=expectedVersion+1;
    int changed=jdbc.update("update dynamic_resource.record set data=?::jsonb,record_version=?,updated_at=now() where id=? and tenant_id=? and definition_id=? and record_version=? and status='ACTIVE'",data.toString(),next,id,t,d.id(),expectedVersion);
    if(changed==0)throw new ApiProblem(HttpStatus.CONFLICT,"VERSION_CONFLICT","Record đã thay đổi hoặc không tồn tại");
    jdbc.update("insert into dynamic_resource.revision(tenant_id,record_id,record_version,operation,data,actor_id) values(?,?,?,'UPDATE',?::jsonb,?)",t,id,next,data.toString(),account(auth));audit(auth,"DYNAMIC_RECORD_UPDATED",resourceKey,id);return getRecord(id,resourceKey,t);
  }
  @DeleteMapping("/{resourceKey}/records/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional void archive(@PathVariable String resourceKey,@PathVariable UUID id,Authentication auth){var current=getRecord(id,resourceKey,tenant(auth));permissions.require(auth,"DYNAMIC_RECORD","DELETE",current.ownerSubjectId());var t=tenant(auth);var d=definitionByKey(resourceKey,t);int c=jdbc.update("update dynamic_resource.record set status='ARCHIVED',record_version=record_version+1,updated_at=now() where id=? and tenant_id=? and definition_id=? and status='ACTIVE'",id,t,d.id());if(c==0)throw new ApiProblem(HttpStatus.NOT_FOUND,"RECORD_NOT_FOUND","Record không tồn tại");audit(auth,"DYNAMIC_RECORD_ARCHIVED",resourceKey,id);}
  @GetMapping("/{resourceKey}/records/{id}/history") List<Revision> history(@PathVariable String resourceKey,@PathVariable UUID id,Authentication auth){var current=getRecord(id,resourceKey,tenant(auth));permissions.require(auth,"DYNAMIC_RECORD","READ",current.ownerSubjectId());var t=tenant(auth);return jdbc.query("select * from dynamic_resource.revision where tenant_id=? and record_id=? order by record_version desc",(r,n)->new Revision(r.getObject("id",UUID.class),r.getInt("record_version"),r.getString("operation"),readJson(r.getString("data")),r.getObject("actor_id",UUID.class),r.getTimestamp("occurred_at").toInstant()),t,id);}

  @GetMapping(value="/{resourceKey}/export.csv",produces="text/csv") ResponseEntity<byte[]> exportCsv(@PathVariable String resourceKey,Authentication auth){
    var scope=permissions.scope(auth,"DYNAMIC_RECORD","READ");if(!scope.allowed())throw new ApiProblem(HttpStatus.FORBIDDEN,"PERMISSION_DENIED","Không có quyền export");var t=tenant(auth);var d=definitionByKey(resourceKey,t);var owner=scope.ownerOnly()?account(auth):null;
    var rows=jdbc.query("select data from dynamic_resource.record where tenant_id=? and definition_id=? and status='ACTIVE' and (?::uuid is null or owner_subject_id=?) order by created_at,id",(r,n)->readJson(r.getString(1)),t,d.id(),owner,owner);
    try(var out=new ByteArrayOutputStream();var writer=new OutputStreamWriter(out,StandardCharsets.UTF_8);var csv=new CSVPrinter(writer,CSVFormat.DEFAULT.builder().setHeader(fieldKeys(d.schema()).toArray(String[]::new)).build())){for(var row:rows){var values=new ArrayList<String>();for(var key:fieldKeys(d.schema()))values.add(row.path(key).isMissingNode()?"":row.path(key).asText());csv.printRecord(values);}csv.flush();audit(auth,"DYNAMIC_CSV_EXPORTED",resourceKey,d.id());return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+resourceKey+".csv\"").body(out.toByteArray());}catch(IOException e){throw new ApiProblem(HttpStatus.INTERNAL_SERVER_ERROR,"CSV_EXPORT_FAILED","Không thể tạo CSV");}
  }
  @PostMapping(value="/{resourceKey}/import.csv",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @Transactional ImportResult importCsv(@PathVariable String resourceKey,@RequestPart("file") MultipartFile file,Authentication auth){
    if(file.isEmpty()||file.getSize()>10_000_000)throw new ApiProblem(HttpStatus.BAD_REQUEST,"INVALID_FILE","CSV rỗng hoặc vượt 10 MB");var t=tenant(auth);var d=definitionByKey(resourceKey,t);permissions.require(auth,"DYNAMIC_RECORD","CREATE",account(auth));int imported=0,failed=0,rowNo=1;var errors=new ArrayList<String>();
    try(var reader=new InputStreamReader(file.getInputStream(),StandardCharsets.UTF_8);var csv=CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get().parse(reader)){for(var row:csv){rowNo++;if(rowNo>10001)throw new ApiProblem(HttpStatus.BAD_REQUEST,"ROW_LIMIT","Tối đa 10.000 dòng");try{var data=new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();for(var field:d.schema().path("fields")){var key=field.path("key").asText();var raw=row.isMapped(key)?row.get(key):"";if(raw.isBlank())continue;switch(field.path("type").asText()){case "number"->data.put(key,new java.math.BigDecimal(raw));case "boolean"->data.put(key,Boolean.parseBoolean(raw));default->data.put(key,raw);}}create(resourceKey,data,auth);imported++;}catch(Exception e){failed++;if(errors.size()<100)errors.add("Dòng "+rowNo+": "+e.getMessage());}}}catch(IOException|IllegalArgumentException e){throw new ApiProblem(HttpStatus.BAD_REQUEST,"INVALID_CSV","CSV không hợp lệ: "+e.getMessage());}audit(auth,"DYNAMIC_CSV_IMPORTED",resourceKey,d.id());return new ImportResult(imported,failed,errors);
  }

  private void validateSchema(JsonNode schema){if(!schema.isObject()||!schema.path("fields").isArray())throw new ApiProblem(HttpStatus.BAD_REQUEST,"INVALID_SCHEMA","Schema phải có mảng fields");var keys=new HashSet<String>();for(var f:schema.path("fields")){var k=f.path("key").asText();var type=f.path("type").asText();if(!k.matches("[a-z][a-zA-Z0-9_]{1,79}")||!Set.of("string","number","boolean","date","object").contains(type)||!keys.add(k))throw new ApiProblem(HttpStatus.BAD_REQUEST,"INVALID_FIELD","Field key/type không hợp lệ hoặc trùng");}}
  private void validateData(JsonNode schema,JsonNode data){if(!data.isObject())throw new ApiProblem(HttpStatus.BAD_REQUEST,"INVALID_DATA","Data phải là JSON object");for(var f:schema.path("fields")){var k=f.path("key").asText();var v=data.get(k);if(f.path("required").asBoolean(false)&&(v==null||v.isNull()||v.asText().isBlank()))throw new ApiProblem(HttpStatus.BAD_REQUEST,"REQUIRED_FIELD","Thiếu field bắt buộc: "+k);if(v!=null&&!v.isNull()){var type=f.path("type").asText();boolean ok=switch(type){case "string","date"->v.isTextual();case "number"->v.isNumber();case "boolean"->v.isBoolean();case "object"->v.isObject();default->false;};if(!ok)throw new ApiProblem(HttpStatus.BAD_REQUEST,"FIELD_TYPE_MISMATCH","Sai kiểu dữ liệu: "+k);}}}
  private List<String> fieldKeys(JsonNode schema){var keys=new ArrayList<String>();schema.path("fields").forEach(f->keys.add(f.path("key").asText()));return keys;}
  private Definition definitionByKey(String key,UUID t){var x=jdbc.query("select * from dynamic_resource.definition where tenant_id=? and resource_key=? and status='ACTIVE'",(r,n)->definition(r),t,key);if(x.isEmpty())throw new ApiProblem(HttpStatus.NOT_FOUND,"DEFINITION_NOT_FOUND","Dynamic Resource không tồn tại");return x.get(0);}
  private Definition getDefinition(UUID id,UUID t){return jdbc.queryForObject("select * from dynamic_resource.definition where id=? and tenant_id=?",(r,n)->definition(r),id,t);}
  private RecordItem getRecord(UUID id,String key,UUID t){var d=definitionByKey(key,t);var x=jdbc.query("select r.*,? resource_key from dynamic_resource.record r where id=? and tenant_id=? and definition_id=?",(r,n)->record(r),key,id,t,d.id());if(x.isEmpty())throw new ApiProblem(HttpStatus.NOT_FOUND,"RECORD_NOT_FOUND","Record không tồn tại");return x.get(0);}
  private Definition definition(java.sql.ResultSet r)throws java.sql.SQLException{return new Definition(r.getObject("id",UUID.class),r.getString("resource_key"),r.getString("name"),r.getInt("version"),readJson(r.getString("schema_json")),r.getString("status"),r.getTimestamp("updated_at").toInstant());}
  private RecordItem record(java.sql.ResultSet r)throws java.sql.SQLException{return new RecordItem(r.getObject("id",UUID.class),r.getString("resource_key"),readJson(r.getString("data")),r.getInt("record_version"),r.getString("status"),r.getObject("owner_subject_id",UUID.class),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant());}
  private JsonNode readJson(String value){try{return new com.fasterxml.jackson.databind.ObjectMapper().readTree(value);}catch(Exception e){throw new IllegalStateException(e);}}
  @SuppressWarnings("unchecked") private Map<String,Object> details(Authentication a){return (Map<String,Object>)a.getDetails();}
  private UUID tenant(Authentication a){if(a==null)throw new ApiProblem(HttpStatus.UNAUTHORIZED,"AUTH_REQUIRED","Yêu cầu đăng nhập");return (UUID)details(a).get("tenantId");}
  private UUID account(Authentication a){return (UUID)details(a).get("accountId");}
  private void requireAdmin(Authentication a){if(a==null||a.getAuthorities().stream().noneMatch(x->x.getAuthority().equals("ROLE_PLATFORM_ADMIN")))throw new ApiProblem(HttpStatus.FORBIDDEN,"PERMISSION_DENIED","Yêu cầu quyền Platform Administrator");}
  private void audit(Authentication a,String action,String type,UUID id){jdbc.update("insert into audit.event(id,actor_id,actor_email,tenant_key,action,resource_type,resource_id,result,occurred_at) values(?,?,?,?,?,?,?,?,now())",UUID.randomUUID(),account(a),a.getName(),tenant(a).toString(),action,type,id.toString(),"SUCCESS");}
}
