package dev.vality.orgmanager.entity;

import lombok.*;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "member_role")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MemberRoleEntity implements Serializable {

    @Id
    @EqualsAndHashCode.Include
    private String id;
    private String organizationId;
    private String roleId;
    private String scopeId;
    private String resourceId;
    private boolean active = true;
}
