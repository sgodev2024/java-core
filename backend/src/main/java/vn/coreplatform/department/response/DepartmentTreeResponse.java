package vn.coreplatform.department.response;

import java.util.List;
import java.util.UUID;

public record DepartmentTreeResponse(
    UUID id, 
    String code, 
    String name, 
    String status,
    UUID managerId, 
    String managerName, 
    int memberCount,
    List<DepartmentTreeResponse> children
) {

}
