package ro.hibyte.ingestion.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

@Configuration
public class AppConfig {

    @Value("${eonet.base-url}")
    private String eonetBaseUrl;

    @Value("${eonet.connect-timeout:5s}")
    private Duration eonetConnectTimeout;

    @Value("${eonet.read-timeout:10s}")
    private Duration eonetReadTimeout;

    @Bean
    public RestClient eonetRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(eonetConnectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(eonetReadTimeout);

        return RestClient.builder()
                .baseUrl(eonetBaseUrl)
                .requestFactory(requestFactory)
                .defaultHeaders(h -> h.setAccept(List.of(MediaType.APPLICATION_JSON)))
                .build();
    }
}

