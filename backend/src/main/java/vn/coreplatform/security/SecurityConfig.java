package vn.coreplatform.security;

import jakarta.servlet.*; import jakarta.servlet.http.*;
import java.io.IOException; import java.nio.charset.StandardCharsets; import java.security.MessageDigest; import java.util.*;
import org.springframework.context.annotation.*; import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity; import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority; import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain; import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component; import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {
  @Bean PasswordEncoder passwordEncoder(){ return new BCryptPasswordEncoder(12); }
  @Bean CorsConfigurationSource corsConfigurationSource(){ var c=new CorsConfiguration(); c.setAllowedOriginPatterns(List.of("http://localhost:*","https://*.chatgpt.site")); c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS")); c.setAllowedHeaders(List.of("Authorization","Content-Type","X-Correlation-Id")); c.setAllowCredentials(false); var s=new UrlBasedCorsConfigurationSource(); s.registerCorsConfiguration("/**",c); return s; }
  @Bean SecurityFilterChain security(HttpSecurity http, TokenFilter tokenFilter) throws Exception { return http.csrf(x->x.disable()).cors(x->{}).sessionManagement(x->x.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(x->x.requestMatchers("/api/v1/auth/login","/api/v1/auth/mfa","/actuator/health/**","/v3/api-docs/**","/swagger-ui/**").permitAll().anyRequest().authenticated()).addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class).build(); }

  @Component static class TokenFilter extends OncePerRequestFilter {
    private final JdbcTemplate jdbc; TokenFilter(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
      var header=req.getHeader(HttpHeaders.AUTHORIZATION);
      if(header!=null&&header.startsWith("Bearer ")){
        var hash=sha256(header.substring(7));
        jdbc.query("select a.id,a.email,a.display_name,a.role from identity.session s join identity.account a on a.id=s.account_id where s.token_hash=? and s.revoked_at is null and s.expires_at>now()", rs->{ if(rs.next()){ var auth=new UsernamePasswordAuthenticationToken(rs.getString("email"),null,List.of(new SimpleGrantedAuthority("ROLE_"+rs.getString("role")))); auth.setDetails(Map.of("accountId",rs.getObject("id"),"displayName",rs.getString("display_name"))); SecurityContextHolder.getContext().setAuthentication(auth);} return null;},hash);
      } chain.doFilter(req,res);
    }
  }
  public static String sha256(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
