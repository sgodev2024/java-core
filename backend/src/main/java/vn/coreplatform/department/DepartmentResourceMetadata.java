package vn.coreplatform.department;

import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.coreplatform.kernel.ResourceDescriptor;
import vn.coreplatform.kernel.ResourceRegistry;

@Component
@Order(100)
public class DepartmentResourceMetadata implements ApplicationRunner {
    private final ResourceRegistry resources;
    private final JdbcTemplate jdbc;

    public DepartmentResourceMetadata(ResourceRegistry resources, JdbcTemplate jdbc) {
        this.resources = resources;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Đăng ký resource: Department
        resources.register(new ResourceDescriptor(
            "DEPARTMENT",                        // resourceType
            "Phòng ban",                         // name
            "department",                        // ownerModule
            "DOMAIN",                            // storageMode
            "v1",                                // schemaVersion
            List.of("READ", "CREATE", "UPDATE", "DELETE"), // supportedActions
            "ALWAYS",                            // auditPolicy
            "INTERNAL"                           // dataClassification
        ));

        // Đăng ký resource: Department Member
        resources.register(new ResourceDescriptor(
            "DEPARTMENT-MEMBER",                 // resourceType
            "Thành viên phòng ban",              // name
            "department",                        // ownerModule
            "DOMAIN",                            // storageMode
            "v1",                                // schemaVersion
            List.of("READ", "CREATE", "DELETE"), // supportedActions
            "ALWAYS",                            // auditPolicy
            "INTERNAL"                           // dataClassification
        ));

        // Đồng bộ số lượng record (tùy chọn nhưng nên có để phục vụ telemetry)
        try {
            var count = jdbc.queryForObject("select count(*) from identity.department", Long.class);
            resources.synchronizeRecordCount("department", count == null ? 0 : count);
        } catch (Exception e) {
            // Đang dev nên không test được
        }
    }
}
