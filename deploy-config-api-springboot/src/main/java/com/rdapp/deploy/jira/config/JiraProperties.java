package com.rdapp.deploy.jira.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jira")
@Getter @Setter
public class JiraProperties {

    private String baseUrl = "https://jira.rdapp.com";

    /** Type d'authentification : "pat" (recommandé) ou "basic" */
    private String authType = "pat";

    /** Personal Access Token (Jira DC 8.14+) */
    private String patToken;

    /** Basic Auth fallback */
    private String username;
    private String password;

    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 15000;
    private int maxRetries = 3;
    private int retryDelayMs = 1000;
    private int cacheTtlMinutes = 5;

    // ── Helpers ──

    public String getAgileBaseUrl() {
        return baseUrl + "/rest/agile/1.0";
    }

    public String getApiBaseUrl() {
        return baseUrl + "/rest/api/2";
    }

    public boolean isConfigured() {
        if ("pat".equalsIgnoreCase(authType)) {
            return patToken != null && !patToken.isBlank();
        }
        return username != null && !username.isBlank();
    }
}
