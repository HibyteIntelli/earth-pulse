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

    @Value("${app.notifier-service.url}")
    private String notifierUrl;

    @Value("${app.notifier-service.internal-secret}")
    private String notifierInternalSecret;

    @Value("${app.notifier-service.connect-timeout:3s}")
    private Duration notifierConnectTimeout;

    @Value("${app.notifier-service.read-timeout:10s}")
    private Duration notifierReadTimeout;

    private RestClient.Builder restClientBuilder(String baseUrl, Duration connectTimeout, Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory);
    }

    @Bean
    public RestClient eonetRestClient() {
        return restClientBuilder(eonetBaseUrl, eonetConnectTimeout, eonetReadTimeout)
                .defaultHeaders(h -> h.setAccept(List.of(MediaType.APPLICATION_JSON)))
                .build();
    }

    @Bean
    public RestClient notifierRestClient() {
        return restClientBuilder(notifierUrl, notifierConnectTimeout, notifierReadTimeout)
                .defaultHeader("X-Internal-Secret", notifierInternalSecret)
                .build();
    }
}

