package com.skillup.staff.controller;

import com.skillup.assignment.dto.GradeSubmissionRequest;
import com.skillup.assignment.dto.SubmissionResponse;
import com.skillup.assignment.service.AssignmentService;
import com.skillup.attendance.dto.BulkAttendanceRequest;
import com.skillup.attendance.dto.SessionAttendanceResponse;
import com.skillup.attendance.service.AttendanceService;
import com.skillup.classmanagement.domain.ClassSession;
import com.skillup.classmanagement.domain.ClassTemplate;
import com.skillup.classmanagement.repository.ClassEnrollmentRepository;
import com.skillup.classmanagement.repository.ClassSessionRepository;
import com.skillup.classmanagement.repository.ClassTemplateRepository;
import com.skillup.staff.domain.Staff;
import com.skillup.staff.dto.TeacherMeResponse;
import com.skillup.staff.service.StaffContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Teacher-facing portal endpoints.
 *
 * All endpoints resolve the caller via {@link StaffContextService}:
 * production uses a Keycloak JWT; local dev uses the {@code X-Staff-ID} header.
 *
 * Teachers can only access sessions/classes they are assigned to.
 */
@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Tag(name = "Teacher Portal", description = "Teacher-facing endpoints: timetable, attendance, grading")
public class TeacherController {

    private final StaffContextService      staffContextService;
    private final ClassTemplateRepository  templateRepository;
    private final ClassSessionRepository   sessionRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final AttendanceService        attendanceService;
    private final AssignmentService        assignmentService;

    // ═══════════════════════════════════════════════════════════════════════════
    // ME
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/me")
    @Operation(
            summary = "My profile + assigned classes",
            description = "Returns the teacher's profile and a list of class templates currently assigned to them."
    )
    public ResponseEntity<TeacherMeResponse> getMe(HttpServletRequest request) {
        Staff staff = staffContextService.getCurrentStaff(request);

        List<ClassTemplate> templates = templateRepository.findByTeacherId(staff.getId());

        List<TeacherMeResponse.AssignedClass> assignedClasses = templates.stream()
                .map(t -> {
                    long enrolled = enrollmentRepository
                            .countByTemplateIdAndStatusAndDeletedAtIsNull(
                                    t.getId(),
                                    com.skillup.classmanagement.domain.EnrollmentStatus.ACTIVE);
                    return TeacherMeResponse.AssignedClass.builder()
                            .templateId(t.getId())
                            .templateName(t.getName())
                            .subjectName(t.getSubject().getName())
                            .dayOfWeek(t.getDayOfWeek().name())
                            .startTime(t.getStartTime())
                            .endTime(t.getEndTime())
                            .enrolledCount((int) enrolled)
                            .active(t.isActive())
                            .startDate(t.getStartDate())
                            .endDate(t.getEndDate())
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(TeacherMeResponse.builder()
                .staffId(staff.getId())
                .staffRef(staff.getStaffRef())
                .fullName(staff.getFullName())
                .role(staff.getRole().name())
                .branchName(staff.getBranch() != null ? staff.getBranch().getName() : null)
                .assignedClasses(assignedClasses)
                .build());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SESSIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/sessions")
    @Operation(
            summary = "My upcoming sessions",
            description = "Returns sessions for all classes assigned to this teacher, optionally filtered by date range."
    )
    public ResponseEntity<List<com.skillup.classmanagement.dto.ClassSessionResponse>> getMySessions(
            @Parameter(description = "From date (inclusive), defaults to today")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "To date (inclusive)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request) {

        Staff staff = staffContextService.getCurrentStaff(request);
        LocalDate effectiveFrom = (from != null) ? from : LocalDate.now();
        LocalDate effectiveTo   = (to   != null) ? to   : effectiveFrom.plusYears(1);

        List<ClassSession> sessions = sessionRepository
                .findByTeacherIdAndDateRange(staff.getId(), effectiveFrom, effectiveTo);

        List<com.skillup.classmanagement.dto.ClassSessionResponse> views = sessions.stream()
                .map(s -> toSessionResponse(s))
                .collect(Collectors.toList());

        return ResponseEntity.ok(views);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private com.skillup.classmanagement.dto.ClassSessionResponse toSessionResponse(ClassSession s) {
        ClassTemplate t = s.getTemplate();
        String teacherName = s.getTeacherName() != null ? s.getTeacherName()
                : (t.getTeacher() != null ? t.getTeacher().getFullName() : t.getTeacherName());
        return com.skillup.classmanagement.dto.ClassSessionResponse.builder()
                .id(s.getId())
                .templateId(t.getId())
                .templateName(t.getName())
                .subjectName(t.getSubject().getName())
                .sessionDate(s.getSessionDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .teacherName(teacherName)
                .status(s.getStatus().name())
                .notes(s.getNotes())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ATTENDANCE
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/sessions/{sessionId}/attendance")
    @Operation(
            summary = "View attendance sheet for a session",
            description = "Returns all enrolled students with their attendance marks. Restricted to the assigned teacher."
    )
    public ResponseEntity<SessionAttendanceResponse> getAttendance(
            @Parameter(description = "Session UUID") @PathVariable UUID sessionId,
            HttpServletRequest request) {

        staffContextService.requireSessionAccess(request, sessionId);
        return ResponseEntity.ok(attendanceService.getSessionAttendance(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/attendance")
    @Transactional
    @Operation(
            summary = "Mark attendance for a session",
            description = "Bulk upserts attendance marks. Restricted to the assigned teacher. Triggers WhatsApp absent alerts."
    )
    public ResponseEntity<SessionAttendanceResponse> markAttendance(
            @Parameter(description = "Session UUID") @PathVariable UUID sessionId,
            @Valid @RequestBody BulkAttendanceRequest req,
            HttpServletRequest request) {

        staffContextService.requireSessionAccess(request, sessionId);
        return ResponseEntity.ok(attendanceService.markAttendance(sessionId, req));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GRADING
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/sessions/{sessionId}/submissions")
    @Operation(
            summary = "Submissions pending grading for a session",
            description = "Returns all assignment submissions for the template linked to this session. Restricted to the assigned teacher."
    )
    public ResponseEntity<List<SubmissionResponse>> getSubmissions(
            @Parameter(description = "Session UUID") @PathVariable UUID sessionId,
            HttpServletRequest request) {

        ClassSession session = staffContextService.requireSessionAccess(request, sessionId);
        List<SubmissionResponse> submissions = assignmentService
                .getAssignmentsForTemplate(session.getTemplate().getId(), null)
                .stream()
                .flatMap(a -> assignmentService.getSubmissions(a.getId()).stream())
                .collect(Collectors.toList());

        return ResponseEntity.ok(submissions);
    }

    @PatchMapping("/submissions/{submissionId}/grade")
    @Transactional
    @Operation(
            summary = "Grade an assignment submission",
            description = "Sets score + feedback on a submitted assignment. The teacher's name is recorded as gradedBy."
    )
    public ResponseEntity<SubmissionResponse> gradeSubmission(
            @Parameter(description = "Submission UUID") @PathVariable UUID submissionId,
            @Valid @RequestBody GradeSubmissionRequest req,
            HttpServletRequest request) {

        Staff staff = staffContextService.getCurrentStaff(request);
        return ResponseEntity.ok(
                assignmentService.gradeSubmission(submissionId, req, staff.getFullName()));
    }
}
