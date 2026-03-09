package com.glassystem.optics.configuration;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;


@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GhnConfig {
    @Value("${ghn.base-url}") String baseUrl;
    @Value("${ghn.token}") String token;
    @Value("${ghn.shop-id}") String shopId;


    @Bean(name = "ghnRestTemplate")
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
