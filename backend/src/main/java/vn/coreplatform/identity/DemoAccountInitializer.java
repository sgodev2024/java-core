package vn.coreplatform.identity;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
class DemoAccountInitializer implements CommandLineRunner {
  private final JdbcTemplate jdbc; private final PasswordEncoder encoder;
  DemoAccountInitializer(JdbcTemplate jdbc, PasswordEncoder encoder){this.jdbc=jdbc;this.encoder=encoder;}
  @Override public void run(String... args){jdbc.update("update identity.account set password_hash=? where email='admin@core.local'",encoder.encode("Core@2026"));}
}
