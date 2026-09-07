package vn.coreplatform.department;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import vn.coreplatform.department.request.MemberAddRequest;
import vn.coreplatform.department.response.MemberResponse;
import vn.coreplatform.department.service.IDepartmentService;

@RestController 
@RequestMapping("/api/departments/{departmentCode}/members")
public class DepartmentMemberController {
    private final IDepartmentService departmentService;

    public DepartmentMemberController (IDepartmentService departmentService){
        this.departmentService = departmentService;
    }

    @PostMapping("/add")
    public ResponseEntity<MemberResponse> addMember(
            @PathVariable String departmentCode,
            @Valid @RequestBody MemberAddRequest request
    ) {
        MemberResponse response =
                departmentService.addMemberToDepartment(
                    request,
                    departmentCode
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
