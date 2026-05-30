package com.skillup.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Resolves the current branch for every incoming request.
 *
 * Resolution order:
 *   1. JWT claim  "branch_id"   (set by Keycloak session attribute)
 *   2. Header     "X-Branch-ID" (for admin API calls or service-to-service)
 *
 * Sets BranchContext and MDC "branchId" for log correlation.
 * Always clears both in a finally block.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BranchResolutionFilter extends OncePerRequestFilter {

    private static final String BRANCH_HEADER = "X-Branch-ID";
    private static final String JWT_CLAIM     = "branch_id";
    private static final String MDC_KEY       = "branchId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            UUID branchId = resolveFromJwt();
            if (branchId == null) {
                branchId = resolveFromHeader(request);
            }

            if (branchId != null) {
                BranchContext.set(branchId);
                MDC.put(MDC_KEY, branchId.toString());
                log.debug("Branch resolved: {}", branchId);
            }

            filterChain.doFilter(request, response);

        } finally {
            BranchContext.clear();
            MDC.remove(MDC_KEY);
        }
    }

    private UUID resolveFromJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }
        String claim = jwt.getClaimAsString(JWT_CLAIM);
        if (!StringUtils.hasText(claim)) {
            return null;
        }
        try {
            return UUID.fromString(claim);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid branch_id claim in JWT: {}", claim);
            return null;
        }
    }

    private UUID resolveFromHeader(HttpServletRequest request) {
        String header = request.getHeader(BRANCH_HEADER);
        if (!StringUtils.hasText(header)) {
            return null;
        }
        try {
            return UUID.fromString(header);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid {} header value: {}", BRANCH_HEADER, header);
            return null;
        }
    }
}
