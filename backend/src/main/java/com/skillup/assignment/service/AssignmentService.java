package com.skillup.assignment.service;

import com.skillup.assignment.domain.*;
import com.skillup.assignment.dto.*;
import com.skillup.assignment.repository.AssignmentRepository;
import com.skillup.assignment.repository.AssignmentSubmissionRepository;
import com.skillup.assignment.repository.SubmissionAttachmentRepository;
import com.skillup.classmanagement.domain.ClassEnrollment;
import com.skillup.classmanagement.domain.ClassSession;
import com.skillup.classmanagement.domain.ClassTemplate;
import com.skillup.classmanagement.domain.EnrollmentStatus;
import com.skillup.classmanagement.repository.ClassEnrollmentRepository;
import com.skillup.classmanagement.repository.ClassSessionRepository;
import com.skillup.classmanagement.repository.ClassTemplateRepository;
import com.skillup.common.exception.BusinessException;
import com.skillup.common.exception.ResourceNotFoundException;
import com.skillup.student.domain.Student;
import com.skillup.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository            assignmentRepository;
    private final AssignmentSubmissionRepository  submissionRepository;
    private final SubmissionAttachmentRepository  attachmentRepository;
    private final ClassTemplateRepository         templateRepository;
    private final ClassSessionRepository          sessionRepository;
    private final ClassEnrollmentRepository       enrollmentRepository;
    private final StudentRepository               studentRepository;

    // ═══════════════════════════════════════════════════════════════════════════
    // ASSIGNMENT CRUD
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public AssignmentResponse createAssignment(CreateAssignmentRequest req) {
        ClassTemplate template = templateRepository.findActiveById(req.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("ClassTemplate", req.getTemplateId()));

        ClassSession session = null;
        if (req.getSessionId() != null) {
            session = sessionRepository.findActiveById(req.getSessionId())
                    .orElseThrow(() -> new ResourceNotFoundException("ClassSession", req.getSessionId()));
            if (!session.getTemplate().getId().equals(template.getId())) {
                throw new BusinessException("SESSION_TEMPLATE_MISMATCH",
                        "The specified session does not belong to the given class template");
            }
        }

        AssignmentType type = parseType(req.getType());

        if (req.getMaxScore() != null && req.getMaxScore().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_MAX_SCORE", "Max score must be greater than zero");
        }

        Assignment assignment = Assignment.builder()
                .template(template)
                .session(session)
                .title(req.getTitle())
                .description(req.getDescription())
                .type(type)
                .dueDate(req.getDueDate())
                .maxScore(req.getMaxScore())
                .allowLate(req.isAllowLate())
                .build();

        Assignment saved = assignmentRepository.save(assignment);
        log.info("Assignment created: '{}' (template={}, type={}, due={})",
                saved.getTitle(), template.getName(), type, saved.getDueDate());
        return toAssignmentResponse(saved, false);
    }

    @Transactional
    public AssignmentResponse updateAssignment(UUID id, UpdateAssignmentRequest req) {
        Assignment assignment = requireAssignment(id);

        if (assignment.getStatus() != AssignmentStatus.DRAFT) {
            throw new BusinessException("ASSIGNMENT_NOT_DRAFT",
                    "Only DRAFT assignments can be edited. Current status: " + assignment.getStatus());
        }

        if (req.getSessionId() != null) {
            ClassSession session = sessionRepository.findActiveById(req.getSessionId())
                    .orElseThrow(() -> new ResourceNotFoundException("ClassSession", req.getSessionId()));
            if (!session.getTemplate().getId().equals(assignment.getTemplate().getId())) {
                throw new BusinessException("SESSION_TEMPLATE_MISMATCH",
                        "The specified session does not belong to this assignment's class template");
            }
            assignment.setSession(session);
        }

        if (req.getTitle() != null)       assignment.setTitle(req.getTitle());
        if (req.getDescription() != null) assignment.setDescription(req.getDescription());
        if (req.getType() != null)        assignment.setType(parseType(req.getType()));
        if (req.getDueDate() != null)     assignment.setDueDate(req.getDueDate());
        if (req.getAllowLate() != null)   assignment.setAllowLate(req.getAllowLate());

        if (req.getMaxScore() != null) {
            if (req.getMaxScore().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("INVALID_MAX_SCORE", "Max score must be greater than zero");
            }
            assignment.setMaxScore(req.getMaxScore());
        }

        Assignment saved = assignmentRepository.save(assignment);
        log.info("Assignment updated: {} ('{}')", id, saved.getTitle());
        return toAssignmentResponse(saved, false);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LIFECYCLE: PUBLISH / CLOSE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Publishes a DRAFT assignment and auto-creates a PENDING submission for every
     * ACTIVE student enrolled in the class at the time of publishing.
     * Idempotent — skips students who already have a submission record.
     */
    @Transactional
    public AssignmentResponse publishAssignment(UUID id) {
        Assignment assignment = requireAssignment(id);

        if (assignment.getStatus() != AssignmentStatus.DRAFT) {
            throw new BusinessException("ASSIGNMENT_NOT_DRAFT",
                    "Only DRAFT assignments can be published. Current status: " + assignment.getStatus());
        }

        assignment.setStatus(AssignmentStatus.PUBLISHED);
        assignmentRepository.save(assignment);

        // Create PENDING submissions for all currently ACTIVE students
        List<ClassEnrollment> enrollments = enrollmentRepository
                .findByTemplateId(assignment.getTemplate().getId())
                .stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .collect(Collectors.toList());

        int created = 0, skipped = 0;
        for (ClassEnrollment enrollment : enrollments) {
            UUID studentId = enrollment.getStudent().getId();
            if (submissionRepository.existsByAssignmentIdAndStudentIdAndDeletedAtIsNull(id, studentId)) {
                skipped++;
                continue;
            }
            AssignmentSubmission sub = AssignmentSubmission.builder()
                    .assignment(assignment)
                    .student(enrollment.getStudent())
                    .status(SubmissionStatus.PENDING)
                    .build();
            submissionRepository.save(sub);
            created++;
        }

        log.info("Assignment '{}' published — created {} submission slots ({} skipped)",
                assignment.getTitle(), created, skipped);
        return toAssignmentResponse(assignment, true);
    }

    /**
     * Closes a PUBLISHED assignment — no new submissions accepted after this.
     * Existing unsubmitted (PENDING) slots remain for reporting; teacher can
     * individually EXCUSE students via gradeSubmission if needed.
     */
    @Transactional
    public AssignmentResponse closeAssignment(UUID id) {
        Assignment assignment = requireAssignment(id);

        if (assignment.getStatus() != AssignmentStatus.PUBLISHED) {
            throw new BusinessException("ASSIGNMENT_NOT_PUBLISHED",
                    "Only PUBLISHED assignments can be closed. Current status: " + assignment.getStatus());
        }

        assignment.setStatus(AssignmentStatus.CLOSED);
        Assignment saved = assignmentRepository.save(assignment);
        log.info("Assignment '{}' closed ({})", assignment.getTitle(), id);
        return toAssignmentResponse(saved, true);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ASSIGNMENT READ
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public AssignmentResponse getAssignment(UUID id) {
        return toAssignmentResponse(requireAssignment(id), true);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignmentsForTemplate(UUID templateId, String statusFilter) {
        templateRepository.findActiveById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("ClassTemplate", templateId));

        AssignmentStatus status = parseStatusOrNull(statusFilter);
        return assignmentRepository.findByTemplate(templateId, status)
                .stream()
                .map(a -> toAssignmentResponse(a, true))
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SUBMISSIONS — TEACHER VIEW
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissions(UUID assignmentId) {
        requireAssignment(assignmentId);
        return submissionRepository.findByAssignment(assignmentId)
                .stream()
                .map(this::toSubmissionResponse)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SUBMIT — STUDENT ACTION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Records a student's submission.
     * <ul>
     *   <li>Assignment must be PUBLISHED.</li>
     *   <li>If the student already has a PENDING slot it is updated; otherwise a new submission is created.</li>
     *   <li>Submissions after dueDate: rejected unless allowLate=true, in which case status=LATE.</li>
     * </ul>
     */
    @Transactional
    public SubmissionResponse submitAssignment(UUID assignmentId, SubmitAssignmentRequest req) {
        Assignment assignment = requireAssignment(assignmentId);

        if (assignment.getStatus() == AssignmentStatus.DRAFT) {
            throw new BusinessException("ASSIGNMENT_NOT_PUBLISHED",
                    "This assignment has not been published yet");
        }
        if (assignment.getStatus() == AssignmentStatus.CLOSED) {
            throw new BusinessException("ASSIGNMENT_CLOSED",
                    "This assignment is closed and no longer accepting submissions");
        }

        Student student = studentRepository.findActiveById(req.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", req.getStudentId()));

        AssignmentSubmission submission = submissionRepository
                .findByAssignmentAndStudent(assignmentId, req.getStudentId())
                .orElseGet(() -> {
                    // Student enrolled after publish — create the slot now
                    AssignmentSubmission newSub = AssignmentSubmission.builder()
                            .assignment(assignment)
                            .student(student)
                            .status(SubmissionStatus.PENDING)
                            .build();
                    return submissionRepository.save(newSub);
                });

        // Guard: cannot re-submit if already graded
        if (submission.getStatus() == SubmissionStatus.GRADED) {
            throw new BusinessException("ALREADY_GRADED",
                    "This submission has already been graded and cannot be re-submitted");
        }
        if (submission.getStatus() == SubmissionStatus.EXCUSED) {
            throw new BusinessException("EXCUSED",
                    "This student has been excused from this assignment");
        }

        // Determine status based on timing
        OffsetDateTime now = OffsetDateTime.now();
        SubmissionStatus newStatus;
        if (now.isAfter(assignment.getDueDate())) {
            if (!assignment.isAllowLate()) {
                throw new BusinessException("PAST_DUE",
                        String.format("The due date for this assignment was %s. Late submissions are not allowed.",
                                assignment.getDueDate()));
            }
            newStatus = SubmissionStatus.LATE;
        } else {
            newStatus = SubmissionStatus.SUBMITTED;
        }

        submission.setContent(req.getContent());
        submission.setSubmittedAt(now);
        submission.setStatus(newStatus);

        // Process attachments — append to existing (allow multiple submit calls before grading)
        if (req.getAttachments() != null && !req.getAttachments().isEmpty()) {
            List<SubmissionAttachment> attachments = req.getAttachments().stream()
                    .map(a -> SubmissionAttachment.builder()
                            .submission(submission)
                            .fileKey(a.getFileKey())
                            .fileName(a.getFileName())
                            .fileSize(a.getFileSize())
                            .contentType(a.getContentType())
                            .build())
                    .collect(Collectors.toList());
            submission.getAttachments().addAll(attachments);
        }

        AssignmentSubmission saved = submissionRepository.save(submission);
        log.info("Submission {} ({}) for assignment '{}' — status={}",
                saved.getId(), student.getStudentRef(), assignment.getTitle(), newStatus);
        return toSubmissionResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GRADE — TEACHER ACTION
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public SubmissionResponse gradeSubmission(UUID submissionId, GradeSubmissionRequest req) {
        AssignmentSubmission submission = requireSubmission(submissionId);

        if (submission.getStatus() == SubmissionStatus.PENDING) {
            throw new BusinessException("NOT_SUBMITTED",
                    "Cannot grade a submission that has not been submitted yet");
        }
        if (submission.getStatus() == SubmissionStatus.EXCUSED) {
            throw new BusinessException("EXCUSED",
                    "This student is excused — cannot be graded");
        }

        // Validate score against maxScore when both present
        BigDecimal maxScore = submission.getAssignment().getMaxScore();
        if (req.getScore() != null && maxScore != null) {
            if (req.getScore().compareTo(maxScore) > 0) {
                throw new BusinessException("SCORE_EXCEEDS_MAX",
                        String.format("Score %.1f exceeds the maximum score %.1f",
                                req.getScore(), maxScore));
            }
        }

        submission.setScore(req.getScore());
        submission.setFeedback(req.getFeedback());
        submission.setGradedAt(OffsetDateTime.now());
        submission.setGradedBy("SYSTEM");   // Phase 9: replace with authenticated user principal
        submission.setStatus(SubmissionStatus.GRADED);

        AssignmentSubmission saved = submissionRepository.save(submission);
        log.info("Submission {} graded — score={}, student={}",
                submissionId, req.getScore(),
                submission.getStudent().getStudentRef());
        return toSubmissionResponse(saved);
    }

    /**
     * Excuses a student from an assignment (teacher action).
     * Allowed on PENDING submissions (student didn't submit); converting
     * a graded submission to EXCUSED is blocked.
     */
    @Transactional
    public SubmissionResponse excuseSubmission(UUID submissionId) {
        AssignmentSubmission submission = requireSubmission(submissionId);

        if (submission.getStatus() == SubmissionStatus.GRADED) {
            throw new BusinessException("ALREADY_GRADED",
                    "Cannot excuse a submission that has already been graded");
        }

        submission.setStatus(SubmissionStatus.EXCUSED);
        AssignmentSubmission saved = submissionRepository.save(submission);
        log.info("Submission {} excused (student={})", submissionId,
                submission.getStudent().getStudentRef());
        return toSubmissionResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STUDENT VIEW
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getStudentSubmissions(UUID studentId) {
        studentRepository.findActiveById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
        return submissionRepository.findByStudent(studentId)
                .stream()
                .map(this::toSubmissionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getSubmissionById(UUID submissionId) {
        return toSubmissionResponse(requireSubmission(submissionId));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS — LOADERS
    // ═══════════════════════════════════════════════════════════════════════════

    private Assignment requireAssignment(UUID id) {
        return assignmentRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", id));
    }

    private AssignmentSubmission requireSubmission(UUID id) {
        return submissionRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AssignmentSubmission", id));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS — PARSE ENUMS
    // ═══════════════════════════════════════════════════════════════════════════

    private AssignmentType parseType(String value) {
        if (value == null || value.isBlank()) return AssignmentType.HOMEWORK;
        try { return AssignmentType.valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_TYPE",
                    "Invalid assignment type: '" + value + "'. Must be HOMEWORK, PROJECT, QUIZ, or CLASSWORK");
        }
    }

    private AssignmentStatus parseStatusOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try { return AssignmentStatus.valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS — MAPPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private AssignmentResponse toAssignmentResponse(Assignment a, boolean includeStats) {
        ClassTemplate t = a.getTemplate();
        ClassSession  s = a.getSession();

        Integer total = null, pending = null, submitted = null, graded = null;
        if (includeStats) {
            total     = (int) submissionRepository.countByAssignmentIdAndStatusAndDeletedAtIsNull(a.getId(), SubmissionStatus.PENDING)
                      + (int) submissionRepository.countByAssignmentIdAndStatusAndDeletedAtIsNull(a.getId(), SubmissionStatus.SUBMITTED)
                      + (int) submissionRepository.countByAssignmentIdAndStatusAndDeletedAtIsNull(a.getId(), SubmissionStatus.LATE)
                      + (int) submissionRepository.countByAssignmentIdAndStatusAndDeletedAtIsNull(a.getId(), SubmissionStatus.GRADED)
                      + (int) submissionRepository.countByAssignmentIdAndStatusAndDeletedAtIsNull(a.getId(), SubmissionStatus.EXCUSED);
            pending   = (int) submissionRepository.countByAssignmentIdAndStatusAndDeletedAtIsNull(a.getId(), SubmissionStatus.PENDING);
            submitted = (int) submissionRepository.countByAssignmentIdAndStatusAndDeletedAtIsNull(a.getId(), SubmissionStatus.SUBMITTED)
                      + (int) submissionRepository.countByAssignmentIdAndStatusAndDeletedAtIsNull(a.getId(), SubmissionStatus.LATE);
            graded    = (int) submissionRepository.countByAssignmentIdAndStatusAndDeletedAtIsNull(a.getId(), SubmissionStatus.GRADED);
        }

        String sessionLabel = null;
        if (s != null) {
            sessionLabel = String.format("%s %s–%s (%s)",
                    s.getTemplate().getDayOfWeek(),
                    s.getStartTime(), s.getEndTime(),
                    s.getSessionDate());
        }

        return AssignmentResponse.builder()
                .id(a.getId())
                .templateId(t.getId())
                .templateName(t.getName())
                .subjectName(t.getSubject().getName())
                .sessionId(s != null ? s.getId() : null)
                .sessionLabel(sessionLabel)
                .title(a.getTitle())
                .description(a.getDescription())
                .type(a.getType().name())
                .status(a.getStatus().name())
                .dueDate(a.getDueDate())
                .maxScore(a.getMaxScore())
                .allowLate(a.isAllowLate())
                .totalSubmissions(total)
                .pendingCount(pending)
                .submittedCount(submitted)
                .gradedCount(graded)
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .createdBy(a.getCreatedBy())
                .build();
    }

    private SubmissionResponse toSubmissionResponse(AssignmentSubmission s) {
        Assignment a   = s.getAssignment();
        Student    st  = s.getStudent();

        List<SubmissionAttachmentResponse> attachments = s.getAttachments()
                .stream()
                .filter(att -> att.getDeletedAt() == null)
                .map(att -> SubmissionAttachmentResponse.builder()
                        .id(att.getId())
                        .fileKey(att.getFileKey())
                        .fileName(att.getFileName())
                        .fileSize(att.getFileSize())
                        .contentType(att.getContentType())
                        .uploadedAt(att.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return SubmissionResponse.builder()
                .id(s.getId())
                .assignmentId(a.getId())
                .assignmentTitle(a.getTitle())
                .dueDate(a.getDueDate())
                .maxScore(a.getMaxScore())
                .studentId(st.getId())
                .studentRef(st.getStudentRef())
                .studentName(st.getFullName())
                .status(s.getStatus().name())
                .content(s.getContent())
                .submittedAt(s.getSubmittedAt())
                .score(s.getScore())
                .feedback(s.getFeedback())
                .gradedAt(s.getGradedAt())
                .gradedBy(s.getGradedBy())
                .attachments(attachments)
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
