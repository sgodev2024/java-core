package vn.coreplatform.department.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.coreplatform.department.response.DepartmentResponse;
import vn.coreplatform.department.response.DepartmentTreeResponse;

@Repository 
public class DepartmentRepository {
    private  final JdbcTemplate jdbc;

    public DepartmentRepository (JdbcTemplate jdbc){
        this.jdbc =jdbc;
    }

    public boolean existsByCode(UUID tenantId, String code){
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) 
                FROM identity.department
                WHERE tenant_id = ?
                    AND code = ?
                """, Integer.class, tenantId, code);
        return count != null && count > 0;
    }

    public boolean existsByIdAndTenant(UUID tenantId, UUID departmentId){
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM identity.department
                WHERE tenant_id = ?
                    AND id = ?
                """, Integer.class, tenantId, departmentId);
        return  count != null && count > 0;
    }

    public  boolean existsByIdAndTenantAndOrg(UUID tenantId, UUID departmentId, UUID orgId){
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM identity.department
                WHERE tenant_id = ?
                    AND id = ?
                    AND org_id = ?
                """, Integer.class, tenantId, departmentId, orgId);
        return  count != null && count > 0;
    }

    public UUID insert(
            UUID tenantId,
            UUID orgId,
            UUID parentId,
            String code,
            String name,
            String description,
            UUID managerId,
            String status
    ) {
        return jdbc.queryForObject("""
            INSERT INTO identity.department (
                tenant_id,
                org_id,
                parent_id,
                code,
                name,
                description,
                manager_id,
                status
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """,
            UUID.class,
            tenantId,
            orgId,
            parentId,
            code,
            name,
            description,
            managerId,
            status
        );
    }

    public Optional<DepartmentResponse> findByIdAndTenantId(
        UUID departmentId,
        UUID tenantId
    ) {
        return jdbc.query("""
                SELECT
                    d.id,
                    d.code,
                    d.name,
                    d.description,

                    d.org_id,
                    o.name AS org_name,

                    d.parent_id,
                    p.name AS parent_name,

                    d.manager_id,
                    a.display_name AS manager_name,

                    d.status,
                    d.sort_order,

                    (
                        SELECT COUNT(*)
                        FROM identity.department_member dm
                        WHERE dm.department_id = d.id
                        AND dm.tenant_id = d.tenant_id
                        AND dm.removed_at IS NULL
                    ) AS member_count,

                    d.create_at AS created_at,
                    d.updated_at AS updated_at

                FROM identity.department d

                LEFT JOIN identity.organization o
                    ON o.id = d.org_id
                AND o.tenant_id = d.tenant_id

                LEFT JOIN identity.department p
                    ON p.id = d.parent_id
                AND p.tenant_id = d.tenant_id

                LEFT JOIN identity.account a
                    ON a.id = d.manager_id

                WHERE d.id = ?
                AND d.tenant_id = ?
                """,
            (rs, rowNum) -> new DepartmentResponse(
                rs.getObject("id", UUID.class),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("description"),

                rs.getObject("org_id", UUID.class),
                rs.getString("org_name"),

                rs.getObject("parent_id", UUID.class),
                rs.getString("parent_name"),

                rs.getObject("manager_id", UUID.class),
                rs.getString("manager_name"),

                rs.getString("status"),
                rs.getInt("sort_order"),
                rs.getInt("member_count"),

                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
            ),
            departmentId,
            tenantId
        ).stream().findFirst();
    }
    
    public Optional<DepartmentResponse> findByCodeAndTenantId(String code, UUID tenantId){
        return jdbc.query("""
            SELECT
                d.id,
                d.code,
                d.name,
                d.description,
                d.org_id,
                o.name AS org_name,
                d.parent_id,
                p.name AS parent_name,
                d.manager_id,
                a.display_name AS manager_name,
                d.status,
                d.sort_order,
                (
                    SELECT COUNT(*)
                    FROM identity.department_member dm
                    WHERE dm.department_id = d.id
                    AND dm.tenant_id = d.tenant_id
                    AND dm.removed_at IS NULL
                ) AS member_count,
                d.create_at AS created_at,
                d.updated_at AS updated_at
            FROM identity.department d
            LEFT JOIN identity.organization o
                ON o.id = d.org_id
            AND o.tenant_id = d.tenant_id
            LEFT JOIN identity.department p
                ON p.id = d.parent_id
            AND p.tenant_id = d.tenant_id
            LEFT JOIN identity.account a
                ON a.id = d.manager_id
            WHERE d.code = ?
            AND d.tenant_id = ?
            """,
                (rs, rowNum) -> new DepartmentResponse(
                rs.getObject("id", UUID.class),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("description"),

                rs.getObject("org_id", UUID.class),
                rs.getString("org_name"),

                rs.getObject("parent_id", UUID.class),
                rs.getString("parent_name"),

                rs.getObject("manager_id", UUID.class),
                rs.getString("manager_name"),

                rs.getString("status"),
                rs.getInt("sort_order"),
                rs.getInt("member_count"),

                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
            ),
            code,
            tenantId
        ).stream().findFirst();
    }

    public List<DepartmentResponse> getAll(UUID tenantId) {
        return jdbc.query("""
            SELECT
                d.id,
                d.code,
                d.name,
                d.description,
                d.org_id,
                o.name AS org_name,
                d.parent_id,
                p.name AS parent_name,
                d.manager_id,
                a.display_name AS manager_name,
                d.status,
                d.sort_order,
                (
                    SELECT COUNT(*)
                    FROM identity.department_member dm
                    WHERE dm.department_id = d.id
                    AND dm.tenant_id = d.tenant_id
                    AND dm.removed_at IS NULL
                ) AS member_count,
                d.create_at AS created_at,
                d.updated_at AS updated_at
            FROM identity.department d

            LEFT JOIN identity.organization o
                ON o.id = d.org_id
            AND o.tenant_id = d.tenant_id

            LEFT JOIN identity.department p
                ON p.id = d.parent_id
            AND p.tenant_id = d.tenant_id

            LEFT JOIN identity.account a
                ON a.id = d.manager_id

            WHERE d.tenant_id = ?

            ORDER BY d.sort_order ASC, d.name ASC
            """,
            (rs, rowNum) -> new DepartmentResponse(
                rs.getObject("id", UUID.class),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("description"),

                rs.getObject("org_id", UUID.class),
                rs.getString("org_name"),

                rs.getObject("parent_id", UUID.class),
                rs.getString("parent_name"),

                rs.getObject("manager_id", UUID.class),
                rs.getString("manager_name"),

                rs.getString("status"),
                rs.getInt("sort_order"),
                rs.getInt("member_count"),

                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
            ),
            tenantId
        );
    }

    public int deleteByIdAndTenantId(UUID departmentId, UUID tenantId){
        return jdbc.update("""
                DELETE FROM identity.department
                WHERE id =?
                    AND tenant_id = ?
                """,departmentId, tenantId
        );
    }

    public List<DepartmentTreeResponse> getTreeData(UUID tenantId, UUID orgId) {
        return jdbc.query("""
            SELECT
                d.id,
                d.code,
                d.name,
                d.status,
                d.parent_id,
                a.display_name AS manager_name,
                (
                    SELECT COUNT(*)
                    FROM identity.department_member dm
                    WHERE dm.tenant_id = d.tenant_id
                    AND dm.department_id = d.id
                    AND dm.removed_at IS NULL
                ) AS member_count
            FROM identity.department d
            LEFT JOIN identity.account a
                ON a.id = d.manager_id
            WHERE d.tenant_id = ?
            AND d.org_id = ?
            ORDER BY
                d.sort_order ASC,
                d.name ASC
            """,
            (rs, rowNum) -> new DepartmentTreeResponse(
                rs.getObject("id", UUID.class),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("status"),
                rs.getObject("parent_id", UUID.class),
                rs.getString("manager_name"),
                rs.getInt("member_count")
            ),
            tenantId,
            orgId
        );
    }
}
