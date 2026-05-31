package com.skillup.portal.controller;

import com.skillup.assignment.domain.AssignmentSubmission;
import com.skillup.assignment.domain.SubmissionStatus;
import com.skillup.assignment.dto.SubmissionResponse;
import com.skillup.assignment.dto.SubmitAssignmentRequest;
import com.skillup.assignment.repository.AssignmentSubmissionRepository;
import com.skillup.assignment.service.AssignmentService;
import com.skillup.attendance.domain.AttendanceRecord;
import com.skillup.attendance.repository.AttendanceRepository;
import com.skillup.fees.domain.FeeRecord;
import com.skillup.fees.domain.FeeStatus;
import com.skillup.fees.repository.FeeRecordRepository;
import com.skillup.portal.dto.*;
import com.skillup.portal.service.PortalContextService;
import com.skillup.report.dto.StudentReportResponse;
import com.skillup.report.service.ReportService;
import com.skillup.student.domain.Student;
import com.skillup.student.domain.StudentPortalAccount;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Parent-facing portal endpoints.
 *
 * All endpoints require either:
 * <ul>
 *   <li>A valid Keycloak JWT (production), or</li>
 *   <li>An {@code X-Portal-Account-ID} header (local dev only, when {@code skillup.portal.local-dev-bypass=true})</li>
 * </ul>
 *
 * Student-scoped endpoints enforce that the requested student belongs to the
 * authenticated portal account — parents cannot view other children's data.
 */
@RestController
@RequestMapping("/portal")
@RequiredArgsConstructor
@Transactional(readOnly = true)   // keeps the JPA session open so lazy associations can be resolved
@Tag(name = "Portal — My Account", description = "Parent-facing endpoints: profile, children, assignments, fees, attendance")
public class PortalController {

    private final PortalContextService           portalContextService;
    private final AssignmentSubmissionRepository submissionRepository;
    private final AssignmentService              assignmentService;
    private final FeeRecordRepository            feeRecordRepository;
    private final AttendanceRepository           attendanceRepository;
    private final ReportService                  reportService;

    // ═══════════════════════════════════════════════════════════════════════════
    // ME
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/me")
    @Operation(
            summary = "My portal account + children overview",
            description = "Returns the portal account profile and a quick-glance summary for each linked student."
    )
    public ResponseEntity<PortalMeResponse> getMe(HttpServletRequest request) {
        StudentPortalAccount account = portalContextService.getCurrentAccount(request);

        List<PortalStudentSummary> summaries = account.getStudents().stream()
                .filter(s -> s.getDeletedAt() == null)
                .map(s -> buildStudentSummary(s))
                .collect(Collectors.toList());

        return ResponseEntity.ok(PortalMeResponse.builder()
                .accountId(account.getId())
                .email(account.getEmail())
                .fullName(account.getFullName())
                .keycloakProvisioned(account.isKeycloakProvisioned())
                .lastLoginAt(account.getLastLoginAt())
                .students(summaries)
                .build());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ASSIGNMENTS
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/students/{studentId}/assignments")
    @Operation(
            summary = "My child's assignments with submission status",
            description = "Lists all assignments for the student, newest due date first, including their submission status and any grading/feedback."
    )
    public ResponseEntity<List<PortalAssignmentView>> getAssignments(
            @Parameter(description = "Student UUID") @PathVariable UUID studentId,
            HttpServletRequest request) {

        portalContextService.requireStudentAccess(request, studentId);

        List<AssignmentSubmission> subs = submissionRepository.findByStudent(studentId);
        List<PortalAssignmentView> views = subs.stream()
                .map(this::toAssignmentView)
                .collect(Collectors.toList());

        return ResponseEntity.ok(views);
    }

    @PostMapping("/students/{studentId}/assignments/{assignmentId}/submit")
    @Transactional   // override class-level readOnly=true for this write operation
    @Operation(
            summary = "Submit an assignment on behalf of the student",
            description = """
                    Submit text content and/or file attachments for an assignment.
                    File keys must have been obtained from the presign-upload endpoint first.
                    Returns the updated submission record.
                    """
    )
    public ResponseEntity<SubmissionResponse> submitAssignment(
            @Parameter(description = "Student UUID") @PathVariable UUID studentId,
            @Parameter(description = "Assignment UUID") @PathVariable UUID assignmentId,
            @Valid @RequestBody PortalSubmitRequest req,
            HttpServletRequest request) {

        portalContextService.requireStudentAccess(request, studentId);

        // Build the standard SubmitAssignmentRequest with studentId locked to portal context
        SubmitAssignmentRequest submitReq = new SubmitAssignmentRequest();
        submitReq.setStudentId(studentId);
        submitReq.setContent(req.getContent());
        submitReq.setAttachments(req.getAttachments() != null ? req.getAttachments() : List.of());

        SubmissionResponse response = assignmentService.submitAssignment(assignmentId, submitReq);
        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FEES
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/students/{studentId}/fees")
    @Operation(
            summary = "My child's fee history",
            description = "Returns all fee records for the student — most recent billing month first."
    )
    public ResponseEntity<List<FeeRecord>> getFees(
            @Parameter(description = "Student UUID") @PathVariable UUID studentId,
            HttpServletRequest request) {

        portalContextService.requireStudentAccess(request, studentId);
        return ResponseEntity.ok(feeRecordRepository.findByStudentId(studentId));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ATTENDANCE
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/students/{studentId}/attendance")
    @Operation(
            summary = "My child's attendance history",
            description = "Returns all marked attendance records for the student — most recent first."
    )
    public ResponseEntity<List<AttendanceRecord>> getAttendance(
            @Parameter(description = "Student UUID") @PathVariable UUID studentId,
            HttpServletRequest request) {

        portalContextService.requireStudentAccess(request, studentId);
        return ResponseEntity.ok(attendanceRepository.findByStudentId(studentId));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FULL REPORT
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/students/{studentId}/report")
    @Operation(
            summary = "Comprehensive student report",
            description = "All-in-one parent-facing report: enrolled classes, fees, attendance, assignments, and exam results."
    )
    public ResponseEntity<StudentReportResponse> getReport(
            @Parameter(description = "Student UUID") @PathVariable UUID studentId,
            HttpServletRequest request) {

        portalContextService.requireStudentAccess(request, studentId);
        return ResponseEntity.ok(reportService.getStudentReport(studentId));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private PortalStudentSummary buildStudentSummary(Student s) {
        List<AssignmentSubmission> subs = submissionRepository.findByStudent(s.getId());
        int pending = (int) subs.stream()
                .filter(sub -> sub.getStatus() == SubmissionStatus.PENDING)
                .count();

        List<FeeRecord> fees = feeRecordRepository.findByStudentId(s.getId());
        int overdueCount = (int) fees.stream()
                .filter(f -> f.getStatus() == FeeStatus.OVERDUE)
                .count();
        BigDecimal outstanding = fees.stream()
                .map(FeeRecord::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PortalStudentSummary.builder()
                .studentId(s.getId())
                .studentRef(s.getStudentRef())
                .fullName(s.getFullName())
                .preferredName(s.getPreferredName())
                .currentGrade(s.getCurrentGrade())
                .schoolName(s.getSchoolName())
                .branchName(s.getBranch().getName())
                .status(s.getStatus().name())
                .pendingAssignments(pending)
                .overdueFeesCount(overdueCount)
                .outstandingBalance(outstanding)
                .build();
    }

    private PortalAssignmentView toAssignmentView(AssignmentSubmission sub) {
        var assignment = sub.getAssignment();
        var template   = assignment.getTemplate();

        List<PortalAssignmentView.AttachmentView> attachViews = sub.getAttachments().stream()
                .filter(a -> a.getDeletedAt() == null)
                .map(a -> PortalAssignmentView.AttachmentView.builder()
                        .attachmentId(a.getId())
                        .fileName(a.getFileName())
                        .fileSize(a.getFileSize())
                        .contentType(a.getContentType())
                        // fileKey is intentionally NOT exposed — use the download presign endpoint
                        .build())
                .collect(Collectors.toList());

        return PortalAssignmentView.builder()
                .assignmentId(assignment.getId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .type(assignment.getType().name())
                .dueDate(assignment.getDueDate())
                .allowLate(assignment.isAllowLate())
                .maxScore(assignment.getMaxScore())
                .className(template.getName())
                .submissionId(sub.getId())
                .submissionStatus(sub.getStatus().name())
                .submittedAt(sub.getSubmittedAt())
                .score(sub.getScore())
                .feedback(sub.getFeedback())
                .attachments(attachViews)
                .build();
    }
}
