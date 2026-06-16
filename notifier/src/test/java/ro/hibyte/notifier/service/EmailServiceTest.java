package ro.hibyte.notifier.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class EmailServiceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Container
    static GenericContainer<?> mailpit = new GenericContainer<>(DockerImageName.parse("axllent/mailpit:latest"))
            .withExposedPorts(1025, 8025);

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", mailpit::getHost);
        registry.add("spring.mail.port", () -> mailpit.getMappedPort(1025));
    }

    @Autowired
    private EmailService emailService;

    @Test
    void sendEmail_shouldDeliverToMailpit() {
        emailService.sendEmail(
                "test@earth-pulse.local",
                "Mailpit Integration Test",
                "Email trimis din EmailServiceTest."
        );

        String mailpitApiUrl = "http://" + mailpit.getHost() + ":" + mailpit.getMappedPort(8025);
        var response = RestClient.create()
                .get()
                .uri(mailpitApiUrl + "/api/v1/messages")
                .retrieve()
                .body(MailpitMessages.class);

        assertThat(response).isNotNull();
        assertThat(response.messages()).isNotEmpty();
        assertThat(response.messages().get(0).subject()).isEqualTo("Mailpit Integration Test");
        assertThat(response.messages().get(0).to().get(0).address()).isEqualTo("test@earth-pulse.local");
    }

    private record MailpitMessages(java.util.List<MailpitMessage> messages) {}
    private record MailpitMessage(@JsonProperty("Subject") String subject, @JsonProperty("To") java.util.List<MailpitAddress> to) {}
    private record MailpitAddress(@JsonProperty("Address") String address) {}
}
