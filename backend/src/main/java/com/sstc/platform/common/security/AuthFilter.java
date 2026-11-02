package com.sstc.platform.common.security;

import com.sstc.platform.identity.domain.AccessAudit;
import com.sstc.platform.identity.repo.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * S01 - the {@code Access Decision Engine} + {@code Access Audit Recorder} of the
 * Authorization Service (US04).
 *
 * <p>Every {@code /api/**} call is authenticated against the session table, the decision is
 * published into the request scope and the outcome is recorded in {@code usr_access_audit}.</p>
 */
@Component
@Order(10)
public class AuthFilter extends OncePerRequestFilter {

    private static final String[] PUBLIC_PREFIXES = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/health",
            "/api/meta",
            "/api/public",
            "/h2-console",
            "/v3/api-docs",
            "/swagger-ui",
            "/actuator"
    };

    private final TokenService tokenService;

    private final AccessAuditsRepo accessAudits;

    public AuthFilter(TokenService tokenService, AccessAuditsRepo accessAudits) {
        this.tokenService = tokenService;
        this.accessAudits = accessAudits;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        long started = System.currentTimeMillis();
        String token = bearerToken(request);
        Optional<CurrentUser> current = tokenService.resolve(token);

        if (current.isPresent()) {
            CurrentUserHolder.set(current.get());
        }

        boolean isApi = path.startsWith("/api/");
        boolean isPublic = isPublic(path);
        try {
            if (isApi && !isPublic && !current.isPresent()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"status\":401,\"code\":\"UNAUTHORIZED\","
                        + "\"message\":\"Authentication required. Sign in through /api/auth/login.\"}");
                recordAudit(request, null, "DENY", "missing or invalid session token", started);
                return;
            }
            chain.doFilter(request, response);
            if (isApi && current.isPresent()) {
                recordAudit(request, current.get(), response.getStatus() < 400 ? "ALLOW" : "DENY",
                        response.getStatus() < 400 ? "permission granted" : "business rule rejected the call",
                        started);
            }
        } finally {
            CurrentUserHolder.clear();
        }
    }

    private boolean isPublic(String path) {
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private String bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return header.substring(7).trim();
        }
        String header2 = request.getHeader("X-STC-Token");
        if (header2 != null && !header2.isEmpty()) {
            return header2.trim();
        }
        return request.getParameter("token");
    }

    private void recordAudit(HttpServletRequest request, CurrentUser user, String decision,
                             String reason, long started) {
        try {
            AccessAudit audit = new AccessAudit(
                    user == null ? null : user.getId(),
                    user == null ? null : user.getUsername(),
                    permissionOf(request),
                    request.getMethod() + " " + request.getRequestURI(),
                    request.getMethod(),
                    decision,
                    reason,
                    System.currentTimeMillis() - started);
            accessAudits.save(audit);
        } catch (RuntimeException ignored) {
            // auditing must never break the request
        }
    }

    /**
     * Maps an endpoint to the permission that protects it. This keeps the audit trail
     * readable: every record answers "which permission was checked?".
     */
    private String permissionOf(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (path.startsWith("/api/identity")) {
            return "S01.IDENTITY.ACCESS";
        }
        if (path.startsWith("/api/users") || path.startsWith("/api/roles")
                || path.startsWith("/api/permissions") || path.startsWith("/api/policies")) {
            return "GET".equals(method) ? "S01.ACCOUNT.VIEW" : "S01.ACCOUNT.MANAGE";
        }
        if (path.startsWith("/api/access-audits") || path.startsWith("/api/account-events")) {
            return "S01.AUDIT.VIEW";
        }
        if (path.startsWith("/api/tasks") || path.startsWith("/api/cycles")) {
            return "GET".equals(method) ? "S02.TASK.VIEW" : "S02.TASK.MANAGE";
        }
        if (path.startsWith("/api/files") || path.startsWith("/api/file-versions")) {
            return "GET".equals(method) ? "S02.FILE.VIEW" : "S02.FILE.MANAGE";
        }
        if (path.startsWith("/api/okrs")) {
            return "S02.OKR.VIEW";
        }
        if (path.startsWith("/api/performance")) {
            return "S02.PERFORMANCE.VIEW";
        }
        if (path.startsWith("/api/operations") || path.startsWith("/api/audits")) {
            return "S02.AUDIT.VIEW";
        }
        if (path.startsWith("/api/channels") || path.startsWith("/api/messages")
                || path.startsWith("/api/meetings")) {
            return "S03.COLLABORATION.USE";
        }
        if (path.startsWith("/api/notifications") || path.startsWith("/api/digest")) {
            return "S03.NOTIFICATION.USE";
        }
        if (path.startsWith("/api/toolbox/documents") || path.startsWith("/api/toolbox/presentations")) {
            return "S04.OFFICE.USE";
        }
        if (path.startsWith("/api/toolbox/repos") || path.startsWith("/api/toolbox/environments")) {
            return "S04.DEVTOOLS.USE";
        }
        if (path.startsWith("/api/toolbox/files") || path.startsWith("/api/toolbox/links")) {
            return "S04.FILE.USE";
        }
        return "PLATFORM.ACCESS";
    }
}
