package vn.coreplatform.department.service.member;

import java.util.List;
import java.util.UUID;

import vn.coreplatform.department.enums.DepartmentMemberRole;
import vn.coreplatform.department.request.MemberAddRequest;
import vn.coreplatform.department.response.MemberResponse;

public interface IMemberService {
    MemberResponse addMemberToDepartment (MemberAddRequest request, UUID departmentId);
    MemberResponse getMember (UUID memberId, UUID departmentId);
    MemberResponse updateMemberRole (UUID memberId, UUID departmentId, DepartmentMemberRole role);
    List<MemberResponse> getAllMemberInDeapartment(UUID departmentId);
    void removeMember (UUID memberId, UUID departmentId);
}
