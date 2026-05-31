package com.skillup.classmanagement.controller;

import com.skillup.classmanagement.dto.SubjectRequest;
import com.skillup.classmanagement.dto.SubjectResponse;
import com.skillup.classmanagement.service.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/subjects")
@RequiredArgsConstructor
@Tag(name = "Subjects", description = "Manage subjects / topics taught at the centre")
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    @Operation(summary = "List all subjects (activeOnly=true by default)")
    public ResponseEntity<List<SubjectResponse>> list(
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(subjectService.findAll(activeOnly));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a subject by ID")
    public ResponseEntity<SubjectResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(subjectService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new subject")
    public ResponseEntity<SubjectResponse> create(@Valid @RequestBody SubjectRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subjectService.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a subject")
    public ResponseEntity<SubjectResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody SubjectRequest req) {
        return ResponseEntity.ok(subjectService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a subject")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        subjectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
