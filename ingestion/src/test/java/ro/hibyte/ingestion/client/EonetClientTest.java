package ro.hibyte.ingestion.client;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ro.hibyte.ingestion.exception.EonetUnavailableException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

class EonetClientTest {

    @Test
    void preservesOriginalExceptionAsCauseWhenEonetFails() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://eonet.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(method(GET)).andRespond(withServerError());

        EonetClient client = new EonetClient(builder.build(), new ObjectMapper());

        EonetUnavailableException ex = assertThrows(EonetUnavailableException.class, () -> client.fetchEvents(1));

        assertThat(ex.getCause())
                .isNotNull()
                .isInstanceOf(RestClientException.class);
    }
}
