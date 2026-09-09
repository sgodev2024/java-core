package vn.coreplatform.department.service.member;

import java.util.List;

import vn.coreplatform.department.request.MemberAddRequest;
import vn.coreplatform.department.response.MemberResponse;

public interface IMemberService {
    MemberResponse addMemberToDepartment (MemberAddRequest request, String departmentCode);
    MemberResponse getMember (Long accountId, Long departmentId);
    MemberResponse updateMemberRole (Long accountId, Long departmentId);
    List<MemberResponse> getAllMemberInDeapartment(Long departmentId);
    void removeMember (Long accountId, Long departmentId);
}
