package com.travel.travelapp.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.travel.travelapp.exception.BadRequestException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GoogleIdentityService {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Value("${app.google.client-id:}")
    private String googleClientId;

    public GoogleProfile verifyCredential(String credential) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new BadRequestException("Google sign-in is not configured");
        }

        try {
            NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, JSON_FACTORY)
                    .setAudience(List.of(googleClientId.trim()))
                    .build();

            GoogleIdToken idToken = verifier.verify(credential);
            if (idToken == null) {
                throw new BadRequestException("Invalid Google credential");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new BadRequestException("Google account email is not verified");
            }

            String email = payload.getEmail();
            if (email == null || email.isBlank()) {
                throw new BadRequestException("Google account did not return an email");
            }

            String name = payload.get("name") instanceof String value ? value.trim() : "";
            return new GoogleProfile(payload.getSubject(), email.trim().toLowerCase(), name);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid Google credential");
        } catch (GeneralSecurityException | IOException ex) {
            throw new IllegalStateException("Unable to verify Google credential", ex);
        }
    }

    public record GoogleProfile(String subject, String email, String name) {
    }
}
