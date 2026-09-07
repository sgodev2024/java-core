CREATE TABLE identity.department (
    
    id uuid primary key default gen_random_uuid(),
    tenant_id uuid not null references platform.tenant(id) -- thuộc doanh nghiệp nào,
    org_id uuid  not null -- thuộc tổ chức nào,
    parent_id uuid references identity.department(id) --thuộc quản lí của phòng nào,
    code varchar(80) not null -- mỗi phòng có một mã code riêng,
    name varchar(160) not null -- tên hiển thị,
    description varchar(250) -- mô tả (options),
    manager_id uuid -- ai là người quản lí phòng ban,
    status varchar(20) not null default 'ACTIVE'
            check (status in ('ACTIVE', 'INACTIVE')) -- trạng thái phòng ban,
    sort_order int not null default 0 -- sắp xếp phòng ban khi hiển thị,
    create_at timestamptz not null default now() ,
    updated_at timestamptz not null default now(),
    unique(tenant_id, code) -- trong một doanh nghiệp không thể tồn tại 2 phòng ban cùng code(cùng nghiệp vụ),
    foreign key (org_id, tenant_id)
        references identity.organization(id, tenant_id) -- phòng ban phải thuộc organization tồn tại,
    foreign key (manager_id)
        references identity.account(id) -- người quản lí là tài khoản có trong identity.account
);

-- index để truy vấn theo org_id
CREATE INDEX department_org_idx ON identity.department(tenant_id, org_id);

CREATE UNIQUE INDEX department_id_tenant_uidx ON identity.department(id, tenant_id);


CREATE TABLE identity.department_member (
    id uuid primary key default gen_random_uuid(),
    tenant_id uuid not null references platform.tenant(id),
    department_id uuid not null,
    account_id uuid not null references identity.account(id),
    role_in_dept varchar(20) not null default 'MEMBER'
                check ( role_in_dept in ('MEMBER', 'LEAD', 'DEPUTY')),
    assigned_at timestamptz not null default now(),
    removed_at timestamptz,
    unique (tenant_id, department_id, account_id),
    foreign key (department_id, tenant_id)
            references identity.department(id, tenant_id)
);

ALTER TABLE identity.department ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity.department FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON identity.department
    USING (tenant_id = platform.current_tenant_id())
    WITH CHECK (tenant_id = platform.current_tenant_id());
ALTER TABLE identity.department_member ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity.department_member FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON identity.department_member
    USING (tenant_id = platform.current_tenant_id())
    WITH CHECK (tenant_id = platform.current_tenant_id());

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'core_app') THEN
    GRANT SELECT, INSERT, UPDATE, DELETE
        ON identity.department, identity.department_member TO core_app;
  END IF;
END $$;

