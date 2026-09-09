package vn.coreplatform.department.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateDepartmentRequest(
    String code,
    String name,
    @Size(max = 500)String description,
    @NotNull UUID orgId,
    UUID parentId   
) {
    
}
