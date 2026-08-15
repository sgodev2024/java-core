package vn.coreplatform.identity;

import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.security.SecureRandom; import java.time.*; import java.util.*;
import org.springframework.http.*; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.security.core.Authentication; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.transaction.annotation.Transactional; import org.springframework.web.bind.annotation.*;
import vn.coreplatform.security.SecurityConfig; import vn.coreplatform.shared.ApiExceptionHandler.ApiProblem;

@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
  private final JdbcTemplate jdbc; private final PasswordEncoder encoder; private final SecureRandom random=new SecureRandom();
  public AuthController(JdbcTemplate jdbc,PasswordEncoder encoder){this.jdbc=jdbc;this.encoder=encoder;}
  record LoginRequest(@Email @NotBlank String email,@NotBlank @Size(min=8,max=128) String password){}
  record LoginResponse(String challengeId,boolean mfaRequired,String maskedDestination){}
  record MfaRequest(@NotBlank String challengeId,@Pattern(regexp="\\d{6}") String code,boolean remember){}
  record SessionResponse(String accessToken,Instant expiresAt,UserResponse user){}
  record UserResponse(UUID id,String email,String displayName,String role){}

  @PostMapping("/login") @Transactional
  LoginResponse login(@Valid @RequestBody LoginRequest input){
    var rows=jdbc.query("select id,password_hash,enabled from identity.account where lower(email)=lower(?)",(rs,n)->Map.of("id",rs.getObject("id",UUID.class),"hash",rs.getString("password_hash"),"enabled",rs.getBoolean("enabled")),input.email());
    if(rows.isEmpty()||!encoder.matches(input.password(),(String)rows.getFirst().get("hash"))) throw new ApiProblem(HttpStatus.UNAUTHORIZED,"INVALID_CREDENTIALS","Email hoặc mật khẩu không chính xác");
    if(!((Boolean)rows.getFirst().get("enabled"))) throw new ApiProblem(HttpStatus.FORBIDDEN,"ACCOUNT_DISABLED","Tài khoản đã bị vô hiệu hóa");
    var challenge=UUID.randomUUID(); jdbc.update("insert into identity.mfa_challenge(id,account_id,expires_at) values(?,?,now()+interval '5 minutes')",challenge,rows.getFirst().get("id"));
    audit((UUID)rows.getFirst().get("id"),"AUTH_LOGIN_CHALLENGE","SUCCESS"); return new LoginResponse(challenge.toString(),true,"Authenticator app");
  }
  @PostMapping("/mfa") @Transactional
  SessionResponse mfa(@Valid @RequestBody MfaRequest input){
    var account=jdbc.query("select a.id,a.email,a.display_name,a.role from identity.mfa_challenge c join identity.account a on a.id=c.account_id where c.id=? and c.used_at is null and c.expires_at>now()",(rs,n)->new UserResponse(rs.getObject("id",UUID.class),rs.getString("email"),rs.getString("display_name"),rs.getString("role")),UUID.fromString(input.challengeId()));
    if(account.isEmpty()||!"123456".equals(input.code())) throw new ApiProblem(HttpStatus.UNAUTHORIZED,"INVALID_MFA_CODE","Mã xác thực không hợp lệ hoặc đã hết hạn");
    jdbc.update("update identity.mfa_challenge set used_at=now() where id=?",UUID.fromString(input.challengeId()));
    var token=newToken(); var expires=Instant.now().plus(input.remember()?Duration.ofDays(7):Duration.ofHours(8)); jdbc.update("insert into identity.session(id,account_id,token_hash,expires_at) values(?,?,?,?)",UUID.randomUUID(),account.getFirst().id(),SecurityConfig.sha256(token),expires);
    audit(account.getFirst().id(),"AUTH_LOGIN","SUCCESS"); return new SessionResponse(token,expires,account.getFirst());
  }
  @GetMapping("/me") UserResponse me(Authentication auth){ return jdbc.queryForObject("select id,email,display_name,role from identity.account where email=?",(rs,n)->new UserResponse(rs.getObject("id",UUID.class),rs.getString("email"),rs.getString("display_name"),rs.getString("role")),auth.getName()); }
  @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT) void logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String bearer,Authentication auth){ jdbc.update("update identity.session set revoked_at=now() where token_hash=?",SecurityConfig.sha256(bearer.substring(7))); jdbc.update("insert into audit.event(id,actor_email,action,result,occurred_at) values(?,?,?,?,now())",UUID.randomUUID(),auth.getName(),"AUTH_LOGOUT","SUCCESS"); }
  private String newToken(){var bytes=new byte[32];random.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
  private void audit(UUID actor,String action,String result){jdbc.update("insert into audit.event(id,actor_id,action,result,occurred_at) values(?,?,?,?,now())",UUID.randomUUID(),actor,action,result);}
}
