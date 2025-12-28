package com.ilyassan.albaraka.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/oauth")
@Slf4j
public class OAuthController {

    @GetMapping("/success")
    public ResponseEntity<?> oauthSuccess(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken token) {
            log.info("OAuth2 Login Success for user: {}", token.getPrincipal().getName());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Successfully authenticated with Keycloak");
            response.put("principal", token.getPrincipal().getName());
            response.put("authorities", token.getAuthorities());
            response.put("attributes", token.getPrincipal().getAttributes());

            return ResponseEntity.ok(response);
        }

        return ResponseEntity.badRequest().body("Invalid authentication");
    }

    @GetMapping("/user-info")
    public ResponseEntity<?> getUserInfo(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken token) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("name", token.getPrincipal().getAttribute("name"));
            userInfo.put("email", token.getPrincipal().getAttribute("email"));
            userInfo.put("preferred_username", token.getPrincipal().getAttribute("preferred_username"));
            userInfo.put("roles", token.getPrincipal().getAttribute("resource_access"));

            return ResponseEntity.ok(userInfo);
        }

        return ResponseEntity.badRequest().body("Not authenticated with OAuth2");
    }

    @GetMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }
}
