package ro.hibyte.notifier.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class EmailServiceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static GenericContainer<?> mailpit = new GenericContainer<>(DockerImageName.parse("axllent/mailpit:latest"))
            .withExposedPorts(1025, 8025);

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", mailpit::getHost);
        registry.add("spring.mail.port", () -> mailpit.getMappedPort(1025));
        registry.add("spring.mail.properties.mail.smtp.auth", () -> "false");
        registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");
    }

    @Autowired
    private EmailService emailService;

    private final RestTemplate rest = new RestTemplate();

    @BeforeEach
    void clearMailbox() {
        String api = mailpitApi();
        rest.delete(api + "/messages");
    }

    @Test
    void sendEmail_shouldDeliverToMailpit() {
        emailService.sendEmail(
                "test@earth-pulse.local",
                "Mailpit Integration Test",
                "Email trimis din EmailServiceTest."
        );

        await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> {
                    String messages = rest.getForObject(mailpitApi() + "/messages", String.class);
                    assertThat(messages)
                            .contains("test@earth-pulse.local")
                            .contains("Mailpit Integration Test");
                });
    }

    private String mailpitApi() {
        return "http://%s:%d/api/v1".formatted(mailpit.getHost(), mailpit.getMappedPort(8025));
    }
}
