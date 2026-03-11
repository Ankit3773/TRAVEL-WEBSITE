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

    @Value("${app.frontend.api-base-url:}")
    private String apiBaseUrl;

    @Value("${app.google.client-id:}")
    private String googleClientId;

    @GetMapping(value = "/config.js", produces = "application/javascript")
    public ResponseEntity<String> configJs() {
        String body = "window.NARAYAN_TRAVELS_CONFIG = Object.assign({}, window.NARAYAN_TRAVELS_CONFIG || {}, {"
                + "apiBaseUrl: " + toJsString(normalize(apiBaseUrl)) + ", "
                + "googleClientId: " + toJsString(normalize(googleClientId))
                + "});";

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(new MediaType("application", "javascript", StandardCharsets.UTF_8))
                .body(body);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
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
