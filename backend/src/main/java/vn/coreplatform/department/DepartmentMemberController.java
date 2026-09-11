package vn.coreplatform.department;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import vn.coreplatform.department.enums.DepartmentMemberRole;
import vn.coreplatform.department.request.MemberAddRequest;
import vn.coreplatform.department.response.MemberResponse;
import vn.coreplatform.department.service.member.IMemberService;

@RestController
@RequestMapping("/api/departments/{departmentId}/members")
public class DepartmentMemberController {

    private final IMemberService memberService;

    public DepartmentMemberController(IMemberService memberService) {
        this.memberService = memberService;
    }

    // POST /api/departments/{departmentId}/members/add
    @PostMapping("/add")
    public ResponseEntity<MemberResponse> addMember(
            @PathVariable UUID departmentId,
            @Valid @RequestBody MemberAddRequest request
    ) {
        MemberResponse response =
                memberService.addMemberToDepartment(
                        request,
                        departmentId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<MemberResponse>> getAll(
            @PathVariable UUID departmentId
    ) {
        List<MemberResponse> members =
                memberService.getAllMemberInDeapartment(departmentId);

        return ResponseEntity.ok(members);
    }

    // GET /api/departments/{departmentId}/members/{memberId}
    @GetMapping("/{memberId}")
    public ResponseEntity<MemberResponse> findById(
            @PathVariable UUID departmentId,
            @PathVariable UUID memberId
    ) {
        MemberResponse member =
                memberService.getMember(memberId, departmentId);

        return ResponseEntity.ok(member);
    }

  
    @PutMapping("/{memberId}")
    public ResponseEntity<MemberResponse> updateMemberRole(
            @PathVariable UUID departmentId,
            @PathVariable UUID memberId,
            DepartmentMemberRole role
    ) {
        MemberResponse member =
                memberService.updateMemberRole(
                        memberId,
                        departmentId,
                        role
                );

        return ResponseEntity.ok(member);
    }

    @DeleteMapping("/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable UUID departmentId,
            @PathVariable UUID memberId
    ) {
        memberService.removeMember(
                memberId,
                departmentId
        );
    }
}