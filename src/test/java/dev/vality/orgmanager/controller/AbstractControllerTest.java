package dev.vality.orgmanager.controller;

import dev.vality.orgmanager.repository.AbstractRepositoryTest;
import dev.vality.orgmanager.service.ResourceAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import tools.jackson.databind.json.JsonMapper;

@Import(KeycloakTestConfig.class)
@AutoConfigureMockMvc
@EnableWireMock(@ConfigureWireMock(port = 0, baseUrlProperties = "wiremock.server.baseUrl"))
@TestPropertySource(
        locations = "classpath:wiremock.properties",
        properties = "auth.enabled=true"
)
public abstract class AbstractControllerTest extends AbstractRepositoryTest {

    @MockitoSpyBean
    protected ResourceAccessService resourceAccessService;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        keycloakOpenIdStub.givenStub();
    }

    @Autowired
    protected KeycloakOpenIdStub keycloakOpenIdStub;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JsonMapper objectMapper;

    protected String generateJwt(long iat, long exp, String... roles) {
        return keycloakOpenIdStub.generateJwt(iat, exp, roles);
    }

    protected String generateAdminJwt() {
        return keycloakOpenIdStub.generateJwt("admin");
    }

    protected String getUserFromToken() {
        return keycloakOpenIdStub.getUserId();
    }

}
