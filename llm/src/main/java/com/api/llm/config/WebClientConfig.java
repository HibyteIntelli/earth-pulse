package com.api.llm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient ollamaWebClient(
            WebClient.Builder builder,
            @Value("${ollama.baseUrl}") String llmUrl) {

        return builder
                .baseUrl(llmUrl)
                .build();
    }
}


