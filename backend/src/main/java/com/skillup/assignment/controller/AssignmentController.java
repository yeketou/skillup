package com.skillup.assignment.controller;

import com.skillup.assignment.dto.*;
import com.skillup.assignment.service.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Assignments", description = "Homework, quizzes, and project management with submission tracking")
public class AssignmentController {

    private final AssignmentService assignmentService;

    // ── Assignment CRUD ───────────────────────────────────────────────────────

    @PostMapping("/classes/{templateId}/assignments")
    @Operation(summary = "Create a new assignment for a class (starts in DRAFT status)")
    public ResponseEntity<AssignmentResponse> create(
            @PathVariable UUID templateId,
            @Valid @RequestBody CreateAssignmentRequest req) {
        req.setTemplateId(templateId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignmentService.createAssignment(req));
    }

    @GetMapping("/classes/{templateId}/assignments")
    @Operation(summary = "List all assignments for a class (optional status filter: DRAFT | PUBLISHED | CLOSED)")
    public ResponseEntity<List<AssignmentResponse>> listForTemplate(
            @PathVariable UUID templateId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(assignmentService.getAssignmentsForTemplate(templateId, status));
    }

    @GetMapping("/assignments/{id}")
    @Operation(summary = "Get assignment detail including submission summary stats")
    public ResponseEntity<AssignmentResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(assignmentService.getAssignment(id));
    }

    @PutMapping("/assignments/{id}")
    @Operation(summary = "Update a DRAFT assignment (title, description, type, dueDate, maxScore, allowLate)")
    public ResponseEntity<AssignmentResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAssignmentRequest req) {
        return ResponseEntity.ok(assignmentService.updateAssignment(id, req));
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @PostMapping("/assignments/{id}/publish")
    @Operation(summary = "Publish a DRAFT assignment — auto-creates PENDING submission slots for all active students")
    public ResponseEntity<AssignmentResponse> publish(@PathVariable UUID id) {
        return ResponseEntity.ok(assignmentService.publishAssignment(id));
    }

    @PostMapping("/assignments/{id}/close")
    @Operation(summary = "Close a PUBLISHED assignment — stops accepting new submissions")
    public ResponseEntity<AssignmentResponse> close(@PathVariable UUID id) {
        return ResponseEntity.ok(assignmentService.closeAssignment(id));
    }

    // ── Submissions — Teacher ─────────────────────────────────────────────────

    @GetMapping("/assignments/{id}/submissions")
    @Operation(summary = "Get all submission slots for an assignment (teacher roster view)")
    public ResponseEntity<List<SubmissionResponse>> getSubmissions(@PathVariable UUID id) {
        return ResponseEntity.ok(assignmentService.getSubmissions(id));
    }

    @PostMapping("/assignments/{id}/submissions")
    @Operation(summary = "Submit an assignment (student or teacher submitting on behalf of a student)")
    public ResponseEntity<SubmissionResponse> submit(
            @PathVariable UUID id,
            @Valid @RequestBody SubmitAssignmentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignmentService.submitAssignment(id, req));
    }

    // ── Grading ───────────────────────────────────────────────────────────────

    @PatchMapping("/submissions/{submissionId}/grade")
    @Operation(summary = "Grade a submission — set score and written feedback")
    public ResponseEntity<SubmissionResponse> grade(
            @PathVariable UUID submissionId,
            @Valid @RequestBody GradeSubmissionRequest req) {
        return ResponseEntity.ok(assignmentService.gradeSubmission(submissionId, req));
    }

    @PatchMapping("/submissions/{submissionId}/excuse")
    @Operation(summary = "Excuse a student from an assignment (marks as EXCUSED — not counted as missing)")
    public ResponseEntity<SubmissionResponse> excuse(@PathVariable UUID submissionId) {
        return ResponseEntity.ok(assignmentService.excuseSubmission(submissionId));
    }

    @GetMapping("/submissions/{submissionId}")
    @Operation(summary = "Get a single submission detail")
    public ResponseEntity<SubmissionResponse> getSubmission(@PathVariable UUID submissionId) {
        return ResponseEntity.ok(assignmentService.getSubmissionById(submissionId));
    }

    // ── Student view ──────────────────────────────────────────────────────────

    @GetMapping("/students/{studentId}/submissions")
    @Operation(summary = "Get all assignment submissions for a student (their submission history)")
    public ResponseEntity<List<SubmissionResponse>> getStudentSubmissions(
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(assignmentService.getStudentSubmissions(studentId));
    }
}
