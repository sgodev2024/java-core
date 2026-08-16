package vn.coreplatform.filemanagement;

import static vn.coreplatform.shared.ApiExceptionHandler.ApiProblem;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.coreplatform.permission.PermissionService;

@RestController @RequestMapping("/api/v1/files")
public class FileController {
  private final JdbcTemplate jdbc;private final PermissionService permissions;private final vn.coreplatform.audit.AuditService audits;private final Path root;
  public FileController(JdbcTemplate jdbc,PermissionService permissions,vn.coreplatform.audit.AuditService audits,@Value("${core.file-storage-root:/data/files}") String root)throws IOException{this.jdbc=jdbc;this.permissions=permissions;this.audits=audits;this.root=Path.of(root).toAbsolutePath().normalize();Files.createDirectories(this.root);}
  public record FileItem(UUID id,String name,String mediaType,long sizeBytes,String classification,String status,String checksumSha256,Instant createdAt,Instant updatedAt){}
  public record PageResult(List<FileItem> items,int page,int size,long total){}

  @GetMapping PageResult list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size,@RequestParam(defaultValue="") String q,Authentication a){var scope=permissions.scope(a,"FILE","READ");if(!scope.allowed())throw new ApiProblem(HttpStatus.FORBIDDEN,"PERMISSION_DENIED","Không có quyền đọc file");var t=permissions.tenant(a);var owner=scope.ownerOnly()?permissions.account(a):null;int s=Math.max(1,Math.min(size,100)),p=Math.max(0,page);String search="%"+q.toLowerCase(Locale.ROOT)+"%";long total=jdbc.queryForObject("select count(*) from files.file_object where tenant_id=? and status<>'DELETED' and (?::uuid is null or owner_subject_id=?) and (?='' or lower(name) like ?)",Long.class,t,owner,owner,q,search);var items=jdbc.query("select * from files.file_object where tenant_id=? and status<>'DELETED' and (?::uuid is null or owner_subject_id=?) and (?='' or lower(name) like ?) order by updated_at desc,id limit ? offset ?",(r,n)->item(r),t,owner,owner,q,search,s,p*s);return new PageResult(items,p,s,total);}
  @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED) @Transactional FileItem upload(@RequestPart("file") MultipartFile file,@RequestParam(defaultValue="INTERNAL") String classification,Authentication a){
    if(file.isEmpty()||file.getSize()>25L*1024*1024)throw new ApiProblem(HttpStatus.BAD_REQUEST,"FILE_SIZE","File rỗng hoặc vượt 25 MB");if(!classification.matches("INTERNAL|CONFIDENTIAL|RESTRICTED"))throw new ApiProblem(HttpStatus.BAD_REQUEST,"CLASSIFICATION","Phân loại không hợp lệ");var actor=permissions.account(a);permissions.require(a,"FILE","CREATE",actor);var id=UUID.randomUUID();var key=permissions.tenant(a)+"/"+id;var target=resolve(key);try{Files.createDirectories(target.getParent());var digest=MessageDigest.getInstance("SHA-256");try(var in=file.getInputStream();var out=Files.newOutputStream(target,StandardOpenOption.CREATE_NEW)){var buffer=new byte[64*1024];int read;while((read=in.read(buffer))!=-1){digest.update(buffer,0,read);out.write(buffer,0,read);}}var checksum=HexFormat.of().formatHex(digest.digest());jdbc.update("insert into files.file_object(id,tenant_id,name,media_type,size_bytes,classification,status,checksum_sha256,storage_key,owner_subject_id,created_by) values(?,?,?,?,?,?,'ACTIVE',?,?,?,?)",id,permissions.tenant(a),safeName(file.getOriginalFilename()),Optional.ofNullable(file.getContentType()).orElse("application/octet-stream"),file.getSize(),classification,checksum,key,actor,actor);audit(a,"FILE_UPLOADED",id);return getItem(id,a);}catch(Exception e){try{Files.deleteIfExists(target);}catch(IOException ignored){}if(e instanceof ApiProblem p)throw p;throw new ApiProblem(HttpStatus.INTERNAL_SERVER_ERROR,"FILE_WRITE_FAILED","Không thể lưu file");}
  }
  @GetMapping("/{id}/content") ResponseEntity<Resource> download(@PathVariable UUID id,Authentication a){var row=row(id,a);permissions.require(a,"FILE","READ",row.owner);if(row.key==null)throw new ApiProblem(HttpStatus.NOT_FOUND,"CONTENT_NOT_AVAILABLE","File demo chưa có nội dung vật lý");var path=resolve(row.key);if(!Files.isRegularFile(path))throw new ApiProblem(HttpStatus.NOT_FOUND,"CONTENT_MISSING","Không tìm thấy nội dung file");audit(a,"FILE_DOWNLOADED",id);return ResponseEntity.ok().contentType(MediaType.parseMediaType(row.mediaType)).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename*=UTF-8''"+java.net.URLEncoder.encode(row.name,java.nio.charset.StandardCharsets.UTF_8)).contentLength(row.size).body(new FileSystemResource(path));}
  @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional void delete(@PathVariable UUID id,Authentication a){var row=row(id,a);permissions.require(a,"FILE","DELETE",row.owner);jdbc.update("update files.file_object set status='DELETED',deleted_at=now(),updated_at=now() where id=? and tenant_id=?",id,permissions.tenant(a));if(row.key!=null)try{Files.deleteIfExists(resolve(row.key));}catch(IOException e){throw new ApiProblem(HttpStatus.INTERNAL_SERVER_ERROR,"FILE_DELETE_FAILED","Không thể xóa object");}audit(a,"FILE_DELETED",id);}
  private record Row(String key,String name,String mediaType,long size,UUID owner){}
  private Row row(UUID id,Authentication a){var x=jdbc.query("select storage_key,name,media_type,size_bytes,owner_subject_id from files.file_object where id=? and tenant_id=? and status<>'DELETED'",(r,n)->new Row(r.getString(1),r.getString(2),r.getString(3),r.getLong(4),r.getObject(5,UUID.class)),id,permissions.tenant(a));if(x.isEmpty())throw new ApiProblem(HttpStatus.NOT_FOUND,"FILE_NOT_FOUND","File không tồn tại");return x.get(0);}
  private FileItem getItem(UUID id,Authentication a){return jdbc.queryForObject("select * from files.file_object where id=? and tenant_id=?",(r,n)->item(r),id,permissions.tenant(a));}
  private FileItem item(java.sql.ResultSet r)throws java.sql.SQLException{return new FileItem(r.getObject("id",UUID.class),r.getString("name"),r.getString("media_type"),r.getLong("size_bytes"),r.getString("classification"),r.getString("status"),r.getString("checksum_sha256"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant());}
  private Path resolve(String key){var path=root.resolve(key).normalize();if(!path.startsWith(root))throw new ApiProblem(HttpStatus.BAD_REQUEST,"INVALID_STORAGE_KEY","Storage key không hợp lệ");return path;}
  private String safeName(String value){var name=Optional.ofNullable(value).orElse("file.bin").replace('\\','/');name=name.substring(name.lastIndexOf('/')+1).replaceAll("[\\r\\n\\u0000]","_");return name.length()>255?name.substring(name.length()-255):name;}
  private void audit(Authentication a,String action,UUID id){jdbc.update("insert into audit.event(id,actor_id,actor_email,tenant_key,action,resource_type,resource_id,result,correlation_id,occurred_at) values(?,?,?,?,?,'FILE',?,'SUCCESS',?,now())",UUID.randomUUID(),permissions.account(a),a.getName(),permissions.tenant(a).toString(),action,id.toString(),vn.coreplatform.shared.CorrelationIdFilter.current());}
}
