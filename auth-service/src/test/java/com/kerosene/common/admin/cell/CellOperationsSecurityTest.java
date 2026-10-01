package com.kerosene.common.admin.cell;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CellOperationsController.class)
@ContextConfiguration(classes = {CellOperationsController.class, CellOperationsSecurityTest.Config.class})
class CellOperationsSecurityTest {
    @Autowired MockMvc mvc;
    @Configuration @EnableMethodSecurity @Import(CellOperationsAudit.class)
    static class Config {
        @Bean CellOperationsService service() {
            var service = mock(CellOperationsService.class); when(service.snapshot()).thenReturn(Map.of("ready", false)); return service;
        }
        @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
            return http.csrf(c -> c.disable()).authorizeHttpRequests(a -> a.anyRequest().authenticated()).httpBasic(b -> {}).build();
        }
    }
    @Test void anonymousDeniedAndRequestIdAudited() throws Exception {
        mvc.perform(get("/api/admin/operations/cell").header("X-Request-Id", "audit-test"))
                .andExpect(status().isUnauthorized()).andExpect(header().string("X-Request-Id", "audit-test"));
    }
    @Test void userAndAuditorCannotReadOrPlan() throws Exception {
        mvc.perform(get("/api/admin/operations/cell").with(user("reader").roles("AUDITOR"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/operations/cell/updates/plans").with(user("reader").roles("USER"))
                .contentType("application/json").content("{}")) .andExpect(status().isForbidden());
    }
    @Test void operatorCanReadSnapshot() throws Exception {
        mvc.perform(get("/api/admin/operations/cell").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ready").value(false));
        mvc.perform(get("/api/admin/operations/cell").with(user("operator").roles("OPERATOR")))
                .andExpect(header().string("Cache-Control", "no-store"));
    }
}
