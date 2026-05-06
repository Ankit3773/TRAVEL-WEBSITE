package com.travel.travelapp.controller;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FrontendConfigController {

    private static final String GOOGLE_CLIENT_ID_PLACEHOLDER = "your-google-web-client-id.apps.googleusercontent.com";

    @Value("${app.frontend.api-base-url:}")
    private String apiBaseUrl;

    @Value("${app.google.client-id:}")
    private String googleClientId;

    @Value("${app.frontend.show-local-admin-hint:false}")
    private boolean showLocalAdminHint;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @GetMapping(value = "/config.js", produces = "application/javascript")
    public ResponseEntity<String> configJs() {
        String body = "window.NARAYAN_TRAVELS_CONFIG = Object.assign({}, window.NARAYAN_TRAVELS_CONFIG || {}, {"
                + "apiBaseUrl: " + toJsString(normalize(apiBaseUrl)) + ", "
                + "googleClientId: " + toJsString(normalizeGoogleClientId(googleClientId)) + ", "
                + "showLocalAdminHint: " + showLocalAdminHint + ", "
                + "localAdminEmail: " + toJsString(showLocalAdminHint ? normalize(adminEmail) : "") + ", "
                + "localAdminPassword: " + toJsString(showLocalAdminHint ? normalize(adminPassword) : "")
                + "});";

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(new MediaType("application", "javascript", StandardCharsets.UTF_8))
                .body(body);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeGoogleClientId(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty() || GOOGLE_CLIENT_ID_PLACEHOLDER.equalsIgnoreCase(normalized)) {
            return "";
        }
        return normalized;
    }

    private String toJsString(String value) {
        return "'" + value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                + "'";
    }
}
