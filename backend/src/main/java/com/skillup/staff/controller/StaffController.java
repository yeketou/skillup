package com.skillup.staff.controller;

import com.skillup.staff.dto.*;
import com.skillup.staff.service.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/staff")
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Tag(name = "Staff", description = "Staff management — teachers, branch admins, centre admins")
public class StaffController {

    private final StaffService staffService;

    @PostMapping
    @Transactional
    @Operation(summary = "Create a staff member")
    public ResponseEntity<StaffResponse> create(@Valid @RequestBody CreateStaffRequest req) {
        return ResponseEntity.ok(staffService.create(req));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get staff member by ID")
    public ResponseEntity<StaffResponse> getById(
            @Parameter(description = "Staff UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(staffService.findById(id));
    }

    @GetMapping
    @Operation(
            summary = "Search staff",
            description = "Filter by branch, role (TEACHER / BRANCH_ADMIN / CENTRE_ADMIN), active status, and/or name substring."
    )
    public ResponseEntity<List<StaffSummaryResponse>> search(
            @RequestParam(required = false) UUID    branchId,
            @RequestParam(required = false) String  role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String  name) {
        return ResponseEntity.ok(staffService.search(branchId, role, active, name));
    }

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "Update a staff member")
    public ResponseEntity<StaffResponse> update(
            @Parameter(description = "Staff UUID") @PathVariable UUID id,
            @Valid @RequestBody UpdateStaffRequest req) {
        return ResponseEntity.ok(staffService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "Soft-delete a staff member")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Staff UUID") @PathVariable UUID id) {
        staffService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate-portal")
    @Transactional
    @Operation(
            summary = "Activate teacher portal access",
            description = """
                    Provisions a Keycloak account for the staff member.
                    Temporary password = IC number (if set) or a random 8-char token.
                    Idempotent — safe to call again if provisioning was interrupted.
                    """
    )
    public ResponseEntity<StaffResponse> activatePortal(
            @Parameter(description = "Staff UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(staffService.activatePortalAccess(id));
    }
}
