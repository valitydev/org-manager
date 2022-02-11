package dev.vality.orgmanager.repository;

import dev.vality.orgmanager.entity.MemberRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberRoleRepository extends JpaRepository<MemberRoleEntity, String> {

    List<MemberRoleEntity> findByOrganizationId(String organizationId);
}
