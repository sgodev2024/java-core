package vn.coreplatform.department.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import vn.coreplatform.department.enums.DepartmentMemberRole;
import vn.coreplatform.department.response.MemberResponse;

@Repository 
public class DepartmentMemberRepository {
    private  JdbcTemplate jdbc;

    public DepartmentMemberRepository ( JdbcTemplate jdbc){
        this.jdbc = jdbc;
    }

    public  boolean existsByAccountId(UUID accountId, UUID departmentId, UUID tenantId){
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM identity.department_member
                WHERE account_id = ?
                AND tenant_id = ?
                AND department_id = ? 
                AND removed_at IS NULL
                """, Integer.class, accountId, tenantId, departmentId
        );

        return count != null && count > 0;
    }

    public UUID insert(UUID tenantId, UUID departmentId, UUID accountId, String role){
        return  jdbc.queryForObject("""
                INSERT INTO identity.department_member(
                    tenant_id,
                    department_id,
                    account_id,
                    role_in_dept
                )
                VALUES (?, ?, ?, ?)
                RETURNING id
                """, UUID.class, tenantId, departmentId, accountId, role);
    }

    public Optional<MemberResponse> findById(
        UUID memberId,
        UUID tenantId,
        UUID departmentId
    ) {
        return jdbc.query("""
            SELECT
                dm.id,
                dm.account_id,
                a.email,
                a.display_name,
                dm.role_in_dept,
                dm.assigned_at,
                dm.removed_at
            FROM identity.department_member dm
            JOIN identity.account a
                ON a.id = dm.account_id
            WHERE dm.id = ?
            AND dm.tenant_id = ?
            AND dm.department_id = ?
            """,
            (rs, rowNum) -> new MemberResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("account_id", UUID.class),
                rs.getString("email"),
                rs.getString("display_name"),
                DepartmentMemberRole.valueOf(
                    rs.getString("role_in_dept")
                ),
                rs.getObject("assigned_at", Instant.class),
                rs.getObject("removed_at", Instant.class)
            ),
            memberId,
            tenantId,
            departmentId
        ).stream().findFirst();
    }

}
