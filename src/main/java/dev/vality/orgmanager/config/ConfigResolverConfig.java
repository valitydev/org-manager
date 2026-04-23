package dev.vality.orgmanager.config;

import com.google.common.base.Strings;
import dev.vality.orgmanager.config.properties.KeyCloakProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@ConditionalOnProperty(value = "auth.enabled", havingValue = "true")
public class ConfigResolverConfig {

    @Bean
    public JwtDecoder jwtDecoder(KeyCloakProperties keyCloakProperties) {
        RSAPublicKey publicKey = readPublicKey(keycloakRealmPublicKey(keyCloakProperties));
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey(publicKey).build();

        String issuer = String.format(
                "%s/realms/%s",
                keyCloakProperties.getAuthServerUrl(),
                keyCloakProperties.getRealm()
        );
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withIssuer);
        jwtDecoder.setJwtValidator(validator);
        return jwtDecoder;
    }

    private String keycloakRealmPublicKey(KeyCloakProperties keyCloakProperties) {
        if (!Strings.isNullOrEmpty(keyCloakProperties.getRealmPublicKeyFilePath())) {
            return readKeyFromFile(keyCloakProperties.getRealmPublicKeyFilePath());
        }
        return keyCloakProperties.getRealmPublicKey();
    }

    private RSAPublicKey readPublicKey(String publicKey) {
        String normalizedPublicKey = publicKey.replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(normalizedPublicKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("Unable to parse keycloak realm public key", ex);
        }
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
