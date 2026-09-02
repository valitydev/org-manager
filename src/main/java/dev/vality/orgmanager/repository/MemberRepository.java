package dev.vality.orgmanager.repository;

import dev.vality.orgmanager.entity.MemberEntity;
import dev.vality.orgmanager.service.dto.MemberWithRoleDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, String> {


    @NativeQuery("SELECT m.id, " +
            "              m.email,  " +
            "              mr.id as memberRoleId, " +
            "              mr.organization_id as organizationId, " +
            "              mr.role_id as roleId, " +
            "              mr.scope_id as scopeId, " +
            "              mr.resource_id as resourceId" +
            " FROM org_manager.member_to_member_role mtmr,  " +
            "     org_manager.member_to_organization mto,  " +
            "     org_manager.member_role mr, " +
            "     org_manager.member m  " +
            " WHERE  " +
            "         mto.organization_id = ?1 " +
            "     AND mto .member_id = m.id " +
            "     AND mr.active = 'true' " +
            "     AND mr.id = mtmr.member_role_id " +
            "     AND mr.organization_id = mto.organization_id " +
            "     AND m.id = mtmr.member_id ")
    List<MemberWithRoleDto> getOrgMemberList(String orgId);

    /**
     * То же, что {@link #getOrgMemberList(String)}, но джойн ролей внешний: участник без активных
     * ролей в организации тоже попадает в выдачу. Административный контракт умеет добавлять
     * участника без роли (AddMember), и такой участник не должен пропадать из списка.
     * У строк для участника без ролей memberRoleId == null.
     */
    @NativeQuery("SELECT m.id, " +
            "              m.email, " +
            "              mr.id as memberRoleId, " +
            "              mr.organization_id as organizationId, " +
            "              mr.role_id as roleId, " +
            "              mr.scope_id as scopeId, " +
            "              mr.resource_id as resourceId" +
            " FROM org_manager.member_to_organization mto " +
            "     JOIN org_manager.member m " +
            "       ON m.id = mto.member_id " +
            "     LEFT JOIN org_manager.member_to_member_role mtmr " +
            "       ON mtmr.member_id = m.id " +
            "     LEFT JOIN org_manager.member_role mr " +
            "       ON mr.id = mtmr.member_role_id " +
            "      AND mr.active IS TRUE " +
            "      AND mr.organization_id = mto.organization_id " +
            " WHERE mto.organization_id = ?1 " +
            " ORDER BY m.id, mr.id ")
    List<MemberWithRoleDto> getOrgMemberListWithRoles(String orgId);

    boolean existsById(String id);


}
