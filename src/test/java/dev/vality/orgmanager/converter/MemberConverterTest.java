package dev.vality.orgmanager.converter;

import dev.vality.orgmanager.entity.MemberEntity;
import dev.vality.orgmanager.entity.MemberRoleEntity;
import dev.vality.orgmanager.entity.OrganizationEntity;
import dev.vality.swag.organizations.model.Member;
import dev.vality.swag.organizations.model.MemberRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemberConverterTest {

    private MemberConverter converter;

    @BeforeEach
    public void setUp() {
        MemberRoleConverter memberRoleConverter = mock(MemberRoleConverter.class);
        when(memberRoleConverter.toDomain(any(MemberRoleEntity.class)))
                .thenReturn(new MemberRole());
        when(memberRoleConverter.toEntity(any(MemberRole.class), anyString()))
                .thenReturn(new MemberRoleEntity());

        converter = new MemberConverter(memberRoleConverter);
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

        assertThat(member).usingRecursiveComparison()
                .isEqualTo(expected);
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