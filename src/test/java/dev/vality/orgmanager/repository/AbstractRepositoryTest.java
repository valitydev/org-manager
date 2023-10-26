package dev.vality.orgmanager.repository;

import dev.vality.orgmanager.OrgManagerApplication;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {OrgManagerApplication.class})
public abstract class AbstractRepositoryTest {

    private static final String POSTGRESQL_IMAGE_NAME = "postgres";
    private static final String POSTGRESQL_VERSION = "13.6";

    @Autowired
    protected InvitationRepository invitationRepository;

    @Autowired
    protected OrganizationRepository organizationRepository;

    @Autowired
    protected MemberRepository memberRepository;

    @Autowired
    protected MemberRoleRepository memberRoleRepository;

    @Autowired
    protected OrganizationRoleRepository organizationRoleRepository;

    @Autowired
    protected MemberContextRepository memberContextRepository;

    @Transactional
    @BeforeEach
    public void setUp() throws Exception {
        invitationRepository.deleteAll();
        memberContextRepository.deleteAll();
        organizationRepository.deleteAll();
        var members = memberRepository.findAll();
        members.forEach(it -> it.getRoles().clear());
        memberRepository.saveAll(members);
        memberRepository.deleteAll();
        memberRoleRepository.deleteAll();
        organizationRoleRepository.deleteAll();
    }

    @BeforeAll
    static void beforeAll() {
        TestPropertyValues.of(
                "spring.datasource.url=" + postgres.getJdbcUrl(),
                "spring.datasource.username=" + postgres.getUsername(),
                "spring.datasource.password=" + postgres.getPassword(),
                "spring.flyway.url=" + postgres.getJdbcUrl(),
                "spring.flyway.user=" + postgres.getUsername(),
                "spring.flyway.password=" + postgres.getPassword());

    }

    @SuppressWarnings("rawtypes")
    static PostgreSQLContainer postgres = new PostgreSQLContainer<>(DockerImageName
            .parse(POSTGRESQL_IMAGE_NAME)
            .withTag(POSTGRESQL_VERSION));

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }
}
