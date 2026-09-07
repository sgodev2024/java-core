package vn.coreplatform.department.response;

import java.time.Instant;
import java.util.UUID;

import vn.coreplatform.department.repository.DepartmentMemberRole;

public record MemberResponse(
    UUID id,
    UUID accountId,
    String email,
    String displayName,
    DepartmentMemberRole roleInDept,
    Instant assignedAt,
    Instant removedAt
) {
    
}
