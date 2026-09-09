package vn.coreplatform.department;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.coreplatform.department.request.DepartmentCreateRequest;
import vn.coreplatform.department.response.DepartmentResponse;
import vn.coreplatform.department.service.department.DepartmentService;


@RestController 
@RequestMapping("/api/v1/departments")
public class DepartmentController {

    private  final DepartmentService departmentService;

    public  DepartmentController(DepartmentService departmentService){
        this.departmentService = departmentService;
    }

    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getAll(){
        List<DepartmentResponse> departments = departmentService.getAllDepartment();
        return ResponseEntity.status(HttpStatus.FOUND).body(departments);
    }

    @PostMapping("/add")
    public ResponseEntity<DepartmentResponse> createDepartment(@RequestBody DepartmentCreateRequest request){
            
        DepartmentResponse department = departmentService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(department);
    }

    @GetMapping("/{departmentId}")
    public ResponseEntity<DepartmentResponse> getDepartmentByCode(String code){
        DepartmentResponse department = departmentService.getDepartmentByCode(code);
        return ResponseEntity.status(HttpStatus.FOUND).body(department);
    }

    @DeleteMapping("/{departmentId}/delete")
    public ResponseEntity<Void> deleteDepartment(@PathVariable UUID departmentId){
        departmentService.deleteDepartment(null);
        return ResponseEntity.noContent().build();
    }
}
