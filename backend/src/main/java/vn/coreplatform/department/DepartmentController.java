package vn.coreplatform.department;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.coreplatform.department.request.DepartmentCreateRequest;
import vn.coreplatform.department.response.DepartmentResponse;
import vn.coreplatform.department.service.DepartmentService;


@RestController 
@RequestMapping("/api/v1/departments")
public class DepartmentController {

    private  final DepartmentService departmentService;

    public  DepartmentController(DepartmentService departmentService){
        this.departmentService = departmentService;
    }

    @PostMapping("/add")
    public ResponseEntity<DepartmentResponse> createDepartment(@RequestBody DepartmentCreateRequest request){
            
        DepartmentResponse department = departmentService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(department);
    }

    

    

}
