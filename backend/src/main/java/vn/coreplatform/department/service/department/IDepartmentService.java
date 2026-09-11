package vn.coreplatform.department.service.department;

import java.util.List;
import java.util.UUID;

import vn.coreplatform.department.request.DepartmentCreateRequest;
import vn.coreplatform.department.request.DepartmentUpdateRequest;
import vn.coreplatform.department.response.DepartmentResponse;
import vn.coreplatform.department.response.DepartmentTreeResponse;

public interface IDepartmentService {
    DepartmentResponse createDepartment(DepartmentCreateRequest request);
    DepartmentResponse getDepartmentByCode(String code);
    DepartmentResponse changeOrDeleteManager(Long departmentId);
    DepartmentResponse updateDepartment(UUID departmentId, DepartmentUpdateRequest request);
    List<DepartmentTreeResponse> getDepartmentTree(UUID orgId);
    List<DepartmentResponse> getAllDepartment();
    void deleteDepartment (UUID departmentId);

}
