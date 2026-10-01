package com.kerosene.common.admin.cell;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Audit all attempts, including authorization denials; never logs bodies or credentials. */
@Component
@Order(-200)
public class CellOperationsAudit extends OncePerRequestFilter implements WebMvcConfigurer {
    public static final String REQUEST_ID = "cell.audit.requestId";
    private static final String ACTOR = "cell.audit.actor";
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !(path.equals("/api/admin/operations/cell") || path.startsWith("/api/admin/operations/cell/"));
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String supplied = request.getHeader("X-Request-Id");
        String id = supplied != null && supplied.matches("[a-zA-Z0-9._-]{1,128}") ? supplied : UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID, id); response.setHeader("X-Request-Id", id);
        response.setHeader("Cache-Control", "no-store");
        boolean failed = true;
        try { chain.doFilter(request, response); failed = false; }
        finally {
            LoggerFactory.getLogger("audit.cell.operations").info(MarkerFactory.getMarker("AUDIT"),
                    "cell.operation requestId={} actor={} method={} operation={} status={} failed={}", id,
                    request.getAttribute(ACTOR) == null ? "anonymous" : request.getAttribute(ACTOR),
                    request.getMethod(), operation(request.getRequestURI()), response.getStatus(), failed);
        }
    }
    private String operation(String path) {
        if (path.endsWith("/updates/plans")) return "UPDATE_PLANS";
        if (path.contains("/updates/plans/")) return "UPDATE_PLAN_INSPECT";
        for (String section : java.util.List.of("releases", "quorum", "blockers", "backups", "updates")) if (path.endsWith("/" + section)) return section.toUpperCase(java.util.Locale.ROOT);
        return "SNAPSHOT";
    }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                var principal = request.getUserPrincipal();
                if (principal != null) request.setAttribute(ACTOR, principal.getName().replaceAll("[^a-zA-Z0-9@._-]", "_").substring(0, Math.min(128, principal.getName().length())));
                return true;
            }
        }).addPathPatterns("/api/admin/operations/cell", "/api/admin/operations/cell/**");
    }
}
