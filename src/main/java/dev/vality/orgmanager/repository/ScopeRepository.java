package dev.vality.orgmanager.repository;

import dev.vality.orgmanager.entity.ScopeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScopeRepository extends JpaRepository<ScopeEntity, String> {
}
