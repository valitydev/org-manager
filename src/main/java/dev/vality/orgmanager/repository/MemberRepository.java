package dev.vality.orgmanager.repository;

import dev.vality.orgmanager.entity.MemberEntity;
import dev.vality.orgmanager.service.dto.MemberWithRoleDto;
import dev.vality.orgmanager.service.dto.UserDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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
     * Страница идентификаторов участников организации, отсортированная по возрастанию id.
     * continuationToken == null — первая страница, иначе участники строго после указанного id.
     */
    @NativeQuery("SELECT m.id " +
            " FROM org_manager.member_to_organization mto " +
            "     JOIN org_manager.member m " +
            "       ON m.id = mto.member_id " +
            " WHERE mto.organization_id = ?1 " +
            "   AND (CAST(?2 AS VARCHAR) IS NULL OR m.id > ?2) " +
            " ORDER BY m.id ")
    List<String> getOrgMemberIds(String orgId, String continuationToken, Pageable pageable);

    /**
     * То же, что {@link #getOrgMemberList(String)}, но джойн ролей внешний и выборка ограничена
     * заданными участниками: участник без активных ролей в организации тоже попадает в выдачу.
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
            "   AND m.id IN (?2) " +
            " ORDER BY m.id, mr.id ")
    List<MemberWithRoleDto> getOrgMemberListWithRoles(String orgId, Collection<String> memberIds);

    /**
     * Страница пользователей по возрастанию id
     */
    @NativeQuery("SELECT m.id, " +
            "              m.email " +
            " FROM org_manager.member m " +
            " WHERE (CAST(?1 AS VARCHAR) IS NULL OR m.id > ?1) " +
            "   AND (CAST(?2 AS VARCHAR) IS NULL OR m.email = ?2) " +
            " ORDER BY m.id ")
    List<UserDto> getUserPage(String continuationToken, String email, Pageable pageable);

    boolean existsById(String id);


}
