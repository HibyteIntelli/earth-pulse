package ro.hibyte.ingestion.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;

@Configuration
public class AppConfig {

    @Value("${eonet.base-url}")
    private String eonetBaseUrl;

    @Bean
    public RestClient eonetRestClient() {
        return RestClient.builder()
                .baseUrl(eonetBaseUrl)
                .defaultHeaders(h -> h.setAccept(List.of(MediaType.APPLICATION_JSON)))
                .build();
    }
}
