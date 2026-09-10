package vn.coreplatform.department.response;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DepartmentTreeResponse {

    private UUID id;
    private String code;
    private String name;
    private String status;
    private UUID parentId;
    private String managerName;
    private int memberCount;
    private List<DepartmentTreeResponse> children = new ArrayList<>();

    public DepartmentTreeResponse() {
    }

    public DepartmentTreeResponse(
            UUID id,
            String code,
            String name,
            String status,
            UUID parentId,
            String managerName,
            int memberCount
    ) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.status = status;
        this.parentId = parentId;
        this.managerName = managerName;
        this.memberCount = memberCount;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public UUID getParentId() { return parentId; }
    public String getManagerName() { return managerName; }
    public int getMemberCount() { return memberCount; }
    public List<DepartmentTreeResponse> getChildren() { return children; }

    public void setId(UUID id) { this.id = id; }
    public void setCode(String code) { this.code = code; }
    public void setName(String name) { this.name = name; }
    public void setStatus(String status) { this.status = status; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }
    public void setManagerName(String managerName) { this.managerName = managerName; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
    public void setChildren(List<DepartmentTreeResponse> children) { this.children = children; }
}