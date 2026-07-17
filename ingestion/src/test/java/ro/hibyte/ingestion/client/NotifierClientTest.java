package ro.hibyte.ingestion.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ro.hibyte.ingestion.dto.notifier.NewEventPayloadDto;
import ro.hibyte.ingestion.support.EventTestData;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class NotifierClientTest {

    private static NewEventPayloadDto payload() {
        return new NewEventPayloadDto(EventTestData.anEvent().geometry(10.0, 20.0).build());
    }

    @Test
    void doesNotRetryOn4xx() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://notifier.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NotifierClient client = new NotifierClient(builder.build(), 3, 0L, 2);

        server.expect(method(POST)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        client.notifyNewEvent(payload());

        server.verify();
    }

    @Test
    void retriesServerErrorUpToMaxAttempts() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://notifier.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NotifierClient client = new NotifierClient(builder.build(), 3, 0L, 2);

        server.expect(ExpectedCount.times(3), method(POST)).andRespond(withServerError());

        client.notifyNewEvent(payload());

        server.verify();
    }

    @Test
    void succeedsOnSecondAttempt() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://notifier.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NotifierClient client = new NotifierClient(builder.build(), 3, 0L, 2);

        server.expect(method(POST)).andRespond(withServerError());
        server.expect(method(POST)).andRespond(withSuccess());

        client.notifyNewEvent(payload());

        server.verify();
    }
}
