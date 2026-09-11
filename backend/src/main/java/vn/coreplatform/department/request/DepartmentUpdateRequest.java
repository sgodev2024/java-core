package vn.coreplatform.department.request;

import java.util.UUID;

import jakarta.validation.constraints.Size;

public record DepartmentUpdateRequest(

    @Size(max = 160)
    String name,

    @Size(max = 80)
    String code,

    @Size(max = 250)
    String description,

    UUID orgId,

    UUID parentId,

    UUID managerId,

    String status,

    Integer sortOrder
) {}
