package com.skillup.portal.service;

import com.skillup.common.exception.BusinessException;
import com.skillup.student.domain.Student;
import com.skillup.student.domain.StudentPortalAccount;
import com.skillup.student.repository.StudentPortalAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Resolves the currently authenticated portal account and guards student data access.
 *
 * <p><b>Local dev bypass:</b> Set {@code skillup.portal.local-dev-bypass=true} and pass
 * an {@code X-Portal-Account-ID} request header to skip JWT validation. Useful for
 * Postman / smoke-testing without a running Keycloak instance.
 *
 * <p><b>Production:</b> Extracts the JWT {@code sub} claim (the Keycloak user UUID) and
 * looks up the linked {@link StudentPortalAccount}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalContextService {

    private static final String BYPASS_HEADER = "X-Portal-Account-ID";

    private final StudentPortalAccountRepository portalAccountRepository;

    @Value("${skillup.portal.local-dev-bypass:false}")
    private boolean localDevBypass;

    // ── Account resolution ─────────────────────────────────────────────────────

    /**
     * Resolves the portal account for the current request.
     * Uses JOIN FETCH to eagerly load the students collection — avoids
     * LazyInitializationException when the caller accesses it outside a JPA session.
     *
     * @throws BusinessException PORTAL_UNAUTHENTICATED if no valid identity is present
     * @throws BusinessException PORTAL_ACCOUNT_NOT_FOUND if the JWT sub is unknown
     */
    @Transactional(readOnly = true)
    public StudentPortalAccount getCurrentAccount(HttpServletRequest request) {
        // ── Local dev bypass ──────────────────────────────────────────────────
        if (localDevBypass) {
            String headerVal = request.getHeader(BYPASS_HEADER);
            if (StringUtils.hasText(headerVal)) {
                try {
                    UUID accountId = UUID.fromString(headerVal);
                    // Use JOIN FETCH so students collection is ready without an open session
                    return portalAccountRepository.findByIdWithStudents(accountId)
                            .orElseThrow(() -> new BusinessException("PORTAL_ACCOUNT_NOT_FOUND",
                                    "Portal account not found: " + accountId));
                } catch (IllegalArgumentException e) {
                    throw new BusinessException("INVALID_ACCOUNT_ID",
                            "Invalid UUID in " + BYPASS_HEADER + " header: " + headerVal);
                }
            }
        }

        // ── JWT resolution (production) ───────────────────────────────────────
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new BusinessException("PORTAL_UNAUTHENTICATED",
                    "Portal authentication required. Log in via POST /portal/auth/login.");
        }

        String sub = jwt.getSubject();
        if (!StringUtils.hasText(sub)) {
            throw new BusinessException("PORTAL_UNAUTHENTICATED", "JWT is missing sub claim");
        }

        UUID keycloakId;
        try {
            keycloakId = UUID.fromString(sub);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("PORTAL_UNAUTHENTICATED", "JWT sub claim is not a valid UUID");
        }

        // Use JOIN FETCH so students collection is ready without an open session
        return portalAccountRepository.findByKeycloakIdWithStudents(keycloakId)
                .orElseThrow(() -> new BusinessException("PORTAL_ACCOUNT_NOT_FOUND",
                        "No portal account is linked to this identity. "
                        + "Please contact the tuition centre to activate your portal access."));
    }

    // ── Access control ─────────────────────────────────────────────────────────

    /**
     * Asserts that the given portal account has access to the requested student.
     * A parent can only view/submit data for their own children.
     *
     * @throws BusinessException PORTAL_ACCESS_DENIED if the student is not linked
     */
    public void assertStudentAccess(StudentPortalAccount account, UUID studentId) {
        boolean hasAccess = account.getStudents().stream()
                .anyMatch(s -> s.getId().equals(studentId) && s.getDeletedAt() == null);
        if (!hasAccess) {
            log.warn("Portal access denied: account={} attempted to access student={}",
                    account.getId(), studentId);
            throw new BusinessException("PORTAL_ACCESS_DENIED",
                    "This portal account does not have access to the requested student");
        }
    }

    /**
     * Combined convenience: resolve account and assert access in one call.
     *
     * @return the resolved portal account (access is guaranteed)
     */
    @Transactional(readOnly = true)
    public StudentPortalAccount requireStudentAccess(HttpServletRequest request, UUID studentId) {
        StudentPortalAccount account = getCurrentAccount(request);
        assertStudentAccess(account, studentId);
        return account;
    }

    /**
     * Returns the first student linked to this account whose ID matches.
     * Call {@link #assertStudentAccess} first to ensure access.
     */
    public Student getStudent(StudentPortalAccount account, UUID studentId) {
        return account.getStudents().stream()
                .filter(s -> s.getId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("PORTAL_ACCESS_DENIED",
                        "Student not found in this portal account"));
    }
}
