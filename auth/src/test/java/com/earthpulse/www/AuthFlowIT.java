package com.earthpulse.www;

import com.earthpulse.www.dto.JwkKeyDto;
import com.earthpulse.www.dto.JwksDto;
import com.earthpulse.www.service.BannedPasswordService;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthFlowIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BannedPasswordService bannedPasswordService;

    @Test
    @DisplayName("POST /auth/signup: valid payload returns 201 Created with empty body")
    void signup_happyPath_returns201() throws Exception {
        String body = "{\"email\":\"newuser@example.com\",\"password\":\"securePass1\",\"name\":\"New User\"}";

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /auth/signup: duplicate email returns 409 Conflict")
    void signup_duplicateEmail_returns409() throws Exception {
        String body = "{\"email\":\"duplicate@example.com\",\"password\":\"securePass1\",\"name\":\"Duplicate User\"}";

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    @DisplayName("POST /auth/signup: missing email returns 400 with validation error")
    void signup_missingEmail_returns400() throws Exception {
        String body = "{\"password\":\"securePass1\"}";

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    @DisplayName("POST /auth/signup: invalid email format returns 400 with validation error")
    void signup_invalidEmailFormat_returns400() throws Exception {
        String body = "{\"email\":\"not-an-email\",\"password\":\"securePass1\"}";

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    @DisplayName("POST /auth/signup: password shorter than 8 characters returns 400 with validation error")
    void signup_shortPassword_returns400() throws Exception {
        String body = "{\"email\":\"shortpass@example.com\",\"password\":\"short\"}";

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    @DisplayName("POST /auth/signup: blank password returns 400 with validation error")
    void signup_blankPassword_returns400() throws Exception {
        String body = "{\"email\":\"blankpass@example.com\",\"password\":\"\"}";

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    @DisplayName("POST /auth/signup: empty request body returns 400")
    void signup_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/signup: both email and password empty returns 400 with validation errors on both fields")
    void signup_bothFieldsEmpty_returns400() throws Exception {
        String body = "{\"email\":\"\",\"password\":\"\"}";

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    @DisplayName("POST /auth/signup: password present in banned list returns 400")
    void signup_bannedPassword_returns400() throws Exception {
        Mockito.doReturn(true).when(bannedPasswordService).isBanned("Banned1pass");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"banned@example.com\",\"password\":\"Banned1pass\",\"name\":\"Banned User\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password is too common. Please choose a more unique password."));

        Mockito.reset(bannedPasswordService);
    }

    @Test
    @DisplayName("POST /auth/login: both email and password empty returns 400 with validation errors on both fields")
    void login_bothFieldsEmpty_returns400() throws Exception {
        String body = "{\"email\":\"\",\"password\":\"\"}";

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    @DisplayName("POST /auth/login: valid credentials return 200 with a JWT token")
    void login_happyPath_returnsJwt() throws Exception {
        String signupBody = "{\"email\":\"loginuser@example.com\",\"password\":\"loginPass1\",\"name\":\"Login User\"}";
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody))
                .andExpect(status().isCreated());

        String loginBody = "{\"email\":\"loginuser@example.com\",\"password\":\"loginPass1\"}";
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseJson).get("token").asString();

        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("POST /auth/login: wrong password returns 401 Unauthorized")
    void login_wrongPassword_returns401() throws Exception {
        String signupBody = "{\"email\":\"wrongpass@example.com\",\"password\":\"correctPass1\",\"name\":\"Wrong Pass User\"}";
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody))
                .andExpect(status().isCreated());

        String loginBody = "{\"email\":\"wrongpass@example.com\",\"password\":\"wrongPassword\"}";
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("POST /auth/login: unknown email returns 401 Unauthorized")
    void login_unknownEmail_returns401() throws Exception {
        String loginBody = "{\"email\":\"ghost@example.com\",\"password\":\"somePassword1\"}";
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("POST /auth/login: missing password field returns 400")
    void login_missingPassword_returns400() throws Exception {
        String loginBody = "{\"email\":\"someone@example.com\"}";
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    @DisplayName("POST /auth/login: invalid email format returns 400")
    void login_invalidEmailFormat_returns400() throws Exception {
        String loginBody = "{\"email\":\"not-valid\",\"password\":\"somePassword\"}";
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    @DisplayName("GET /.well-known/jwks.json: returns 200 with a JSON object containing a 'keys' array")
    void jwks_returns200WithKeysArray() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("GET /.well-known/jwks.json: first key has kty=RSA, use=sig, non-blank kid and alg=RS256")
    void jwks_firstKeyHasCorrectFields() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].use").value("sig"))
                .andExpect(jsonPath("$.keys[0].kid").isNotEmpty())
                .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
                .andExpect(jsonPath("$.keys[0].n").isNotEmpty())
                .andExpect(jsonPath("$.keys[0].e").isNotEmpty());
    }

    @Test
    @DisplayName("GET /.well-known/jwks.json: endpoint is publicly accessible without any auth header")
    void jwks_isPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /auth/login: missing email field returns 400")
    void login_missingEmail_returns400() throws Exception {
        String loginBody = "{\"password\":\"somePassword1\"}";
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    @DisplayName("POST /auth/signup: missing password field (key absent) returns 400")
    void signup_missingPassword_returns400() throws Exception {
        String body = "{\"email\":\"missingpass@example.com\"}";
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    @DisplayName("POST /auth/signup: null field values return 400 with validation errors on both fields")
    void signup_nullFields_returns400() throws Exception {
        String body = "{\"email\":null,\"password\":null}";
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    @DisplayName("POST /auth/signup: wrong Content-Type (text/plain) returns 415 Unsupported Media Type")
    void signup_wrongContentType_returns415() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("email=foo@example.com&password=secret1"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("GET /.well-known/jwks.json: response must not contain any private key fields")
    void jwks_doesNotContainPrivateKeyFields() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].d").doesNotExist())
                .andExpect(jsonPath("$.keys[0].p").doesNotExist())
                .andExpect(jsonPath("$.keys[0].q").doesNotExist())
                .andExpect(jsonPath("$.keys[0].dp").doesNotExist())
                .andExpect(jsonPath("$.keys[0].dq").doesNotExist())
                .andExpect(jsonPath("$.keys[0].qi").doesNotExist());
    }

    @Test
    @DisplayName("GET /internal/**: missing X-Internal-Secret returns 401")
    void internal_missingSecret_returns401() throws Exception {
        mockMvc.perform(get("/internal/health"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /internal/**: wrong X-Internal-Secret returns 401")
    void internal_wrongSecret_returns401() throws Exception {
        mockMvc.perform(get("/internal/health")
                        .header("X-Internal-Secret", "totally-wrong-secret"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /internal/**: correct X-Internal-Secret passes the filter (not 401)")
    void internal_correctSecret_passesFilter() throws Exception {
        mockMvc.perform(get("/internal/health")
                        .header("X-Internal-Secret", "test-internal-secret-32-chars-long!!"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    @DisplayName("Full round-trip: iat claim is present and within a few seconds of now")
    void fullJwtRoundTrip_iatIsPresentAndCloseToNow() throws Exception {
        String email = "iat-check@example.com";
        String password = "iatCheckPass1";
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"name\":\"Iat Check\"}"))
                .andExpect(status().isCreated());

        Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Instant after = Instant.now().plusSeconds(1);

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asString();
        SignedJWT signed = SignedJWT.parse(token);
        JWTClaimsSet claims = signed.getJWTClaimsSet();

        assertThat(claims.getIssueTime()).isNotNull();
        Instant iat = claims.getIssueTime().toInstant();
        assertThat(iat).isAfterOrEqualTo(before);
        assertThat(iat).isBeforeOrEqualTo(after);
    }

    @Test
    @DisplayName("Full round-trip: sub claim is a valid UUID")
    void fullJwtRoundTrip_subIsValidUuid() throws Exception {
        String email = "uuid-check@example.com";
        String password = "uuidCheckPass1";
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"name\":\"UUID Check\"}"))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asString();
        SignedJWT signed = SignedJWT.parse(token);
        String sub = signed.getJWTClaimsSet().getSubject();

        assertThat(sub).isNotBlank();
        UUID parsed = UUID.fromString(sub);
        assertThat(parsed).isNotNull();
    }

    @Test
    @DisplayName("Full round-trip: signup, login, get token, verify signature via JWKS")
    void fullJwtRoundTrip_signupLoginVerify() throws Exception {
        String email = "roundtrip@example.com";
        String password = "roundTripPass1";
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"name\":\"Round Trip\"}"))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asString();
        assertThat(token).isNotBlank();

        MvcResult jwksResult = mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andReturn();

        JwksDto jwksDto = objectMapper.readValue(jwksResult.getResponse().getContentAsString(), JwksDto.class);
        assertThat(jwksDto.keys()).isNotEmpty();

        JwkKeyDto keyDto = jwksDto.keys().getFirst();

        RSAKey publicKey = RSAKey.parse(
                "{\"kty\":\"" + keyDto.kty() + "\","
                + "\"use\":\"" + keyDto.use() + "\","
                + "\"kid\":\"" + keyDto.kid() + "\","
                + "\"alg\":\"" + keyDto.alg() + "\","
                + "\"n\":\"" + keyDto.n() + "\","
                + "\"e\":\"" + keyDto.e() + "\"}"
        );

        SignedJWT signed = SignedJWT.parse(token);
        boolean signatureValid = signed.verify(new RSASSAVerifier(publicKey));
        assertThat(signatureValid).isTrue();

        JWTClaimsSet claims = signed.getJWTClaimsSet();
        assertThat(claims.getIssuer()).isEqualTo("http://localhost:8083");
        assertThat(claims.getAudience()).contains("earth-pulse");
        assertThat(claims.getSubject()).isNotBlank();
        assertThat(claims.getExpirationTime()).isAfter(new java.util.Date());
    }
}
