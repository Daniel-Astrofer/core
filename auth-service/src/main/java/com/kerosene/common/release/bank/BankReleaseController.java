package com.kerosene.common.release.bank;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class BankReleaseController {
    private final BankReleaseService service;
    public BankReleaseController(BankReleaseService service) { this.service = service; }
    @GetMapping(com.kerosene.common.security.EndpointPolicyRegistry.BANK_RELEASE_OBSERVATION)
    public ObjectNode observation(@RequestParam MultiValueMap<String, String> query) {
        if (!query.keySet().equals(Set.of("releaseDigest", "challenge"))
                || query.values().stream().anyMatch(values -> values.size() != 1))
            throw BankReleaseService.error(HttpStatus.BAD_REQUEST, "query_invalid");
        return service.observe(query.getFirst("releaseDigest"), query.getFirst("challenge"));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> failure(ResponseStatusException failure) {
        return ResponseEntity.status(failure.getStatusCode()).header("Cache-Control", "no-store")
                .body(Map.of("code", failure.getReason()));
    }
}
