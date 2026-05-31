package com.skillup.fees.controller;

import com.skillup.fees.dto.*;
import com.skillup.fees.service.FeeService;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Fees", description = "Monthly fee generation, payment recording, and outstanding balances")
public class FeeController {

    private final FeeService feeService;

    // ── Fee generation ────────────────────────────────────────────────────────

    @PostMapping("/fees/generate")
    @Operation(summary = "Generate monthly fee records for all active enrolments (idempotent)")
    public ResponseEntity<GenerateFeesResult> generateFees(
            @Valid @RequestBody GenerateFeesRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(feeService.generateMonthlyFees(req));
    }

    // ── Fee record search ─────────────────────────────────────────────────────

    @GetMapping("/fees")
    @Operation(summary = "Search fee records (filter by status, studentId, branchId, billingMonth)")
    public ResponseEntity<List<FeeRecordResponse>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate billingMonth) {
        return ResponseEntity.ok(feeService.search(status, studentId, branchId, billingMonth));
    }

    @GetMapping("/fees/{id}")
    @Operation(summary = "Get full fee record detail including payment history")
    public ResponseEntity<FeeRecordResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(feeService.getById(id));
    }

    // ── Payments ──────────────────────────────────────────────────────────────

    @PostMapping("/fees/{id}/payments")
    @Operation(summary = "Record a payment against a fee record (supports partial / instalment payments)")
    public ResponseEntity<FeeRecordResponse> recordPayment(
            @PathVariable UUID id,
            @Valid @RequestBody RecordPaymentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(feeService.recordPayment(id, req));
    }

    // ── Waive ─────────────────────────────────────────────────────────────────

    @PatchMapping("/fees/{id}/waive")
    @Operation(summary = "Waive a fee record (write off — sets status to WAIVED)")
    public ResponseEntity<FeeRecordResponse> waiveFee(
            @PathVariable UUID id,
            @RequestBody(required = false) WaiveFeeRequest req) {
        return ResponseEntity.ok(feeService.waiveFee(id, req != null ? req : new WaiveFeeRequest()));
    }

    // ── Overdue sweep (manual trigger) ────────────────────────────────────────

    @PostMapping("/fees/mark-overdue")
    @Operation(summary = "Manually trigger overdue sweep (normally runs automatically at 08:00 MYT daily)")
    public ResponseEntity<Map<String, Integer>> markOverdue() {
        int count = feeService.markOverdueFees();
        return ResponseEntity.ok(Map.of("markedOverdue", count));
    }

    // ── Student fee views ─────────────────────────────────────────────────────

    @GetMapping("/students/{studentId}/fees")
    @Operation(summary = "Get all fee records for a student (full history)")
    public ResponseEntity<List<FeeRecordResponse>> getStudentFees(
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(feeService.getStudentFees(studentId));
    }

    @GetMapping("/students/{studentId}/fees/balance")
    @Operation(summary = "Get outstanding balance for a student (unpaid + partially paid fees)")
    public ResponseEntity<StudentBalanceResponse> getStudentBalance(
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(feeService.getStudentBalance(studentId));
    }
}
