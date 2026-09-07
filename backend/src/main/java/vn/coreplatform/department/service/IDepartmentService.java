package vn.coreplatform.department.service;

import java.util.List;

import vn.coreplatform.department.request.DepartmentCreateRequest;
import vn.coreplatform.department.response.DepartmentResponse;
import vn.coreplatform.department.response.DepartmentTreeResponse;
import vn.coreplatform.department.response.MemberResponse;
import vn.coreplatform.department.request.MemberAddRequest;

public interface IDepartmentService {
    DepartmentResponse createDepartment(DepartmentCreateRequest request);
    DepartmentResponse getDepartment(String code);
    DepartmentResponse changeOrDeleteManager(Long departmentId);
    DepartmentTreeResponse getDepartmentTree(Long orgId);
    List<DepartmentResponse> getAllDepartment();
    void deleteDepartment (Long id, Long orgid);

    MemberResponse addMemberToDepartment (MemberAddRequest request, String departmentCode);
    MemberResponse getMember (Long accountId, Long departmentId);
    MemberResponse updateMemberRole (Long accountId, Long departmentId);
    List<MemberResponse> getAllMemberInDeapartment(Long departmentId);
    void removeMember (Long accountId, Long departmentId);
}
