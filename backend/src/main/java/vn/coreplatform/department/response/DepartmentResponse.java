package vn.coreplatform.department.response;

import java.time.Instant;
import java.util.UUID;

public record DepartmentResponse(
    UUID id, 
    String code, 
    String name, 
    String description,
    UUID orgId, 
    String orgName, 
    UUID parentId, 
    String parentName,
    UUID managerId, 
    String managerName, 
    String status,
    int sortOrder, 
    int memberCount, 
    Instant createdAt, 
    Instant updatedAt
) {

}
