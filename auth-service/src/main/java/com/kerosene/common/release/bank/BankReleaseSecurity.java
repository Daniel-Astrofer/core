package com.kerosene.common.release.bank;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.HexFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

/** Separate certificate-only route; JWTs, proxy headers and admin tokens cannot authorize it. */
@Configuration
public class BankReleaseSecurity {
    @Bean @Order(1)
    SecurityFilterChain bankReleaseChain(HttpSecurity http, BankReleaseProperties config) throws Exception {
        String route = com.kerosene.common.security.EndpointPolicyRegistry.BANK_RELEASE_OBSERVATION;
        return http.securityMatcher(route, route + "/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(new PeerGate(config), UsernamePasswordAuthenticationFilter.class).build();
    }
    static final class PeerGate extends OncePerRequestFilter {
        private final BankReleaseProperties config;
        PeerGate(BankReleaseProperties config) { this.config = config; }
        @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            String supplied = request.getHeader("X-Request-Id");
            String requestId = supplied != null && supplied.matches("[a-zA-Z0-9._-]{1,128}") ? supplied : java.util.UUID.randomUUID().toString();
            response.setHeader("X-Request-Id", requestId);
            String[] peer = {"unverified"}; boolean[] failed = {true};
            try { check(request, response, chain, peer); failed[0] = false; }
            finally {
                org.slf4j.LoggerFactory.getLogger("audit.bank.release").info(org.slf4j.MarkerFactory.getMarker("AUDIT"),
                        "bank.release.read requestId={} peerSpki={} method={} status={} failed={}",
                        requestId, peer[0], request.getMethod(), response.getStatus(), failed[0]);
            }
        }
        private void check(HttpServletRequest request, HttpServletResponse response, FilterChain chain, String[] peer)
                throws ServletException, IOException {
            response.setHeader("Cache-Control", "no-store");
            if (!config.enabled) { reject(response, 503, "observer_unconfigured"); return; }
            if (!"GET".equals(request.getMethod())) { reject(response, 405, "method_not_allowed"); return; }
            if (request.getQueryString() == null || request.getQueryString().length() > 512) {
                reject(response, 400, "query_invalid"); return;
            }
            try {
                Object attribute = request.getAttribute("jakarta.servlet.request.X509Certificate");
                if (!request.isSecure() || !(attribute instanceof X509Certificate[] peers) || peers.length == 0)
                    throw new IllegalArgumentException("direct client certificate missing");
                peers[0].checkValidity();
                String pin = "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(peers[0].getPublicKey().getEncoded()));
                if (!config.clientSpkiDigests.contains(pin)) throw new IllegalArgumentException("client identity not allowed");
                peer[0] = pin;
            } catch (Exception denied) { reject(response, 403, "client_certificate_required"); return; }
            chain.doFilter(request, response);
        }
        private void reject(HttpServletResponse response, int status, String code) throws IOException {
            response.setStatus(status); response.setContentType("application/json");
            response.getWriter().write("{\"code\":\"" + code + "\"}");
        }
    }
}
