package com.earthpulse.www.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.earthpulse.www.dto.JwksDto;
import com.earthpulse.www.mapper.JwksMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.EnumSet;
import java.util.UUID;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final int TOKEN_EXPIRY_HOURS = 1;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${APP_JWT_PRIVATE_KEY:}")
    private String privateKeyJwk;

    private final JwksMapper jwksMapper;

    private RSAKey rsaKey;
    private RSASSASigner signer;

    public JwtService(JwksMapper jwksMapper) {
        this.jwksMapper = jwksMapper;
    }

    @PostConstruct
    public void init() throws JOSEException, ParseException {
        if (privateKeyJwk != null && !privateKeyJwk.isBlank()) {
            rsaKey = RSAKey.parse(privateKeyJwk);
            log.info("RSA signing key loaded from configuration.");
        } else {
            rsaKey = new RSAKeyGenerator(2048)
                    .keyUse(KeyUse.SIGNATURE)
                    .keyID(UUID.randomUUID().toString())
                    .algorithm(JWSAlgorithm.RS256)
                    .generate();
            writeGeneratedKeyToFile(rsaKey.toJSONString());
            log.warn("APP_JWT_PRIVATE_KEY not set — ephemeral RSA key generated. " +
                     "Copy the value from generated-jwk.json into your .env, then delete the file.");
        }
        signer = new RSASSASigner(rsaKey);
    }

    private void writeGeneratedKeyToFile(String jwk) {
        Path out = Path.of("generated-jwk.json");
        try {
            Files.deleteIfExists(out);
            try {
                Files.createFile(out, PosixFilePermissions.asFileAttribute(
                        EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)));
            } catch (UnsupportedOperationException e) {
                Files.createFile(out);
                log.warn("Non-POSIX filesystem: generated-jwk.json has default permissions. " +
                         "Set APP_JWT_PRIVATE_KEY in your .env to avoid writing the key to disk.");
            }
            Files.writeString(out, jwk);
        } catch (IOException e) {
            throw new IllegalStateException("Could not write generated JWK to file", e);
        }
    }

    public String issueToken(UUID userId) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(userId.toString())
                    .issuer(baseUrl)
                    .audience("earth-pulse")
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plus(TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS)))
                    .build();

            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
                    claims
            );
            jwt.sign(signer);
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Token signing failed", e);
        }
    }

    public JwksDto getJwks() {
        return jwksMapper.toDto(rsaKey);
    }

    public JWTClaimsSet validateToken(String token) throws ParseException, JOSEException {
        SignedJWT signed = SignedJWT.parse(token);

        if (!signed.verify(new RSASSAVerifier(rsaKey.toPublicJWK()))) {
            throw new JOSEException("Invalid JWT signature");
        }

        JWTClaimsSet claims = signed.getJWTClaimsSet();

        if (claims.getExpirationTime() == null || claims.getExpirationTime().before(new Date())) {
            throw new JOSEException("JWT has expired");
        }
        if (!baseUrl.equals(claims.getIssuer())) {
            throw new JOSEException("Invalid JWT issuer");
        }
        if (!claims.getAudience().contains("earth-pulse")) {
            throw new JOSEException("Invalid JWT audience");
        }

        return claims;
    }
}
