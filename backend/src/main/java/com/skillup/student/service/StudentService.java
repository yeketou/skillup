package com.skillup.student.service;

import com.skillup.branch.domain.Branch;
import com.skillup.branch.repository.BranchRepository;
import com.skillup.common.exception.BusinessException;
import com.skillup.common.exception.ResourceNotFoundException;
import com.skillup.communication.service.NotificationService;
import com.skillup.portal.service.KeycloakAdminService;
import com.skillup.student.domain.*;
import com.skillup.student.dto.*;
import com.skillup.student.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository              studentRepository;
    private final StudentParentRepository        parentRepository;
    private final StudentPortalAccountRepository portalAccountRepository;
    private final BranchRepository               branchRepository;
    private final EntityManager                  entityManager;
    private final KeycloakAdminService           keycloakAdminService;
    private final NotificationService            notificationService;

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public StudentResponse create(CreateStudentRequest req) {
        Branch branch = branchRepository.findActiveById(req.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", req.getBranchId()));

        // Validate at least one primary parent
        boolean hasPrimary = req.getParents().stream().anyMatch(ParentRequest::isPrimary);
        if (!hasPrimary) {
            throw new BusinessException("NO_PRIMARY_PARENT",
                    "At least one parent/guardian must be marked as primary");
        }

        String studentRef = generateStudentRef();

        Student student = Student.builder()
                .studentRef(studentRef)
                .fullName(req.getFullName())
                .preferredName(req.getPreferredName())
                .icNumber(req.getIcNumber())
                .dateOfBirth(req.getDateOfBirth())
                .gender(parseEnum(Gender.class, req.getGender(), Gender.UNSPECIFIED))
                .nationality(req.getNationality() != null ? req.getNationality() : "Malaysian")
                .schoolName(req.getSchoolName())
                .currentGrade(req.getCurrentGrade())
                .status(StudentStatus.ACTIVE)
                .branch(branch)
                .enrolledDate(req.getEnrolledDate() != null ? req.getEnrolledDate() : LocalDate.now())
                .notes(req.getNotes())
                .build();

        // Build parents
        List<StudentParent> parents = req.getParents().stream()
                .map(p -> buildParent(student, p))
                .collect(Collectors.toList());
        student.setParents(parents);

        Student saved = studentRepository.save(student);

        // Auto-create portal account for primary parent if email provided
        StudentParent primary = saved.getPrimaryParent();
        if (primary != null && primary.getEmail() != null && !primary.getEmail().isBlank()) {
            ensurePortalAccount(primary.getEmail(), primary.getFullName(), saved);
        }

        // Send WhatsApp welcome message to primary parent
        if (primary != null) {
            try {
                notificationService.sendWelcome(saved, primary);
            } catch (Exception e) {
                log.warn("Failed to send welcome message for student {}: {}", studentRef, e.getMessage());
            }
        }

        log.info("Student created: {} {} (branch: {})", studentRef, req.getFullName(), branch.getName());
        return toResponse(saved);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public StudentResponse findById(UUID id) {
        Student student = studentRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
        return toResponse(student);
    }

    @Transactional(readOnly = true)
    public Page<StudentSummaryResponse> search(StudentSearchRequest req) {
        int page = req.getPage() != null ? req.getPage() : 0;
        int size = req.getSize() != null ? Math.min(req.getSize(), 100) : 20;

        Sort sort = buildSort(req.getSortBy(), req.getSortDir());
        Pageable pageable = PageRequest.of(page, size, sort);

        StudentStatus statusEnum = null;
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            try { statusEnum = StudentStatus.valueOf(req.getStatus().toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }

        Page<Student> results = studentRepository.search(
                req.getName(),
                statusEnum,
                req.getBranchId(),
                req.getCurrentGrade(),
                pageable
        );

        return results.map(this::toSummary);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public StudentResponse update(UUID id, UpdateStudentRequest req) {
        Student student = studentRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));

        if (req.getBranchId() != null && !req.getBranchId().equals(student.getBranch().getId())) {
            Branch branch = branchRepository.findActiveById(req.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", req.getBranchId()));
            student.setBranch(branch);
        }

        student.setFullName(req.getFullName());
        if (req.getPreferredName()  != null) student.setPreferredName(req.getPreferredName());
        if (req.getIcNumber()       != null) student.setIcNumber(req.getIcNumber());
        if (req.getDateOfBirth()    != null) student.setDateOfBirth(req.getDateOfBirth());
        if (req.getGender()         != null) student.setGender(parseEnum(Gender.class, req.getGender(), student.getGender()));
        if (req.getNationality()    != null) student.setNationality(req.getNationality());
        if (req.getSchoolName()     != null) student.setSchoolName(req.getSchoolName());
        if (req.getCurrentGrade()   != null) student.setCurrentGrade(req.getCurrentGrade());
        if (req.getNotes()          != null) student.setNotes(req.getNotes());

        Student saved = studentRepository.save(student);
        log.info("Student updated: {}", id);
        return toResponse(saved);
    }

    @Transactional
    public StudentResponse changeStatus(UUID id, StudentStatusRequest req) {
        Student student = studentRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));

        StudentStatus newStatus;
        try {
            newStatus = StudentStatus.valueOf(req.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_STATUS",
                    "Invalid status: " + req.getStatus() + ". Must be ACTIVE, INACTIVE, SUSPENDED, or GRADUATED");
        }

        StudentStatus oldStatus = student.getStatus();
        student.setStatus(newStatus);

        if (newStatus == StudentStatus.GRADUATED && student.getGraduatedDate() == null) {
            student.setGraduatedDate(LocalDate.now());
        }

        if (req.getReason() != null && !req.getReason().isBlank()) {
            String note = "[" + LocalDate.now() + "] Status changed from " + oldStatus
                    + " to " + newStatus + ": " + req.getReason();
            String existing = student.getNotes();
            student.setNotes(existing != null ? existing + "\n" + note : note);
        }

        Student saved = studentRepository.save(student);
        log.info("Student {} status changed: {} → {}", id, oldStatus, newStatus);
        return toResponse(saved);
    }

    // ── Parents ───────────────────────────────────────────────────────────────

    @Transactional
    public StudentResponse addParent(UUID studentId, ParentRequest req) {
        Student student = studentRepository.findActiveById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        // If new parent is primary, demote existing primary
        if (req.isPrimary()) {
            student.getParents().stream()
                    .filter(p -> p.isPrimary() && p.getDeletedAt() == null)
                    .forEach(p -> p.setPrimary(false));
        }

        StudentParent parent = buildParent(student, req);
        student.getParents().add(parent);

        Student saved = studentRepository.save(student);

        // Update portal account if primary email changed
        if (req.isPrimary() && req.getEmail() != null && !req.getEmail().isBlank()) {
            ensurePortalAccount(req.getEmail(), req.getFullName(), saved);
        }

        return toResponse(saved);
    }

    // ── Soft Delete ───────────────────────────────────────────────────────────

    @Transactional
    public void delete(UUID id) {
        Student student = studentRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
        student.softDelete();
        studentRepository.save(student);
        log.info("Student soft-deleted: {}", id);
    }

    // ── Portal Account ────────────────────────────────────────────────────────

    @Transactional
    public void activatePortalAccount(UUID studentId) {
        Student student = studentRepository.findActiveById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        StudentParent primary = student.getPrimaryParent();
        if (primary == null || primary.getEmail() == null || primary.getEmail().isBlank()) {
            throw new BusinessException("NO_PARENT_EMAIL",
                    "Cannot activate portal account: primary parent has no email address");
        }

        StudentPortalAccount account = ensurePortalAccount(primary.getEmail(), primary.getFullName(), student);

        if (!account.isActive()) {
            account.setActive(true);
        }

        // Provision Keycloak user if not yet done
        if (!account.isKeycloakProvisioned()) {
            String keycloakId = keycloakAdminService.createUser(primary.getEmail(), primary.getFullName());
            // Temporary password: IC number if available, else a UUID prefix the admin will share
            String tempPassword = (primary.getIcNumber() != null && !primary.getIcNumber().isBlank())
                    ? primary.getIcNumber()
                    : java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            keycloakAdminService.setPassword(keycloakId, tempPassword, true);
            account.setKeycloakId(java.util.UUID.fromString(keycloakId));
            log.info("Keycloak account provisioned for portal: email={}, keycloakId={}", primary.getEmail(), keycloakId);
        }

        portalAccountRepository.save(account);
        log.info("Portal account activated for student: {} (email: {})", studentId, primary.getEmail());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private StudentPortalAccount ensurePortalAccount(String email, String parentName, Student student) {
        StudentPortalAccount account = portalAccountRepository
                .findActiveByEmail(email)
                .orElseGet(() -> {
                    StudentPortalAccount newAccount = StudentPortalAccount.builder()
                            .email(email)
                            .fullName(parentName)
                            .active(true)
                            .build();
                    return portalAccountRepository.save(newAccount);
                });

        // Link student to account if not already linked
        boolean alreadyLinked = account.getStudents().stream()
                .anyMatch(s -> s.getId().equals(student.getId()));
        if (!alreadyLinked) {
            account.getStudents().add(student);
            portalAccountRepository.save(account);
        }

        return account;
    }

    /**
     * Generates the next student reference number: STU-YYYY-NNNN
     * Uses a PostgreSQL sequence for uniqueness under concurrent inserts.
     */
    private String generateStudentRef() {
        Long seq = ((Number) entityManager
                .createNativeQuery("SELECT nextval('student_id_seq')")
                .getSingleResult()).longValue();
        return String.format("STU-%d-%04d", Year.now().getValue(), seq);
    }

    private StudentParent buildParent(Student student, ParentRequest req) {
        return StudentParent.builder()
                .student(student)
                .fullName(req.getFullName())
                .icNumber(req.getIcNumber())
                .phone(req.getPhone())
                .email(req.getEmail())
                .whatsappNumber(req.getWhatsappNumber())
                .relationship(parseEnum(ParentRelationship.class, req.getRelationship(), ParentRelationship.PARENT))
                .preferredLanguage(req.getPreferredLanguage() != null ? req.getPreferredLanguage() : "MS")
                .primary(req.isPrimary())
                .build();
    }

    private Sort buildSort(String sortBy, String sortDir) {
        String field = switch (sortBy != null ? sortBy.toLowerCase() : "") {
            case "student_ref", "studentref" -> "studentRef";
            case "enrolled_date", "enrolleddate" -> "enrolledDate";
            case "status" -> "status";
            default -> "fullName";
        };
        Sort.Direction dir = "desc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, field);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, E fallback) {
        if (value == null || value.isBlank()) return fallback;
        try { return Enum.valueOf(type, value.toUpperCase()); }
        catch (IllegalArgumentException e) { return fallback; }
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private StudentResponse toResponse(Student s) {
        List<ParentResponse> parentResponses = s.getParents().stream()
                .filter(p -> p.getDeletedAt() == null)
                .map(this::toParentResponse)
                .collect(Collectors.toList());

        Optional<StudentPortalAccount> portalAccount = s.getPrimaryParent() != null
                && s.getPrimaryParent().getEmail() != null
                ? portalAccountRepository.findActiveByEmail(s.getPrimaryParent().getEmail())
                : Optional.empty();

        return StudentResponse.builder()
                .id(s.getId())
                .studentRef(s.getStudentRef())
                .fullName(s.getFullName())
                .preferredName(s.getPreferredName())
                .icNumber(s.getIcNumber())
                .dateOfBirth(s.getDateOfBirth())
                .gender(s.getGender().name())
                .nationality(s.getNationality())
                .photoUrl(s.getPhotoUrl())
                .schoolName(s.getSchoolName())
                .currentGrade(s.getCurrentGrade())
                .status(s.getStatus().name())
                .branchId(s.getBranch().getId())
                .branchName(s.getBranch().getName())
                .enrolledDate(s.getEnrolledDate())
                .graduatedDate(s.getGraduatedDate())
                .notes(s.getNotes())
                .parents(parentResponses)
                .portalAccountExists(portalAccount.isPresent())
                .portalAccountActive(portalAccount.map(StudentPortalAccount::isActive).orElse(false))
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .createdBy(s.getCreatedBy())
                .build();
    }

    private StudentSummaryResponse toSummary(Student s) {
        StudentParent primary = s.getPrimaryParent();
        return StudentSummaryResponse.builder()
                .id(s.getId())
                .studentRef(s.getStudentRef())
                .fullName(s.getFullName())
                .preferredName(s.getPreferredName())
                .gender(s.getGender().name())
                .currentGrade(s.getCurrentGrade())
                .status(s.getStatus().name())
                .branchId(s.getBranch().getId())
                .branchName(s.getBranch().getName())
                .enrolledDate(s.getEnrolledDate())
                .photoUrl(s.getPhotoUrl())
                .primaryParentName(primary != null ? primary.getFullName() : null)
                .primaryParentPhone(primary != null ? primary.getPhone() : null)
                .build();
    }

    private ParentResponse toParentResponse(StudentParent p) {
        return ParentResponse.builder()
                .id(p.getId())
                .fullName(p.getFullName())
                .icNumber(p.getIcNumber())
                .phone(p.getPhone())
                .email(p.getEmail())
                .whatsappNumber(p.getWhatsappNumber())
                .relationship(p.getRelationship().name())
                .preferredLanguage(p.getPreferredLanguage())
                .primary(p.isPrimary())
                .build();
    }
}
