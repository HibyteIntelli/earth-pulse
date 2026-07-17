package ro.hibyte.ingestion.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ro.hibyte.ingestion.dto.eonet.EonetResponse;
import ro.hibyte.ingestion.exception.EonetUnavailableException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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

    @Test
    void parsesWellFormedEventsResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://eonet.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        String body = """
                {"events":[
                  {"id":"EONET_1","title":"Wildfire"},
                  {"id":"EONET_2","title":"Storm"}
                ]}
                """;
        server.expect(method(GET)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        EonetClient client = new EonetClient(builder.build(), new ObjectMapper());
        EonetResponse response = client.fetchEvents(1);

        assertThat(response.getEvents()).hasSize(2);
        assertThat(response.getEvents().get(0).getId()).isEqualTo("EONET_1");
        assertThat(response.getEvents().get(0).getTitle()).isEqualTo("Wildfire");
    }

    @Test
    void throwsWhenBodyIsEmpty() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://eonet.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(method(GET)).andRespond(withSuccess());
        EonetClient client = new EonetClient(builder.build(), new ObjectMapper());
        EonetUnavailableException ex = assertThrows(EonetUnavailableException.class, () -> client.fetchEvents(1));

        assertThat(ex.getMessage()).contains("empty response body");
    }

    @Test
    void returnsEmptyWhenEventsKeyMissing() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://eonet.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        String body = """
                {"title":"no events array at all"}
                """;

        server.expect(method(GET)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        EonetClient client = new EonetClient(builder.build(), new ObjectMapper());
        EonetResponse response = client.fetchEvents(1);

        assertThat(response.getEvents()).isEmpty();
    }

    @Test
    void returnsEmptyWhenEventsArrayEmpty() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://eonet.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        String body = """
                {"events":[]}
                """;

        server.expect(method(GET)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        EonetClient client = new EonetClient(builder.build(), new ObjectMapper());
        EonetResponse response = client.fetchEvents(1);

        assertThat(response.getEvents()).isEmpty();
    }

    @Test
    void keepsBracesInsideStringValues() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://eonet.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        String body = """
                {"events":[{"id":"EONET_1","title":"Fire near {block} zone"}]}
                """;

        server.expect(method(GET)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        EonetClient client = new EonetClient(builder.build(), new ObjectMapper());
        EonetResponse response = client.fetchEvents(1);

        assertThat(response.getEvents()).hasSize(1);
        assertThat(response.getEvents().get(0).getId()).isEqualTo("EONET_1");
        assertThat(response.getEvents().get(0).getTitle()).isEqualTo("Fire near {block} zone");
    }

    @Test
    void handlesEscapedQuotesInStrings() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://eonet.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        String body = """
                {"events":[{"id":"EONET_1","title":"He said \\"fire\\""}]}
                """;

        server.expect(method(GET)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        EonetClient client = new EonetClient(builder.build(), new ObjectMapper());
        EonetResponse response = client.fetchEvents(1);

        assertThat(response.getEvents()).hasSize(1);
        assertThat(response.getEvents().get(0).getId()).isEqualTo("EONET_1");
        assertThat(response.getEvents().get(0).getTitle()).isEqualTo("He said \"fire\"");

    }

    @Test
    void splitsEventWithNestedObjects() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://eonet.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        String body = """
                {"events":[{"id":"EONET_1","title":"Quake","geometry":[{"coordinates":[10,20]}]}]}
                """;

        server.expect(method(GET)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        EonetClient client = new EonetClient(builder.build(), new ObjectMapper());
        EonetResponse response = client.fetchEvents(1);

        assertThat(response.getEvents()).hasSize(1);
        assertThat(response.getEvents().get(0).getId()).isEqualTo("EONET_1");
    }

    @Test
    void skipsMalformedEventButKeepsValid() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://eonet.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        String body = """
                {"events":[
                  {"id":"EONET_1","title":"Valid"},
                  {"id":"EONET_2","title":"Broken","closed":"not-a-date"}
                ]}
                """;

        server.expect(method(GET)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        EonetClient client = new EonetClient(builder.build(), new ObjectMapper());
        EonetResponse response = client.fetchEvents(1);

        assertThat(response.getEvents()).hasSize(1);
        assertThat(response.getEvents().get(0).getId()).isEqualTo("EONET_1");
    }

}
