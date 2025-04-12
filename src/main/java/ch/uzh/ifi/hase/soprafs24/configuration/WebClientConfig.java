package ch.uzh.ifi.hase.soprafs24.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean("externalApiClient")
    public WebClient externalApiClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder.baseUrl("https://deckofcardsapi.com/api/deck")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
