package com.earthpulse.www.service;

import com.earthpulse.www.dto.JwkKeyDto;
import com.earthpulse.www.dto.JwksDto;
import com.earthpulse.www.mapper.JwksMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;

    private static final String BASE_URL = "http://localhost:8083";

    @BeforeEach
    void setUp() throws JOSEException, ParseException {
        JwksMapper jwksMapper = new com.earthpulse.www.mapper.JwksMapperImpl();
        jwtService = new JwtService(jwksMapper);
        ReflectionTestUtils.setField(jwtService, "baseUrl", BASE_URL);
        ReflectionTestUtils.setField(jwtService, "privateKeyJwk", "");
        jwtService.init();
    }

    // issueToken

    @Test
    @DisplayName("issueToken: returns a non-blank JWT string")
    void issueToken_returnsNonBlankString() {
        String token = jwtService.issueToken(UUID.randomUUID());

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("issueToken: token contains correct sub, iss, aud claims")
    void issueToken_claimsAreCorrect() throws ParseException, JOSEException {
        UUID userId = UUID.randomUUID();
        String token = jwtService.issueToken(userId);

        JWTClaimsSet claims = jwtService.validateToken(token);

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.getIssuer()).isEqualTo(BASE_URL);
        assertThat(claims.getAudience()).contains("earth-pulse");
    }

    @Test
    @DisplayName("issueToken: token expiry is approximately one hour from now")
    void issueToken_expiryIsAboutOneHour() throws ParseException, JOSEException {
        Instant before = Instant.now();
        String token = jwtService.issueToken(UUID.randomUUID());
        Instant after = Instant.now();

        JWTClaimsSet claims = jwtService.validateToken(token);
        Instant exp = claims.getExpirationTime().toInstant();

        assertThat(exp).isAfter(before.plus(55, ChronoUnit.MINUTES));
        assertThat(exp).isBefore(after.plus(65, ChronoUnit.MINUTES));
    }

    // validateToken

    @Test
    @DisplayName("validateToken: valid token passes without throwing")
    void validateToken_validToken_passes() throws ParseException, JOSEException {
        String token = jwtService.issueToken(UUID.randomUUID());

        JWTClaimsSet claims = jwtService.validateToken(token);

        assertThat(claims).isNotNull();
    }

    @Test
    @DisplayName("validateToken: tampered signature is rejected with JOSEException")
    void validateToken_tamperedSignature_rejected() {
        String token = jwtService.issueToken(UUID.randomUUID());
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + ".invalidsignature";

        assertThatThrownBy(() -> jwtService.validateToken(tampered))
                .isInstanceOf(JOSEException.class);
    }

    @Test
    @DisplayName("validateToken: expired token is rejected")
    void validateToken_expiredToken_rejected() throws JOSEException, ParseException {
        RSAKey rsaKey = (RSAKey) ReflectionTestUtils.getField(jwtService, "rsaKey");
        RSASSASigner signer = new RSASSASigner(rsaKey);

        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(UUID.randomUUID().toString())
                .issuer(BASE_URL)
                .audience("earth-pulse")
                .issueTime(Date.from(now.minus(3, ChronoUnit.HOURS)))
                .expirationTime(Date.from(now.minus(2, ChronoUnit.HOURS)))
                .build();

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
                claims
        );
        jwt.sign(signer);
        String expiredToken = jwt.serialize();

        assertThatThrownBy(() -> jwtService.validateToken(expiredToken))
                .isInstanceOf(JOSEException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("validateToken: wrong issuer is rejected")
    void validateToken_wrongIssuer_rejected() throws JOSEException, ParseException {
        RSAKey rsaKey = (RSAKey) ReflectionTestUtils.getField(jwtService, "rsaKey");
        RSASSASigner signer = new RSASSASigner(rsaKey);

        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(UUID.randomUUID().toString())
                .issuer("http://lala.lala.com")
                .audience("earth-pulse")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .build();

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
                claims
        );
        jwt.sign(signer);

        assertThatThrownBy(() -> jwtService.validateToken(jwt.serialize()))
                .isInstanceOf(JOSEException.class)
                .hasMessageContaining("issuer");
    }

    @Test
    @DisplayName("validateToken: wrong audience is rejected")
    void validateToken_wrongAudience_rejected() throws JOSEException, ParseException {
        RSAKey rsaKey = (RSAKey) ReflectionTestUtils.getField(jwtService, "rsaKey");
        RSASSASigner signer = new RSASSASigner(rsaKey);

        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(UUID.randomUUID().toString())
                .issuer(BASE_URL)
                .audience("wrong-audience")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .build();

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
                claims
        );
        jwt.sign(signer);

        assertThatThrownBy(() -> jwtService.validateToken(jwt.serialize()))
                .isInstanceOf(JOSEException.class)
                .hasMessageContaining("audience");
    }

    @Test
    @DisplayName("validateToken: token signed with a different key is rejected")
    void validateToken_differentKey_rejected() throws JOSEException {
        RSAKey otherKey = new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(UUID.randomUUID().toString())
                .algorithm(JWSAlgorithm.RS256)
                .generate();
        RSASSASigner otherSigner = new RSASSASigner(otherKey);

        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(UUID.randomUUID().toString())
                .issuer(BASE_URL)
                .audience("earth-pulse")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .build();

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(otherKey.getKeyID()).build(),
                claims
        );
        jwt.sign(otherSigner);

        assertThatThrownBy(() -> jwtService.validateToken(jwt.serialize()))
                .isInstanceOf(JOSEException.class);
    }

    @Test
    @DisplayName("validateToken: alg:none token (unsigned) is rejected")
    void validateToken_algNone_rejected() {
        String header = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes());
        String payload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"sub\":\"" + UUID.randomUUID() + "\","
                        + "\"iss\":\"" + BASE_URL + "\","
                        + "\"aud\":\"earth-pulse\","
                        + "\"exp\":" + (Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond())
                        + "}").getBytes());
        String algNoneToken = header + "." + payload + ".";

        assertThatThrownBy(() -> jwtService.validateToken(algNoneToken))
                .isInstanceOf(ParseException.class);
    }

    // getJwks

    @Test
    @DisplayName("getJwks: returns a JwksDto with at least one key")
    void getJwks_returnsAtLeastOneKey() {
        JwksDto jwks = jwtService.getJwks();

        assertThat(jwks).isNotNull();
        assertThat(jwks.keys()).isNotEmpty();
    }

    @Test
    @DisplayName("getJwks: first key has kty=RSA, use=sig, and a non-blank kid")
    void getJwks_keyHasCorrectFields() {
        JwkKeyDto key = jwtService.getJwks().keys().getFirst();

        assertThat(key.kty()).isEqualTo("RSA");
        assertThat(key.use()).isEqualTo("sig");
        assertThat(key.kid()).isNotBlank();
        assertThat(key.alg()).isEqualTo("RS256");
        assertThat(key.n()).isNotBlank();
        assertThat(key.e()).isNotBlank();
    }

    @Test
    @DisplayName("getJwks: JWKS public key successfully verifies a token issued by the service")
    void getJwks_publicKeyValidatesIssuedToken() throws ParseException, JOSEException {
        UUID userId = UUID.randomUUID();
        String token = jwtService.issueToken(userId);

        JwkKeyDto keyDto = jwtService.getJwks().keys().getFirst();
        com.nimbusds.jose.jwk.RSAKey publicKey = com.nimbusds.jose.jwk.RSAKey.parse(
                "{\"kty\":\"" + keyDto.kty() + "\","
                + "\"use\":\"" + keyDto.use() + "\","
                + "\"kid\":\"" + keyDto.kid() + "\","
                + "\"alg\":\"" + keyDto.alg() + "\","
                + "\"n\":\"" + keyDto.n() + "\","
                + "\"e\":\"" + keyDto.e() + "\"}"
        );

        SignedJWT signed = SignedJWT.parse(token);
        boolean valid = signed.verify(new com.nimbusds.jose.crypto.RSASSAVerifier(publicKey));

        assertThat(valid).isTrue();
    }
}
