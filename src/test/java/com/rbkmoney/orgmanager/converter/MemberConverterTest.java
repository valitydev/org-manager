package com.rbkmoney.orgmanager.converter;

import com.rbkmoney.bouncer.context.v1.User;
import com.rbkmoney.orgmanager.entity.MemberEntity;
import com.rbkmoney.orgmanager.entity.MemberRoleEntity;
import com.rbkmoney.orgmanager.entity.OrganizationEntity;
import com.rbkmoney.swag.organizations.model.Member;
import com.rbkmoney.swag.organizations.model.MemberRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MemberConverterTest {

    private MemberConverter converter;

    @BeforeEach
    public void setUp() {
        MemberRoleConverter memberRoleConverter = mock(MemberRoleConverter.class);
        when(memberRoleConverter.toDomain(any(MemberRoleEntity.class)))
                .thenReturn(new MemberRole());
        when(memberRoleConverter.toEntity(any(MemberRole.class), anyString()))
                .thenReturn(new MemberRoleEntity());

        OrganizationConverter organizationConverter = mock(OrganizationConverter.class);
        when(organizationConverter.toThrift(any(OrganizationEntity.class)))
                .thenReturn(new com.rbkmoney.bouncer.context.v1.Organization());

        converter = new MemberConverter(memberRoleConverter, organizationConverter);
    }

    @Test
    void shouldConvertToDomain() {
        // Given
        MemberEntity entity = buildMemberEntity();

        // When
        Member member = converter.toDomain(entity);

        // Then
        Member expected = new Member()
                .id("id")
                .userEmail("email")
                .roles(List.of(new MemberRole()));

        assertThat(member).isEqualToComparingFieldByField(expected);
    }

    @Test
    void shouldConvertToThrift() {
        MemberEntity entity = buildMemberEntity();
        User user = converter.toThrift(entity);
        assertEquals(entity.getId(), user.getId());
        assertEquals(entity.getEmail(), user.getEmail());
        assertEquals(entity.getOrganizations().size(), user.getOrgs().size());
    }

    private MemberEntity buildMemberEntity() {
        return MemberEntity.builder()
                .id("id")
                .email("email")
                .roles(Set.of(new MemberRoleEntity()))
                .organizations(Set.of(new OrganizationEntity()))
                .build();
    }

}