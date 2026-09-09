package vn.coreplatform.department.service.department;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.coreplatform.audit.AuditService;
import vn.coreplatform.department.repository.DepartmentMemberRepository;
import vn.coreplatform.department.repository.DepartmentRepository;
import vn.coreplatform.department.request.DepartmentCreateRequest;
import vn.coreplatform.department.request.MemberAddRequest;
import vn.coreplatform.department.response.DepartmentResponse;
import vn.coreplatform.department.response.DepartmentTreeResponse;
import vn.coreplatform.department.response.MemberResponse;
import vn.coreplatform.kernel.ResourceRegistry;
import vn.coreplatform.kernel.TenantContext;
import vn.coreplatform.permission.PermissionService;
import vn.coreplatform.shared.ApiExceptionHandler.ApiProblem;

@Service 
public class DepartmentService implements IDepartmentService{
    
    private  final JdbcTemplate jdbc;
    private  final AuditService auditService;
    private  final PermissionService permissionService;
    private  final DepartmentRepository departmentRepository;
    private  final DepartmentMemberRepository departmentMemberRepository;

    public  DepartmentService (JdbcTemplate jdbcTemplate, AuditService auditService, PermissionService permissionService, ResourceRegistry resourceRegistry, DepartmentRepository departmentRepository, DepartmentMemberRepository departmentMemberRepository){
        this.jdbc = jdbcTemplate;
        this.auditService = auditService;
        this.permissionService = permissionService;
        this.departmentRepository = departmentRepository;
        this.departmentMemberRepository = departmentMemberRepository;
    }
    
    @Override
    @Transactional  
    public  DepartmentResponse createDepartment (DepartmentCreateRequest request){

        validateCreateRequest(request);

        UUID tenantId = TenantContext.current();
        if(tenantId == null){
            throw new ApiProblem(HttpStatus.UNAUTHORIZED, "TENANT_CONTEXT_MISSING", "Không xác định được tenant hiện tại");
        }  

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth == null || !auth.isAuthenticated()){
            throw new ApiProblem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Chưa xác thực");
        }

        permissionService.require(auth, "department", "CREATE", null);

        validateOrganization(tenantId, request.orgId());

        if(departmentRepository.existsByCode(tenantId, request.code())){
            throw new ApiProblem(HttpStatus.CONFLICT, "DEPARTMENT_CODE_EXISTS", "Mã phòng đã tồn tại!");
        }

        if(request.parentId() != null){
            validateParent(tenantId, request.orgId(), request.parentId());
        }

        if (request.managerId() != null) {
            validateManager(tenantId, request.managerId());
        }

        UUID departmentId = departmentRepository.insert(tenantId, request.orgId(), 
                                    request.parentId(),request.code(), 
                                    request.name(), request.description(), 
                                    request.managerId(), "ACTIVE"
        );
                                    
        UUID actorId = permissionService.account(auth);
        String email = auth.getName();

        auditService.record(
            permissionService.tenantKey(auth),
            actorId,
            email,
            "DEPARTMENT_CREATED",
            "department",
            departmentId.toString(),
            "SUCCESS",
            """
            {
                "departmentId": "%s",
                "orgId": "%s",
                "code": "%s",
                "name": "%s",
                "parentId": %s,
                "managerId": %s
            }
            """.formatted(
                departmentId,
                request.orgId(),
                request.code(),
                request.name(),
                request.parentId() == null
                    ? "null"
                    : "\"" + request.parentId() + "\"",
                request.managerId() == null
                    ? "null"
                    : "\"" + request.managerId() + "\""
            )
        );

        DepartmentResponse department = departmentRepository.findByIdAndTenantId(departmentId, tenantId)
                                        .orElseThrow(() -> new ApiProblem(
                                            HttpStatus.NOT_FOUND,
                                            "DEPARTMENT_NOT_FOUND",
                                            "Không tìm thấy phòng ban"
                                        ));

        return department;
    }

    @Override  
    public DepartmentResponse getDepartmentByCode (String code){

        UUID tenantId = TenantContext.current();
        if(tenantId == null){
            throw new ApiProblem(HttpStatus.UNAUTHORIZED, "TENANT_CONTEXT_MISSING", "Không xác định được tenant hiện tại");
        }
        DepartmentResponse department = departmentRepository.findByCodeAndTenantId(code, tenantId)
                                .orElseThrow(() -> new ApiProblem(
                                    HttpStatus.NOT_FOUND,
                                    "DEPARTMENT_NOT_FOUND",
                                    "Không tìm thấy phòng ban"
                                ));
        
        return department;
    }

    @Override 
    public  DepartmentResponse changeOrDeleteManager(Long departmentId){
        return null;
    }

    @Override 
    public  DepartmentTreeResponse getDepartmentTree(Long orgId){
        return  null;
    }

    @Override
    public DepartmentResponse updateDepartment(UUID departmentId){
        return null;
    }

    @Override 
    public List<DepartmentResponse> getAllDepartment(){
        UUID tenantId = TenantContext.current();
        if(tenantId == null){
            throw new ApiProblem(HttpStatus.UNAUTHORIZED, "TENANT_CONTEXT_MISSING", "Không xác định được tenant hiện tại");
        }  
        return  departmentRepository.getAll(tenantId);
    }

    @Override 
    @Transactional 
    public void deleteDepartment (UUID departmentId){
        UUID tenantId = TenantContext.current();
        if(tenantId == null){
            throw new ApiProblem(HttpStatus.UNAUTHORIZED, "TENANT_CONTEXT_MISSING", "Không xác định được tenant hiện tại");
        } 

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth == null || !auth.isAuthenticated()){
            throw new ApiProblem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Chưa xác thực");
        }

        permissionService.require(auth, "department", "DELETE", null);
        
        departmentRepository.deleteByIdAndTenantId(departmentId, tenantId);

        UUID actorId = permissionService.account(auth);
        auditService.record(permissionService.tenantKey(auth),
                            actorId,
                            auth.getName(), 
                            "DELETE_DEPARTMENT",
                            "department",
                            departmentId.toString(),
                            "SUCCESS",
                            null
        );
    }

    private void validateOrganization(UUID tenantId, UUID orgId) {

        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM identity.organization
            WHERE id = ?
            AND tenant_id = ?
            """,
            Integer.class,
            orgId,
            tenantId
        );

        if (count == null || count == 0) {
            throw new ApiProblem(
                HttpStatus.NOT_FOUND,
                "ORGANIZATION_NOT_FOUND",
                "Organization không tồn tại trong tenant hiện tại"
            );
        }
    }

    private void validateManager(UUID tenantId, UUID managerId) {

        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM identity.account
            WHERE id = ?
            AND tenant_id = ?
            """,
            Integer.class,
            managerId,
            tenantId
        );

        if (count == null || count == 0) {
            throw new ApiProblem(
                HttpStatus.NOT_FOUND,
                "MANAGER_NOT_FOUND",
                "Manager không tồn tại trong tenant hiện tại"
            );
        }
    }

    private  void validateParent(UUID tenantId, UUID orgId,  UUID parentId){
        var rows = jdbc.queryForList("""
                SELECT id, org_id, status FROM identity.department
                WHERE id = ? AND tenant_id = ?
                """, parentId, tenantId);
        if(rows.isEmpty()){
            throw new ApiProblem(HttpStatus.NOT_FOUND, "PARENT_DEPARTMENT_NOT_FOUND", "Phòng ban cha không tồn tại");
        }

        var parent = rows.getFirst();

        UUID parentOrgId =
            (UUID) parent.get("org_id");

        if (!orgId.equals(parentOrgId)) {
            throw new ApiProblem(
                HttpStatus.CONFLICT,
                "PARENT_DIFFERENT_ORGANIZATION",
                "Phòng ban cha phải thuộc cùng organization"
            );
        }

        String status =
            String.valueOf(parent.get("status"));

        if (!"ACTIVE".equals(status)) {
            throw new ApiProblem(
                HttpStatus.CONFLICT,
                "PARENT_DEPARTMENT_INACTIVE",
                "Không thể chọn phòng ban INACTIVE làm phòng ban cha"
            );
        }
    }

    private void validateCreateRequest(DepartmentCreateRequest request) {

        if (request == null) {
            throw new ApiProblem(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "Request không được null"
            );
        }

        if (request.orgId() == null) {
            throw new ApiProblem(
                HttpStatus.BAD_REQUEST,
                "ORG_ID_REQUIRED",
                "orgId không được null"
            );
        }

        if (request.code() == null ||
            request.code().isBlank()) {

            throw new ApiProblem(
                HttpStatus.BAD_REQUEST,
                "DEPARTMENT_CODE_REQUIRED",
                "Mã phòng ban không được để trống"
            );
        }

        if (request.code().length() > 80) {
            throw new ApiProblem(
                HttpStatus.BAD_REQUEST,
                "DEPARTMENT_CODE_TOO_LONG",
                "Mã phòng ban tối đa 80 ký tự"
            );
        }

        if (request.name() == null ||
            request.name().isBlank()) {

            throw new ApiProblem(
                HttpStatus.BAD_REQUEST,
                "DEPARTMENT_NAME_REQUIRED",
                "Tên phòng ban không được để trống"
            );
        }

        if (request.name().length() > 160) {
            throw new ApiProblem(
                HttpStatus.BAD_REQUEST,
                "DEPARTMENT_NAME_TOO_LONG",
                "Tên phòng ban tối đa 160 ký tự"
            );
        }

        if (request.description() != null &&
            request.description().length() > 250) {

            throw new ApiProblem(
                HttpStatus.BAD_REQUEST,
                "DEPARTMENT_DESCRIPTION_TOO_LONG",
                "Mô tả tối đa 250 ký tự"
            );
        }
    }
}
