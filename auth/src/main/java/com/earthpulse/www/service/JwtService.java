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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.earthpulse.www.dto.JwkKeyDto;
import com.earthpulse.www.dto.JwksDto;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private static final int TOKEN_EXPIRY_HOURS = 1;

    @Value("${app.base-url}")
    private String baseUrl;

    private RSAKey rsaKey;
    private RSASSASigner signer;

    @PostConstruct
    public void init() throws JOSEException {
        rsaKey = new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(UUID.randomUUID().toString())
                .algorithm(JWSAlgorithm.RS256)
                .generate();
        signer = new RSASSASigner(rsaKey);
    }

    public String issueToken(UUID userId) throws JOSEException {
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
    }

    public JwksDto getJwks() {
        RSAKey pub = rsaKey.toPublicJWK();
        JwkKeyDto key = new JwkKeyDto(
                pub.getKeyType().getValue(),
                pub.getKeyUse().identifier(),
                pub.getKeyID(),
                pub.getAlgorithm().getName(),
                pub.getModulus().toString(),
                pub.getPublicExponent().toString()
        );
        return new JwksDto(List.of(key));
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
