package com.rdapp.deploy.jira.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.Base64;

@Configuration
@EnableCaching
public class JiraClientConfig {

    @Bean
    public RestClient jiraRestClient(JiraProperties props) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getConnectTimeoutMs());
        factory.setReadTimeout(props.getReadTimeoutMs());

        var builder = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .requestFactory(factory)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json");

        // Auth header
        if ("pat".equalsIgnoreCase(props.getAuthType()) && props.getPatToken() != null) {
            builder.defaultHeader("Authorization", "Bearer " + props.getPatToken());
        } else if (props.getUsername() != null && props.getPassword() != null) {
            var encoded = Base64.getEncoder().encodeToString(
                    (props.getUsername() + ":" + props.getPassword()).getBytes()
            );
            builder.defaultHeader("Authorization", "Basic " + encoded);
        }

        return builder.build();
    }
}
