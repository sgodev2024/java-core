package vn.coreplatform.identity;

import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class DemoAccountInitializer implements CommandLineRunner {
  private final JdbcTemplate jdbc; private final PasswordEncoder encoder;
  private final String bootstrapPassword;
  DemoAccountInitializer(JdbcTemplate jdbc, PasswordEncoder encoder, @Value("${core.bootstrap-admin-password:}") String bootstrapPassword){this.jdbc=jdbc;this.encoder=encoder;this.bootstrapPassword=bootstrapPassword;}
  @Override public void run(String... args){if(!bootstrapPassword.isBlank()) jdbc.update("update identity.account set password_hash=? where email='admin@core.local'",encoder.encode(bootstrapPassword));}
}
