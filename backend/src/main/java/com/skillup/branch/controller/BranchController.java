package com.skillup.branch.controller;

import com.skillup.branch.dto.BranchRequest;
import com.skillup.branch.dto.BranchResponse;
import com.skillup.branch.service.BranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/branches")
@RequiredArgsConstructor
@Tag(name = "Branches", description = "Manage tuition centre locations")
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    @Operation(summary = "List all active branches")
    public ResponseEntity<List<BranchResponse>> listBranches() {
        return ResponseEntity.ok(branchService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a branch by ID")
    public ResponseEntity<BranchResponse> getBranch(@PathVariable UUID id) {
        return ResponseEntity.ok(branchService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Create a new branch")
    public ResponseEntity<BranchResponse> createBranch(@Valid @RequestBody BranchRequest request) {
        BranchResponse response = branchService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Update a branch")
    public ResponseEntity<BranchResponse> updateBranch(
            @PathVariable UUID id,
            @Valid @RequestBody BranchRequest request) {
        return ResponseEntity.ok(branchService.update(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Deactivate a branch (keeps data, hides from active lists)")
    public ResponseEntity<Void> deactivateBranch(@PathVariable UUID id) {
        branchService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Soft-delete a branch (OWNER only)")
    public ResponseEntity<Void> deleteBranch(@PathVariable UUID id) {
        branchService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
