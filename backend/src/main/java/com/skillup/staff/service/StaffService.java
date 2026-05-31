package com.skillup.staff.service;

import com.skillup.branch.domain.Branch;
import com.skillup.branch.repository.BranchRepository;
import com.skillup.classmanagement.domain.Subject;
import com.skillup.classmanagement.repository.SubjectRepository;
import com.skillup.common.exception.BusinessException;
import com.skillup.common.exception.ResourceNotFoundException;
import com.skillup.portal.service.KeycloakAdminService;
import com.skillup.staff.domain.Staff;
import com.skillup.staff.domain.StaffRole;
import com.skillup.staff.dto.*;
import com.skillup.staff.repository.StaffRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository       staffRepository;
    private final BranchRepository      branchRepository;
    private final SubjectRepository     subjectRepository;
    private final KeycloakAdminService  keycloakAdminService;
    private final EntityManager         entityManager;

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public StaffResponse create(CreateStaffRequest req) {
        StaffRole role = parseRole(req.getRole(), StaffRole.TEACHER);

        Branch branch = null;
        if (req.getBranchId() != null) {
            branch = branchRepository.findActiveById(req.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", req.getBranchId()));
        } else if (role == StaffRole.TEACHER || role == StaffRole.BRANCH_ADMIN) {
            throw new BusinessException("BRANCH_REQUIRED",
                    "Branch is required for role " + role);
        }

        if (req.getEmail() != null && staffRepository.existsByEmailAndDeletedAtIsNull(req.getEmail())) {
            throw new BusinessException("EMAIL_IN_USE",
                    "A staff account with this email already exists");
        }

        String staffRef = generateStaffRef();

        Staff staff = Staff.builder()
                .staffRef(staffRef)
                .fullName(req.getFullName())
                .icNumber(req.getIcNumber())
                .phone(req.getPhone())
                .email(req.getEmail())
                .whatsappNumber(req.getWhatsappNumber())
                .role(role)
                .branch(branch)
                .active(true)
                .notes(req.getNotes())
                .build();

        if (req.getSubjectIds() != null && !req.getSubjectIds().isEmpty()) {
            staff.setSubjects(resolveSubjects(req.getSubjectIds()));
        }

        Staff saved = staffRepository.save(staff);
        log.info("Staff created: {} {} (role: {})", staffRef, req.getFullName(), role);
        return toResponse(saved);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public StaffResponse findById(UUID id) {
        return toResponse(requireStaff(id));
    }

    @Transactional(readOnly = true)
    public List<StaffSummaryResponse> search(UUID branchId, String role, Boolean active, String name) {
        StaffRole roleEnum = role != null ? parseRole(role, null) : null;
        return staffRepository.search(branchId, roleEnum, active, name)
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public StaffResponse update(UUID id, UpdateStaffRequest req) {
        Staff staff = requireStaff(id);

        if (req.getFullName()       != null) staff.setFullName(req.getFullName());
        if (req.getIcNumber()       != null) staff.setIcNumber(req.getIcNumber());
        if (req.getPhone()          != null) staff.setPhone(req.getPhone());
        if (req.getWhatsappNumber() != null) staff.setWhatsappNumber(req.getWhatsappNumber());
        if (req.getNotes()          != null) staff.setNotes(req.getNotes());
        if (req.getActive()         != null) staff.setActive(req.getActive());

        if (req.getEmail() != null && !req.getEmail().equals(staff.getEmail())) {
            if (staffRepository.existsByEmailAndDeletedAtIsNull(req.getEmail())) {
                throw new BusinessException("EMAIL_IN_USE", "A staff account with this email already exists");
            }
            staff.setEmail(req.getEmail());
        }

        if (req.getRole() != null) {
            staff.setRole(parseRole(req.getRole(), staff.getRole()));
        }

        if (req.getBranchId() != null && (staff.getBranch() == null
                || !req.getBranchId().equals(staff.getBranch().getId()))) {
            Branch branch = branchRepository.findActiveById(req.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", req.getBranchId()));
            staff.setBranch(branch);
        }

        if (req.getSubjectIds() != null) {
            staff.setSubjects(resolveSubjects(req.getSubjectIds()));
        }

        Staff saved = staffRepository.save(staff);
        log.info("Staff updated: {}", id);
        return toResponse(saved);
    }

    // ── Soft delete ───────────────────────────────────────────────────────────

    @Transactional
    public void delete(UUID id) {
        Staff staff = requireStaff(id);
        staff.softDelete();
        staffRepository.save(staff);
        log.info("Staff soft-deleted: {}", id);
    }

    // ── Portal activation ─────────────────────────────────────────────────────

    /**
     * Provisions a Keycloak account for the staff member so they can log in to
     * the teacher portal. Idempotent — safe to call multiple times.
     */
    @Transactional
    public StaffResponse activatePortalAccess(UUID id) {
        Staff staff = requireStaff(id);

        if (staff.getEmail() == null || staff.getEmail().isBlank()) {
            throw new BusinessException("NO_EMAIL",
                    "Cannot activate portal access: staff member has no email address");
        }

        if (!staff.isKeycloakProvisioned()) {
            String keycloakUserId = keycloakAdminService.createUser(staff.getEmail(), staff.getFullName());
            String tempPassword = (staff.getIcNumber() != null && !staff.getIcNumber().isBlank())
                    ? staff.getIcNumber()
                    : UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            keycloakAdminService.setPassword(keycloakUserId, tempPassword, true);
            staff.setKeycloakId(UUID.fromString(keycloakUserId));
            staff.setKeycloakProvisioned(true);
            log.info("Keycloak account provisioned for staff: email={}, keycloakId={}",
                    staff.getEmail(), keycloakUserId);
        }

        staffRepository.save(staff);
        log.info("Portal access activated for staff: {} ({})", staff.getStaffRef(), id);
        return toResponse(staff);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public Staff requireStaff(UUID id) {
        return staffRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", id));
    }

    private Set<Subject> resolveSubjects(List<UUID> subjectIds) {
        Set<Subject> subjects = new HashSet<>();
        for (UUID sid : subjectIds) {
            Subject s = subjectRepository.findActiveById(sid)
                    .orElseThrow(() -> new ResourceNotFoundException("Subject", sid));
            subjects.add(s);
        }
        return subjects;
    }

    private String generateStaffRef() {
        Long seq = ((Number) entityManager
                .createNativeQuery("SELECT nextval('staff_id_seq')")
                .getSingleResult()).longValue();
        return String.format("STF-%d-%04d", Year.now().getValue(), seq);
    }

    private StaffRole parseRole(String value, StaffRole fallback) {
        if (value == null || value.isBlank()) return fallback;
        try { return StaffRole.valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_ROLE",
                    "Invalid role: '" + value + "'. Must be TEACHER, BRANCH_ADMIN, or CENTRE_ADMIN");
        }
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    public StaffResponse toResponse(Staff s) {
        List<StaffResponse.SubjectSummary> subjectList = s.getSubjects().stream()
                .map(sub -> StaffResponse.SubjectSummary.builder()
                        .id(sub.getId())
                        .code(sub.getCode())
                        .name(sub.getName())
                        .build())
                .sorted(Comparator.comparing(StaffResponse.SubjectSummary::getCode))
                .collect(Collectors.toList());

        return StaffResponse.builder()
                .id(s.getId())
                .staffRef(s.getStaffRef())
                .fullName(s.getFullName())
                .icNumber(s.getIcNumber())
                .phone(s.getPhone())
                .email(s.getEmail())
                .whatsappNumber(s.getWhatsappNumber())
                .role(s.getRole().name())
                .branchId(s.getBranch() != null ? s.getBranch().getId() : null)
                .branchName(s.getBranch() != null ? s.getBranch().getName() : null)
                .active(s.isActive())
                .notes(s.getNotes())
                .keycloakProvisioned(s.isKeycloakProvisioned())
                .lastLoginAt(s.getLastLoginAt())
                .subjects(subjectList)
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private StaffSummaryResponse toSummary(Staff s) {
        return StaffSummaryResponse.builder()
                .id(s.getId())
                .staffRef(s.getStaffRef())
                .fullName(s.getFullName())
                .phone(s.getPhone())
                .email(s.getEmail())
                .role(s.getRole().name())
                .branchId(s.getBranch() != null ? s.getBranch().getId() : null)
                .branchName(s.getBranch() != null ? s.getBranch().getName() : null)
                .active(s.isActive())
                .keycloakProvisioned(s.isKeycloakProvisioned())
                .build();
    }
}
