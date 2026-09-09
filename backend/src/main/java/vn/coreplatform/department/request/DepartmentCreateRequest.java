package vn.coreplatform.department.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DepartmentCreateRequest(
    @Pattern(regexp="[a-z][a-z0-9-]{1,79}") String code,
    @NotBlank @Size(max=160) String name,
    @Size(max=500) String description,
    @NotNull UUID orgId,
    UUID parentId,
    UUID managerId
) {
    
}
