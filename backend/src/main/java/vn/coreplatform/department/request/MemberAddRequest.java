package vn.coreplatform.department.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import vn.coreplatform.department.repository.DepartmentMemberRole;

public record MemberAddRequest(
    @NotNull UUID accountId,
    @NotNull DepartmentMemberRole role
) {
    
}
