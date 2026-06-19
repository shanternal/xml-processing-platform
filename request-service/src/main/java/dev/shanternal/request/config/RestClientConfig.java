package dev.shanternal.request.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient conversionRestClient(
            RestClient.Builder builder,
            @Value("${clients.conversion.base-url}") String baseUrl,
            @Value("${clients.conversion.connect-timeout}") Duration connectTimeout,
            @Value("${clients.conversion.read-timeout}") Duration readTimeout) {

        ClientHttpRequestFactorySettings settings =
                ClientHttpRequestFactorySettings.defaults()
                        .withConnectTimeout(connectTimeout)
                        .withReadTimeout(readTimeout);

        return builder
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
    }
}