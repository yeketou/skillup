package com.skillup.classmanagement.service;

import com.skillup.branch.domain.Branch;
import com.skillup.branch.repository.BranchRepository;
import com.skillup.classmanagement.domain.*;
import com.skillup.classmanagement.dto.*;
import com.skillup.classmanagement.repository.*;
import com.skillup.common.exception.BusinessException;
import com.skillup.common.exception.ResourceNotFoundException;
import com.skillup.student.domain.Student;
import com.skillup.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassManagementService {

    private final ClassTemplateRepository templateRepository;
    private final ClassSessionRepository  sessionRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final SubjectRepository       subjectRepository;
    private final BranchRepository        branchRepository;
    private final StudentRepository       studentRepository;
    private final SubjectService          subjectService;

    // ═══════════════════════════════════════════════════════════════════════════
    // CLASS TEMPLATES
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public ClassTemplateResponse createTemplate(ClassTemplateRequest req) {
        Subject subject = subjectRepository.findActiveById(req.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject", req.getSubjectId()));

        Branch branch = branchRepository.findActiveById(req.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", req.getBranchId()));

        DayOfWeek dayOfWeek = parseDayOfWeek(req.getDayOfWeek());

        if (req.getEndTime() != null && req.getStartTime() != null
                && !req.getEndTime().isAfter(req.getStartTime())) {
            throw new BusinessException("INVALID_TIME_RANGE", "End time must be after start time");
        }

        ClassTemplate template = ClassTemplate.builder()
                .name(req.getName())
                .subject(subject)
                .branch(branch)
                .teacherName(req.getTeacherName())
                .dayOfWeek(dayOfWeek)
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .maxCapacity(req.getMaxCapacity() != null ? req.getMaxCapacity() : 20)
                .feeAmount(req.getFeeAmount() != null ? req.getFeeAmount() : BigDecimal.ZERO)
                .active(req.getActive() == null || req.getActive())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .build();

        ClassTemplate saved = templateRepository.save(template);
        log.info("Class template created: {} ({})", saved.getName(), saved.getDayOfWeek());
        return toTemplateResponse(saved);
    }

    @Transactional(readOnly = true)
    public ClassTemplateResponse getTemplate(UUID id) {
        return toTemplateResponse(requireTemplate(id));
    }

    @Transactional(readOnly = true)
    public List<ClassTemplateResponse> searchTemplates(UUID branchId, UUID subjectId,
                                                       String dayOfWeek, Boolean active) {
        DayOfWeek day = dayOfWeek != null ? parseDayOfWeekOrNull(dayOfWeek) : null;
        return templateRepository.search(branchId, subjectId, day, active)
                .stream()
                .map(this::toTemplateResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClassTemplateResponse updateTemplate(UUID id, ClassTemplateRequest req) {
        ClassTemplate template = requireTemplate(id);

        if (req.getSubjectId() != null && !req.getSubjectId().equals(template.getSubject().getId())) {
            Subject subject = subjectRepository.findActiveById(req.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subject", req.getSubjectId()));
            template.setSubject(subject);
        }
        if (req.getBranchId() != null && !req.getBranchId().equals(template.getBranch().getId())) {
            Branch branch = branchRepository.findActiveById(req.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", req.getBranchId()));
            template.setBranch(branch);
        }

        if (req.getName()        != null) template.setName(req.getName());
        if (req.getTeacherName() != null) template.setTeacherName(req.getTeacherName());
        if (req.getDayOfWeek()   != null) template.setDayOfWeek(parseDayOfWeek(req.getDayOfWeek()));
        if (req.getStartTime()   != null) template.setStartTime(req.getStartTime());
        if (req.getEndTime()     != null) template.setEndTime(req.getEndTime());
        if (req.getMaxCapacity() != null) template.setMaxCapacity(req.getMaxCapacity());
        if (req.getFeeAmount()   != null) template.setFeeAmount(req.getFeeAmount());
        if (req.getActive()      != null) template.setActive(req.getActive());
        if (req.getStartDate()   != null) template.setStartDate(req.getStartDate());
        if (req.getEndDate()     != null) template.setEndDate(req.getEndDate());

        ClassTemplate saved = templateRepository.save(template);
        log.info("Class template updated: {}", id);
        return toTemplateResponse(saved);
    }

    @Transactional
    public void deleteTemplate(UUID id) {
        ClassTemplate template = requireTemplate(id);
        long activeEnrollments = enrollmentRepository
                .countByTemplateIdAndStatusAndDeletedAtIsNull(id, EnrollmentStatus.ACTIVE);
        if (activeEnrollments > 0) {
            throw new BusinessException("CLASS_HAS_ENROLLMENTS",
                    "Cannot delete class with " + activeEnrollments + " active enrolment(s). Drop all students first.");
        }
        template.softDelete();
        templateRepository.save(template);
        log.info("Class template soft-deleted: {}", id);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CLASS SESSIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Generate one ClassSession for each occurrence of the template's dayOfWeek
     * between fromDate and toDate (inclusive). Idempotent — skips dates where a
     * session already exists.
     *
     * @return the list of newly created sessions (excludes skipped dates)
     */
    @Transactional
    public List<ClassSessionResponse> generateSessions(UUID templateId, GenerateSessionsRequest req) {
        ClassTemplate template = requireTemplate(templateId);

        LocalDate from = req.getFromDate();
        LocalDate to   = req.getToDate();

        if (!to.isAfter(from) && !to.equals(from)) {
            throw new BusinessException("INVALID_DATE_RANGE", "To date must be on or after from date");
        }
        if (from.plusYears(1).isBefore(to)) {
            throw new BusinessException("DATE_RANGE_TOO_LARGE", "Cannot generate sessions for more than 1 year at a time");
        }

        List<ClassSession> created = new ArrayList<>();
        LocalDate cursor = from;

        while (!cursor.isAfter(to)) {
            if (cursor.getDayOfWeek() == template.getDayOfWeek()) {
                // Idempotent: skip if session already exists for this date
                boolean exists = sessionRepository
                        .findByTemplateIdAndSessionDateAndDeletedAtIsNull(templateId, cursor)
                        .isPresent();
                if (!exists) {
                    ClassSession session = ClassSession.builder()
                            .template(template)
                            .sessionDate(cursor)
                            .startTime(template.getStartTime())
                            .endTime(template.getEndTime())
                            .teacherName(template.getTeacherName())
                            .status(ClassSessionStatus.SCHEDULED)
                            .build();
                    created.add(sessionRepository.save(session));
                }
            }
            cursor = cursor.plusDays(1);
        }

        log.info("Generated {} sessions for template {} ({} to {})",
                created.size(), templateId, from, to);
        return created.stream().map(this::toSessionResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClassSessionResponse> getSessions(UUID templateId, LocalDate from, LocalDate to) {
        requireTemplate(templateId); // validate template exists
        List<ClassSession> sessions = (from != null && to != null)
                ? sessionRepository.findByTemplateIdAndDateRange(templateId, from, to)
                : sessionRepository.findByTemplateId(templateId);
        return sessions.stream().map(this::toSessionResponse).collect(Collectors.toList());
    }

    @Transactional
    public ClassSessionResponse updateSessionStatus(UUID sessionId, SessionStatusRequest req) {
        ClassSession session = sessionRepository.findActiveById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("ClassSession", sessionId));

        ClassSessionStatus newStatus;
        try {
            newStatus = ClassSessionStatus.valueOf(req.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_SESSION_STATUS",
                    "Invalid status: " + req.getStatus() + ". Must be SCHEDULED, COMPLETED, or CANCELLED");
        }

        session.setStatus(newStatus);
        if (req.getNotes() != null) session.setNotes(req.getNotes());

        ClassSession saved = sessionRepository.save(session);
        log.info("Session {} status → {}", sessionId, newStatus);
        return toSessionResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ENROLLMENTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public ClassEnrollmentResponse enrollStudent(UUID templateId, ClassEnrollmentRequest req) {
        ClassTemplate template = requireTemplate(templateId);

        Student student = studentRepository.findActiveById(req.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", req.getStudentId()));

        // Check for existing (non-deleted) enrollment
        enrollmentRepository
                .findByStudentIdAndTemplateIdAndDeletedAtIsNull(req.getStudentId(), templateId)
                .ifPresent(existing -> {
                    throw new BusinessException("ALREADY_ENROLLED",
                            "Student " + student.getStudentRef() + " is already enrolled in this class (status: " + existing.getStatus() + ")");
                });

        // Capacity check
        long activeCount = enrollmentRepository
                .countByTemplateIdAndStatusAndDeletedAtIsNull(templateId, EnrollmentStatus.ACTIVE);
        if (activeCount >= template.getMaxCapacity()) {
            throw new BusinessException("CLASS_FULL",
                    "Class is at full capacity (" + template.getMaxCapacity() + " students)");
        }

        ClassEnrollment enrollment = ClassEnrollment.builder()
                .student(student)
                .template(template)
                .enrolledDate(req.getEnrolledDate() != null ? req.getEnrolledDate() : LocalDate.now())
                .status(EnrollmentStatus.ACTIVE)
                .notes(req.getNotes())
                .build();

        ClassEnrollment saved = enrollmentRepository.save(enrollment);
        log.info("Student {} enrolled in class {} ({})", student.getStudentRef(), template.getName(), templateId);
        return toEnrollmentResponse(saved);
    }

    @Transactional
    public ClassEnrollmentResponse dropStudent(UUID templateId, UUID studentId) {
        ClassEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndTemplateIdAndDeletedAtIsNull(studentId, templateId)
                .orElseThrow(() -> new BusinessException("NOT_ENROLLED",
                        "Student is not enrolled in this class"));

        if (enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            throw new BusinessException("ALREADY_DROPPED", "Student has already been dropped from this class");
        }

        enrollment.setStatus(EnrollmentStatus.DROPPED);
        enrollment.setDroppedDate(LocalDate.now());

        ClassEnrollment saved = enrollmentRepository.save(enrollment);
        log.info("Student {} dropped from class {}", studentId, templateId);
        return toEnrollmentResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ClassEnrollmentResponse> getEnrollments(UUID templateId) {
        requireTemplate(templateId);
        return enrollmentRepository.findByTemplateId(templateId)
                .stream()
                .map(this::toEnrollmentResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClassEnrollmentResponse> getStudentTimetable(UUID studentId) {
        return enrollmentRepository.findByStudentId(studentId)
                .stream()
                .map(this::toEnrollmentResponse)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private ClassTemplate requireTemplate(UUID id) {
        return templateRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClassTemplate", id));
    }

    private DayOfWeek parseDayOfWeek(String value) {
        try {
            return DayOfWeek.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_DAY_OF_WEEK",
                    "Invalid day of week: '" + value + "'. Must be MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, or SUNDAY");
        }
    }

    private DayOfWeek parseDayOfWeekOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try { return DayOfWeek.valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private ClassTemplateResponse toTemplateResponse(ClassTemplate t) {
        long enrolled = enrollmentRepository
                .countByTemplateIdAndStatusAndDeletedAtIsNull(t.getId(), EnrollmentStatus.ACTIVE);
        return ClassTemplateResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .subjectId(t.getSubject().getId())
                .subjectCode(t.getSubject().getCode())
                .subjectName(t.getSubject().getName())
                .branchId(t.getBranch().getId())
                .branchName(t.getBranch().getName())
                .teacherName(t.getTeacherName())
                .dayOfWeek(t.getDayOfWeek().name())
                .startTime(t.getStartTime())
                .endTime(t.getEndTime())
                .maxCapacity(t.getMaxCapacity())
                .enrolledCount(enrolled)
                .feeAmount(t.getFeeAmount())
                .active(t.isActive())
                .startDate(t.getStartDate())
                .endDate(t.getEndDate())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private ClassSessionResponse toSessionResponse(ClassSession s) {
        ClassTemplate t = s.getTemplate();
        return ClassSessionResponse.builder()
                .id(s.getId())
                .templateId(t.getId())
                .templateName(t.getName())
                .subjectName(t.getSubject().getName())
                .sessionDate(s.getSessionDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .teacherName(s.getTeacherName() != null ? s.getTeacherName() : t.getTeacherName())
                .status(s.getStatus().name())
                .notes(s.getNotes())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private ClassEnrollmentResponse toEnrollmentResponse(ClassEnrollment e) {
        Student st       = e.getStudent();
        ClassTemplate t  = e.getTemplate();
        return ClassEnrollmentResponse.builder()
                .id(e.getId())
                .studentId(st.getId())
                .studentRef(st.getStudentRef())
                .studentName(st.getFullName())
                .templateId(t.getId())
                .templateName(t.getName())
                .subjectName(t.getSubject().getName())
                .dayOfWeek(t.getDayOfWeek().name())
                .enrolledDate(e.getEnrolledDate())
                .droppedDate(e.getDroppedDate())
                .status(e.getStatus().name())
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
