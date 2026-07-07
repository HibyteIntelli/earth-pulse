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
    @SuppressWarnings("resource")
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
        assertThat(response.messages().getFirst().subject()).isEqualTo("Mailpit Integration Test");
        assertThat(response.messages().getFirst().to().getFirst().address()).isEqualTo("test@earth-pulse.local");
    }

    @Test
    void sendHtmlEmail_shouldDeliverHtmlBodyToMailpit() {
        emailService.sendHtmlEmail(
                "html-test@earth-pulse.local",
                "Mailpit HTML Integration Test",
                "<h1>Hello</h1><p>Email trimis din EmailServiceTest.</p>"
        );

        String mailpitApiUrl = "http://" + mailpit.getHost() + ":" + mailpit.getMappedPort(8025);
        var response = RestClient.create()
                .get()
                .uri(mailpitApiUrl + "/api/v1/messages")
                .retrieve()
                .body(MailpitMessages.class);

        assertThat(response).isNotNull();
        assertThat(response.messages()).isNotEmpty();
        String messageId = response.messages().getFirst().id();
        assertThat(response.messages().getFirst().subject()).isEqualTo("Mailpit HTML Integration Test");

        var detail = RestClient.create()
                .get()
                .uri(mailpitApiUrl + "/api/v1/message/" + messageId)
                .retrieve()
                .body(MailpitMessageDetail.class);

        assertThat(detail).isNotNull();
        assertThat(detail.html()).contains("<h1>Hello</h1>");
    }

    private record MailpitMessages(java.util.List<MailpitMessage> messages) {}
    private record MailpitMessage(@JsonProperty("ID") String id, @JsonProperty("Subject") String subject, @JsonProperty("To") java.util.List<MailpitAddress> to) {}
    private record MailpitAddress(@JsonProperty("Address") String address) {}
    private record MailpitMessageDetail(@JsonProperty("HTML") String html) {}
}
