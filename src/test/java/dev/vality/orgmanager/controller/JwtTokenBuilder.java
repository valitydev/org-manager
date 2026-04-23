package dev.vality.orgmanager.controller;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.PrivateKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JwtTokenBuilder {

    public static final String DEFAULT_USERNAME = "Darth Vader";

    public static final String DEFAULT_EMAIL = "darkside-the-best@mail.com";

    private final String userId;

    private final String username;

    private final String email;

    private final PrivateKey privateKey;

    public JwtTokenBuilder(PrivateKey privateKey) {
        this(UUID.randomUUID().toString(), DEFAULT_USERNAME, DEFAULT_EMAIL, privateKey);
    }

    public JwtTokenBuilder(String userId, String username, String email, PrivateKey privateKey) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.privateKey = privateKey;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String generateJwtWithRoles(String issuer, String... roles) {
        long iat = Instant.now().getEpochSecond();
        long exp = iat + 60 * 10;
        return generateJwtWithRoles(iat, exp, issuer, roles);
    }

    public String generateJwtWithRoles(long iat, long exp, String issuer, String... roles) {
        try {
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .jwtID(UUID.randomUUID().toString())
                    .expirationTime(java.util.Date.from(Instant.ofEpochSecond(exp)))
                    .notBeforeTime(java.util.Date.from(Instant.EPOCH))
                    .issueTime(java.util.Date.from(Instant.ofEpochSecond(iat)))
                    .issuer(issuer)
                    .audience("private-api")
                    .subject(userId)
                    .claim("typ", "Bearer")
                    .claim("azp", "private-api")
                    .claim("resource_access", Map.of("common-api", Map.of("roles", List.of(roles))))
                    .claim("preferred_username", username)
                    .claim("email", email)
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(),
                    claimsSet
            );
            signedJWT.sign(new RSASSASigner(privateKey));
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Unable to generate JWT for test", e);
        }
    }

}
