package ro.hibyte.notifier.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration"
})
class EmailServiceTest {

    @Autowired
    private EmailService emailService;

    @Test
    void sendEmail_shouldDeliverToMailpit() {
        emailService.sendEmail(
                "test@earth-pulse.local",
                "Mailpit Integration Test",
                "Email trimis din EmailServiceTest."
        );
    }
}
