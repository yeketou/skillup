package com.skillup.classmanagement.controller;

import com.skillup.classmanagement.dto.*;
import com.skillup.classmanagement.service.ClassManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Class Management", description = "Class templates, sessions, and student enrolments")
public class ClassManagementController {

    private final ClassManagementService classManagementService;

    // ── Class Templates ───────────────────────────────────────────────────────

    @GetMapping("/classes")
    @Operation(summary = "List / search class templates (filter by branch, subject, day, active)")
    public ResponseEntity<List<ClassTemplateResponse>> listTemplates(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) String dayOfWeek,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(
                classManagementService.searchTemplates(branchId, subjectId, dayOfWeek, active));
    }

    @GetMapping("/classes/{id}")
    @Operation(summary = "Get a class template by ID")
    public ResponseEntity<ClassTemplateResponse> getTemplate(@PathVariable UUID id) {
        return ResponseEntity.ok(classManagementService.getTemplate(id));
    }

    @PostMapping("/classes")
    @Operation(summary = "Create a new recurring class template")
    public ResponseEntity<ClassTemplateResponse> createTemplate(
            @Valid @RequestBody ClassTemplateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(classManagementService.createTemplate(req));
    }

    @PutMapping("/classes/{id}")
    @Operation(summary = "Update a class template")
    public ResponseEntity<ClassTemplateResponse> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody ClassTemplateRequest req) {
        return ResponseEntity.ok(classManagementService.updateTemplate(id, req));
    }

    @DeleteMapping("/classes/{id}")
    @Operation(summary = "Soft-delete a class template (must have no active enrolments)")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        classManagementService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    // ── Sessions ──────────────────────────────────────────────────────────────

    @GetMapping("/classes/{id}/sessions")
    @Operation(summary = "List sessions for a class (optionally filter by date range)")
    public ResponseEntity<List<ClassSessionResponse>> getSessions(
            @PathVariable UUID id,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(classManagementService.getSessions(id, from, to));
    }

    @PostMapping("/classes/{id}/sessions/generate")
    @Operation(summary = "Generate class sessions for a date range (idempotent — skips existing dates)")
    public ResponseEntity<List<ClassSessionResponse>> generateSessions(
            @PathVariable UUID id,
            @Valid @RequestBody GenerateSessionsRequest req) {
        List<ClassSessionResponse> sessions = classManagementService.generateSessions(id, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(sessions);
    }

    @PatchMapping("/sessions/{sessionId}/status")
    @Operation(summary = "Update a session status (SCHEDULED → COMPLETED or CANCELLED)")
    public ResponseEntity<ClassSessionResponse> updateSessionStatus(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SessionStatusRequest req) {
        return ResponseEntity.ok(classManagementService.updateSessionStatus(sessionId, req));
    }

    // ── Enrolments ────────────────────────────────────────────────────────────

    @GetMapping("/classes/{id}/enrollments")
    @Operation(summary = "List all enrolments for a class (roster)")
    public ResponseEntity<List<ClassEnrollmentResponse>> getEnrollments(@PathVariable UUID id) {
        return ResponseEntity.ok(classManagementService.getEnrollments(id));
    }

    @PostMapping("/classes/{id}/enroll")
    @Operation(summary = "Enrol a student in this class (checks capacity)")
    public ResponseEntity<ClassEnrollmentResponse> enroll(
            @PathVariable UUID id,
            @Valid @RequestBody ClassEnrollmentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(classManagementService.enrollStudent(id, req));
    }

    @DeleteMapping("/classes/{id}/enrollments/{studentId}")
    @Operation(summary = "Drop a student from a class (sets status to DROPPED)")
    public ResponseEntity<ClassEnrollmentResponse> drop(
            @PathVariable UUID id,
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(classManagementService.dropStudent(id, studentId));
    }

    @GetMapping("/students/{studentId}/timetable")
    @Operation(summary = "Get all classes a student is enrolled in (their personal timetable)")
    public ResponseEntity<List<ClassEnrollmentResponse>> getStudentTimetable(
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(classManagementService.getStudentTimetable(studentId));
    }
}
