package com.skillup.staff.service;

import com.skillup.classmanagement.domain.ClassSession;
import com.skillup.classmanagement.repository.ClassSessionRepository;
import com.skillup.common.exception.BusinessException;
import com.skillup.common.exception.ResourceNotFoundException;
import com.skillup.staff.domain.Staff;
import com.skillup.staff.repository.StaffRepository;
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
 * Resolves the currently authenticated staff member and enforces access control
 * on teacher-portal endpoints.
 *
 * <p><b>Local dev bypass:</b> Pass {@code X-Staff-ID: <staff-uuid>} with
 * {@code skillup.portal.local-dev-bypass=true} to skip JWT validation.
 *
 * <p><b>Production:</b> Extracts JWT {@code sub} (Keycloak user UUID) and looks
 * up the linked {@link Staff} record.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaffContextService {

    private static final String BYPASS_HEADER = "X-Staff-ID";

    private final StaffRepository        staffRepository;
    private final ClassSessionRepository sessionRepository;

    @Value("${skillup.portal.local-dev-bypass:false}")
    private boolean localDevBypass;

    // ── Identity resolution ───────────────────────────────────────────────────

    /**
     * Resolves the staff member for the current request.
     *
     * @throws BusinessException STAFF_UNAUTHENTICATED if no valid identity is present
     * @throws BusinessException STAFF_NOT_FOUND       if the identity is not linked to a staff record
     */
    @Transactional(readOnly = true)
    public Staff getCurrentStaff(HttpServletRequest request) {
        if (localDevBypass) {
            String headerVal = request.getHeader(BYPASS_HEADER);
            if (StringUtils.hasText(headerVal)) {
                try {
                    UUID staffId = UUID.fromString(headerVal);
                    return staffRepository.findActiveById(staffId)
                            .orElseThrow(() -> new BusinessException("STAFF_NOT_FOUND",
                                    "No active staff record found for ID: " + staffId));
                } catch (IllegalArgumentException e) {
                    throw new BusinessException("INVALID_STAFF_ID",
                            "Invalid UUID in " + BYPASS_HEADER + " header: " + headerVal);
                }
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new BusinessException("STAFF_UNAUTHENTICATED",
                    "Teacher portal authentication required.");
        }

        String sub = jwt.getSubject();
        if (!StringUtils.hasText(sub)) {
            throw new BusinessException("STAFF_UNAUTHENTICATED", "JWT is missing sub claim");
        }

        UUID keycloakId;
        try {
            keycloakId = UUID.fromString(sub);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("STAFF_UNAUTHENTICATED", "JWT sub is not a valid UUID");
        }

        return staffRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new BusinessException("STAFF_NOT_FOUND",
                        "No staff account is linked to this identity. Contact the centre administrator."));
    }

    // ── Access control ────────────────────────────────────────────────────────

    /**
     * Loads the session and asserts the teacher is assigned to its template.
     *
     * @return the resolved session (access guaranteed)
     * @throws BusinessException TEACHER_ACCESS_DENIED if the session belongs to a different teacher
     */
    @Transactional(readOnly = true)
    public ClassSession requireSessionAccess(HttpServletRequest request, UUID sessionId) {
        Staff staff = getCurrentStaff(request);

        ClassSession session = sessionRepository.findActiveById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("ClassSession", sessionId));

        Staff assignedTeacher = session.getTemplate().getTeacher();
        if (assignedTeacher == null || !assignedTeacher.getId().equals(staff.getId())) {
            log.warn("Teacher access denied: staff={} attempted to access session={} (assigned teacher={})",
                    staff.getId(), sessionId,
                    assignedTeacher != null ? assignedTeacher.getId() : "none");
            throw new BusinessException("TEACHER_ACCESS_DENIED",
                    "You are not the assigned teacher for this session");
        }

        return session;
    }
}
