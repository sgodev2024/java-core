package vn.coreplatform.department.service.member;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.coreplatform.audit.AuditService;
import vn.coreplatform.department.enums.DepartmentMemberRole;
import vn.coreplatform.department.repository.DepartmentMemberRepository;
import vn.coreplatform.department.repository.DepartmentRepository;
import vn.coreplatform.department.request.MemberAddRequest;
import vn.coreplatform.department.response.DepartmentResponse;
import vn.coreplatform.department.response.MemberResponse;
import vn.coreplatform.kernel.TenantContext;
import vn.coreplatform.permission.PermissionService;
import vn.coreplatform.shared.ApiExceptionHandler.ApiProblem;

@Service 
public class MemberService implements IMemberService{
    private final JdbcTemplate jdbc;
    private final AuditService auditService;
    private final PermissionService permissionService;
    private final DepartmentMemberRepository departmentMemberRepository;
    private final DepartmentRepository departmentRepository;

    public MemberService(JdbcTemplate jdbc, AuditService auditService, PermissionService permissionService, DepartmentMemberRepository departmentMemberRepository, DepartmentRepository departmentRepository){
        this.jdbc = jdbc;
        this.auditService = auditService;
        this.permissionService = permissionService;
        this.departmentMemberRepository = departmentMemberRepository;
        this.departmentRepository = departmentRepository;
    }

    @Override 
    @Transactional 
    public MemberResponse addMemberToDepartment (MemberAddRequest request, UUID departmentId){
        UUID tenantId = TenantContext.current();
        DepartmentResponse department = departmentRepository.findByIdAndTenantId(departmentId, tenantId)
                                    .orElseThrow(()-> new ApiProblem(HttpStatus.NOT_FOUND, "DEPARTMENT_NOT_FOUND", "Không tìm thấy phòng code: " + departmentId)
        );

        if(departmentMemberRepository.existsByAccountId(request.accountId(), department.id(), tenantId)){
            throw new ApiProblem(
            HttpStatus.CONFLICT,
            "MEMBER_ALREADY_EXISTS",
            "Account đã tồn tại trong department"
            );
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth == null || !auth.isAuthenticated()){
            throw new ApiProblem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Chưa xác thực");
        }
        permissionService.require(auth, "department-member", "CREATE", null);
        UUID memberId = departmentMemberRepository.insert(tenantId, department.id(), request.accountId(), request.role().name());
        
        UUID actorId = permissionService.account(auth);
        String email = auth.getName();
        auditService.record(
            permissionService.tenantKey(auth),
            actorId,
            email,
            "DEPARTMENT_CREATED",
            "department",
            memberId.toString(),
            "SUCCESS",
            null
        );
        
        MemberResponse member = departmentMemberRepository.findById(memberId, tenantId, department.id())
                                .orElseThrow(()-> new ApiProblem(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "Không tìm thấy thành viên trong phòng : " + department.code()));

        return member;
    }

    @Override 
    public MemberResponse getMember (UUID memberId, UUID departmentId){
        UUID tenantId = TenantContext.current();
        if(tenantId == null){
            throw new ApiProblem(HttpStatus.UNAUTHORIZED, "TENANT_CONTEXT_MISSING", "Không xác định được tenant hiện tại");
        }    

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth == null || !auth.isAuthenticated()){
            throw new ApiProblem(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Chưa xác thực");
        }

        permissionService.require(auth, "department", "READ", null);
        MemberResponse member = departmentMemberRepository.findById(memberId, tenantId, departmentId)
                                .orElseThrow(()-> new ApiProblem(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "Không tìm thấy thành viên trong phòng!"));
        
        return  member;
    }

    @Override
    @Transactional
    public MemberResponse updateMemberRole(
            UUID memberId,
            UUID departmentId,
            DepartmentMemberRole role
    ) {
        UUID tenantId = TenantContext.current();

        if (tenantId == null) {
            throw new ApiProblem(
                HttpStatus.UNAUTHORIZED,
                "TENANT_CONTEXT_MISSING",
                "Không xác định được tenant hiện tại"
            );
        }

        Authentication auth =
            SecurityContextHolder.getContext()
                .getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new ApiProblem(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHENTICATED",
                "Chưa xác thực"
            );
        }

        permissionService.require(
            auth,
            "DEPARTMENT_MEMBER",
            "UPDATE",
            null
        );

        if (role == null) {
            throw new ApiProblem(
                HttpStatus.BAD_REQUEST,
                "ROLE_REQUIRED",
                "Role không được để trống"
            );
        }

        departmentRepository
            .findByIdAndTenantId(departmentId, tenantId)
            .orElseThrow(() -> new ApiProblem(
                HttpStatus.NOT_FOUND,
                "DEPARTMENT_NOT_FOUND",
                "Không tìm thấy phòng ban"
            ));

        MemberResponse member =
            departmentMemberRepository
                .findById(
                    memberId,
                    tenantId,
                    departmentId
                )
                .orElseThrow(() -> new ApiProblem(
                    HttpStatus.NOT_FOUND,
                    "MEMBER_NOT_FOUND",
                    "Không tìm thấy thành viên trong phòng ban"
                ));

        if (member.removedAt() != null) {
            throw new ApiProblem(
                HttpStatus.BAD_REQUEST,
                "MEMBER_REMOVED",
                "Thành viên đã bị xóa khỏi phòng ban"
            );
        }

        int updated =
            departmentMemberRepository.updateRole(
                memberId,
                departmentId,
                tenantId,
                role
            );

        if (updated == 0) {
            throw new ApiProblem(
                HttpStatus.NOT_FOUND,
                "MEMBER_NOT_FOUND",
                "Không tìm thấy thành viên"
            );
        }

        MemberResponse result =
            departmentMemberRepository
                .findById(
                    memberId,
                    tenantId,
                    departmentId
                )
                .orElseThrow(() -> new ApiProblem(
                    HttpStatus.NOT_FOUND,
                    "MEMBER_NOT_FOUND",
                    "Không tìm thấy thành viên"
                ));

        auditService.record(
            permissionService.tenantKey(auth),
            permissionService.account(auth),
            auth.getName(),
            "DEPARTMENT_MEMBER_ROLE_UPDATED",
            "DEPARTMENT_MEMBER",
            memberId.toString(),
            "SUCCESS",
            null
        );

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> getAllMemberInDeapartment(
            UUID departmentId
    ) {
        UUID tenantId = TenantContext.current();

        if (tenantId == null) {
            throw new ApiProblem(
                HttpStatus.UNAUTHORIZED,
                "TENANT_CONTEXT_MISSING",
                "Không xác định được tenant hiện tại"
            );
        }

        Authentication auth =
            SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new ApiProblem(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHENTICATED",
                "Chưa xác thực"
            );
        }

        permissionService.require(
            auth,
            "DEPARTMENT_MEMBER",
            "READ",
            null
        );

        departmentRepository
            .findByIdAndTenantId(departmentId, tenantId)
            .orElseThrow(() -> new ApiProblem(
                HttpStatus.NOT_FOUND,
                "DEPARTMENT_NOT_FOUND",
                "Không tìm thấy phòng ban"
            ));

        return departmentMemberRepository.getAll(
            tenantId,
            departmentId
        );
    }

    @Override
    @Transactional
    public void removeMember(UUID memberId, UUID departmentId) {

        UUID tenantId = TenantContext.current();

        if (tenantId == null) {
            throw new ApiProblem(
                HttpStatus.UNAUTHORIZED,
                "TENANT_CONTEXT_MISSING",
                "Không xác định được tenant hiện tại"
            );
        }

        Authentication auth =
            SecurityContextHolder.getContext()
                .getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new ApiProblem(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHENTICATED",
                "Chưa xác thực"
            );
        }

        permissionService.require(
            auth,
            "DEPARTMENT_MEMBER",
            "DELETE",
            null
        );

        MemberResponse member =
            departmentMemberRepository
                .findById(
                    memberId,
                    tenantId,
                    departmentId
                )
                .orElseThrow(() -> new ApiProblem(
                    HttpStatus.NOT_FOUND,
                    "MEMBER_NOT_FOUND",
                    "Không tìm thấy thành viên trong phòng ban"
                ));

        if (member.removedAt() != null) {
            throw new ApiProblem(
                HttpStatus.BAD_REQUEST,
                "MEMBER_ALREADY_REMOVED",
                "Thành viên đã được xóa khỏi phòng ban"
            );
        }

        int updated =
            departmentMemberRepository.removeMember(
                memberId,
                departmentId,
                tenantId
            );

        if (updated == 0) {
            throw new ApiProblem(
                HttpStatus.NOT_FOUND,
                "MEMBER_NOT_FOUND",
                "Không tìm thấy thành viên đang hoạt động"
            );
        }

        auditService.record(
            permissionService.tenantKey(auth),
            permissionService.account(auth),
            auth.getName(),
            "DEPARTMENT_MEMBER_REMOVED",
            "DEPARTMENT_MEMBER",
            memberId.toString(),
            "SUCCESS",
            null
        );
    }

}
