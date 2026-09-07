package vn.coreplatform.department.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentUpdateRequest(
    @NotBlank @Size(max=160) String name,
    @Size(max=500) String description,
    UUID parentId,
    UUID managerId,
    int sortOrder
) {
    
}
