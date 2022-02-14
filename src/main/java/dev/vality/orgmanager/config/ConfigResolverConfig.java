package dev.vality.orgmanager.config;

import com.google.common.base.Strings;
import dev.vality.orgmanager.config.properties.KeyCloakProperties;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.springsecurity.config.KeycloakSpringConfigResolverWrapper;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class ConfigResolverConfig {

    @Bean
    public KeycloakConfigResolver keycloakConfigResolver(KeyCloakProperties keyCloakProperties) {
        return facade -> {
            KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(adapterConfig(keyCloakProperties));
            deployment.setNotBefore(keyCloakProperties.getNotBefore());
            return deployment;
        };
    }

    private AdapterConfig adapterConfig(KeyCloakProperties keyCloakProperties) {
        String keycloakRealmPublicKey;
        if (!Strings.isNullOrEmpty(keyCloakProperties.getRealmPublicKeyFilePath())) {
            keycloakRealmPublicKey = readKeyFromFile(keyCloakProperties.getRealmPublicKeyFilePath());
        } else {
            keycloakRealmPublicKey = keyCloakProperties.getRealmPublicKey();
        }

        AdapterConfig adapterConfig = new AdapterConfig();
        adapterConfig.setRealm(keyCloakProperties.getRealm());
        adapterConfig.setRealmKey(keycloakRealmPublicKey);
        adapterConfig.setResource(keyCloakProperties.getResource());
        adapterConfig.setAuthServerUrl(keyCloakProperties.getAuthServerUrl());
        adapterConfig.setUseResourceRoleMappings(true);
        adapterConfig.setBearerOnly(true);
        adapterConfig.setSslRequired(keyCloakProperties.getSslRequired());
        return adapterConfig;
    }

    private String readKeyFromFile(String filePath) {
        try {
            List<String> strings = Files.readAllLines(Paths.get(filePath));
            strings.remove(strings.size() - 1);
            strings.remove(0);

            return strings.stream().map(String::trim).collect(Collectors.joining());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
