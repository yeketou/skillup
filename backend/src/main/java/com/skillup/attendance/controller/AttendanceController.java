package com.skillup.attendance.controller;

import com.skillup.attendance.dto.*;
import com.skillup.attendance.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "Mark and query class attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    // ── Session attendance sheet ───────────────────────────────────────────────

    @GetMapping("/sessions/{sessionId}/attendance")
    @Operation(summary = "Get attendance sheet for a session (all enrolled students + their marks)")
    public ResponseEntity<SessionAttendanceResponse> getSessionAttendance(
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(attendanceService.getSessionAttendance(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/attendance")
    @Operation(summary = "Bulk mark attendance for a session (upserts — safe to call multiple times)")
    public ResponseEntity<SessionAttendanceResponse> markAttendance(
            @PathVariable UUID sessionId,
            @Valid @RequestBody BulkAttendanceRequest req) {
        return ResponseEntity.ok(attendanceService.markAttendance(sessionId, req));
    }

    @PatchMapping("/sessions/{sessionId}/attendance/{studentId}")
    @Operation(summary = "Update a single student's attendance mark for a session")
    public ResponseEntity<AttendanceRecordResponse> updateStudentMark(
            @PathVariable UUID sessionId,
            @PathVariable UUID studentId,
            @Valid @RequestBody AttendanceMarkRequest req) {
        // Ensure the studentId in the path is used (not the body's studentId if provided)
        req.setStudentId(studentId);
        return ResponseEntity.ok(attendanceService.updateStudentMark(sessionId, studentId, req));
    }

    // ── Student history & summary ─────────────────────────────────────────────

    @GetMapping("/students/{studentId}/attendance")
    @Operation(summary = "Get attendance history for a student (optional date filter)")
    public ResponseEntity<List<AttendanceRecordResponse>> getStudentHistory(
            @PathVariable UUID studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(attendanceService.getStudentHistory(studentId, from, to));
    }

    @GetMapping("/students/{studentId}/attendance/summary")
    @Operation(summary = "Get attendance summary for a student (rate per class + overall)")
    public ResponseEntity<StudentAttendanceSummaryResponse> getStudentSummary(
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(attendanceService.getStudentSummary(studentId));
    }
}
