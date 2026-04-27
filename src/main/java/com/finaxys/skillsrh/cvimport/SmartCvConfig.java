package com.finaxys.skillsrh.cvimport;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(SmartCvProperties.class)
public class SmartCvConfig {

    @Bean("smartCvRestClient")
    RestClient smartCvRestClient(SmartCvProperties properties) {
        RestClient.Builder configuredBuilder = RestClient.builder().baseUrl(properties.getBaseUrl());
        if (StringUtils.hasText(properties.getApiKey())) {
            configuredBuilder.defaultHeader("X-API-Key", properties.getApiKey().trim());
        }
        return configuredBuilder.build();
    }
}
