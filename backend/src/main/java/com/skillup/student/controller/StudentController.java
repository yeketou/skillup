package com.skillup.student.controller;

import com.skillup.student.dto.*;
import com.skillup.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
@Tag(name = "Students", description = "Student registration and management")
public class StudentController {

    private final StudentService studentService;

    // ── List / Search ─────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Search students with filters (name, status, branch, grade)")
    public ResponseEntity<Page<StudentSummaryResponse>> search(
            @ParameterObject @ModelAttribute StudentSearchRequest req) {
        return ResponseEntity.ok(studentService.search(req));
    }

    // ── Get one ───────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get full student detail by ID")
    public ResponseEntity<StudentResponse> getStudent(@PathVariable UUID id) {
        return ResponseEntity.ok(studentService.findById(id));
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Register a new student (includes parent/guardian details)")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<StudentResponse> createStudent(
            @Valid @RequestBody CreateStudentRequest req) {
        StudentResponse response = studentService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Update personal details ───────────────────────────────────────────────

    @PutMapping("/{id}")
    @Operation(summary = "Update student personal and academic details")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStudentRequest req) {
        return ResponseEntity.ok(studentService.update(id, req));
    }

    // ── Change status ─────────────────────────────────────────────────────────

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change student enrollment status (ACTIVE / INACTIVE / SUSPENDED / GRADUATED)")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<StudentResponse> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StudentStatusRequest req) {
        return ResponseEntity.ok(studentService.changeStatus(id, req));
    }

    // ── Parents ───────────────────────────────────────────────────────────────

    @PostMapping("/{id}/parents")
    @Operation(summary = "Add an additional parent/guardian to a student")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<StudentResponse> addParent(
            @PathVariable UUID id,
            @Valid @RequestBody ParentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.addParent(id, req));
    }

    // ── Portal account ────────────────────────────────────────────────────────

    @PostMapping("/{id}/portal/activate")
    @Operation(summary = "Activate Student Portal account for the primary parent (sends welcome email)")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<Void> activatePortal(@PathVariable UUID id) {
        studentService.activatePortalAccount(id);
        return ResponseEntity.noContent().build();
    }

    // ── Soft delete ───────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a student record (OWNER only; retains all history)")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteStudent(@PathVariable UUID id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
