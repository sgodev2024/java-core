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
    public MemberResponse addMemberToDepartment (MemberAddRequest request, String departmentCode){
        UUID tenantId = TenantContext.current();
        DepartmentResponse department = departmentRepository.findByCodeAndTenantId(departmentCode, tenantId)
                                    .orElseThrow(()-> new ApiProblem(HttpStatus.NOT_FOUND, "DEPARTMENT_NOT_FOUND", "Không tìm thấy phòng code: " +departmentCode)
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
                                .orElseThrow(()-> new ApiProblem(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "Không tìm thấy thành viên trong phòng : " + departmentCode));

        return member;
    }

     @Override 
    public MemberResponse getMember (Long accountId, Long departmentId){
        return  null;
    }

    @Override 
    public MemberResponse updateMemberRole (Long accountId, Long departmentId){
        return null;
    }

    @Override 
    public List<MemberResponse> getAllMemberInDeapartment(Long departmentId){
        return null;
    }

    @Override 
    public void removeMember (Long accountId, Long departmentId){

    }

}
