package com.skillup.academic.controller;

import com.skillup.academic.dto.*;
import com.skillup.academic.service.AcademicResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Academic Results", description = "Exam score tracking — school exams and internal tuition tests with Malaysian grading")
public class AcademicResultController {

    private final AcademicResultService academicResultService;

    // ── Record results ────────────────────────────────────────────────────────

    @PostMapping("/exam-results")
    @Operation(summary = "Record a single student's exam result (auto-calculates Malaysian grade from score %)")
    public ResponseEntity<ExamResultResponse> record(
            @Valid @RequestBody RecordExamResultRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(academicResultService.recordResult(req));
    }

    @PostMapping("/exam-results/bulk")
    @Operation(summary = "Record results for multiple students in one exam (e.g. after an internal class test)")
    public ResponseEntity<List<ExamResultResponse>> bulkRecord(
            @Valid @RequestBody BulkRecordExamResultRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(academicResultService.bulkRecordResults(req));
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @GetMapping("/exam-results/{id}")
    @Operation(summary = "Get a single exam result by ID")
    public ResponseEntity<ExamResultResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(academicResultService.getById(id));
    }

    @PatchMapping("/exam-results/{id}")
    @Operation(summary = "Correct an exam result (score, grade, date, notes) — for data-entry fixes")
    public ResponseEntity<ExamResultResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExamResultRequest req) {
        return ResponseEntity.ok(academicResultService.updateResult(id, req));
    }

    // ── Student views ─────────────────────────────────────────────────────────

    @GetMapping("/students/{studentId}/exam-results")
    @Operation(summary = "Get a student's exam result history (filter by subjectId, examType, date range)")
    public ResponseEntity<List<ExamResultResponse>> getStudentResults(
            @PathVariable UUID studentId,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) String examType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(
                academicResultService.getStudentResults(studentId, subjectId, examType, from, to));
    }

    @GetMapping("/students/{studentId}/exam-results/summary")
    @Operation(summary = "Get a student's full academic summary — per-subject averages, trends, and recent results")
    public ResponseEntity<StudentAcademicSummaryResponse> getStudentSummary(
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(academicResultService.getStudentSummary(studentId));
    }

    // ── Class view ────────────────────────────────────────────────────────────

    @GetMapping("/classes/{templateId}/exam-results")
    @Operation(summary = "Get exam results for a class (optionally filter by exam name — partial match)")
    public ResponseEntity<List<ExamResultResponse>> getClassResults(
            @PathVariable UUID templateId,
            @RequestParam(required = false) String examName) {
        return ResponseEntity.ok(academicResultService.getClassResults(templateId, examName));
    }
}
