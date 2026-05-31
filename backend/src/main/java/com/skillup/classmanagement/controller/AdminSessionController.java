package com.skillup.classmanagement.controller;

import com.skillup.classmanagement.domain.PublicHoliday;
import com.skillup.classmanagement.dto.GenerationSummaryResponse;
import com.skillup.classmanagement.dto.PublicHolidayRequest;
import com.skillup.classmanagement.dto.PublicHolidayResponse;
import com.skillup.classmanagement.repository.PublicHolidayRepository;
import com.skillup.classmanagement.service.SessionGenerationService;
import com.skillup.common.exception.BusinessException;
import com.skillup.common.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin-facing endpoints for:
 * <ul>
 *   <li>Manually triggering the session auto-generation job (useful after adding a new template
 *       mid-week or to backfill sessions after a config change).</li>
 *   <li>Managing the public holiday register that the scheduler uses to skip non-teaching days.</li>
 * </ul>
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Tag(name = "Admin — Sessions & Holidays",
     description = "Manual session generation and public holiday management")
public class AdminSessionController {

    private final SessionGenerationService sessionGenerationService;
    private final PublicHolidayRepository  holidayRepository;

    // ═══════════════════════════════════════════════════════════════════════════
    // SESSION GENERATION
    // ═══════════════════════════════════════════════════════════════════════════

    @PostMapping("/sessions/generate-upcoming")
    @Transactional
    @Operation(
            summary = "Manually trigger session auto-generation",
            description = """
                    Generates SCHEDULED sessions for all active class templates from today through
                    the given lookahead window (defaults to the configured value, typically 4 weeks).
                    Dates that fall on registered public holidays are skipped.
                    Safe to call multiple times — existing sessions are never duplicated.
                    """
    )
    public ResponseEntity<GenerationSummaryResponse> generateUpcoming(
            @Parameter(description = "Override lookahead in weeks (1–52). Defaults to the app-configured value.")
            @RequestParam(required = false) Integer lookaheadWeeks) {

        if (lookaheadWeeks != null && (lookaheadWeeks < 1 || lookaheadWeeks > 52)) {
            throw new BusinessException("INVALID_LOOKAHEAD",
                    "lookaheadWeeks must be between 1 and 52");
        }

        GenerationSummaryResponse result = (lookaheadWeeks == null)
                ? sessionGenerationService.generateDefault()
                : sessionGenerationService.generateForAllActiveTemplates(
                        LocalDate.now(),
                        LocalDate.now().plusWeeks(lookaheadWeeks));

        return ResponseEntity.ok(result);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC HOLIDAYS
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/public-holidays")
    @Operation(
            summary = "List public holidays for a year",
            description = "Returns all non-deleted holidays for the given year, ordered by date. Defaults to the current year."
    )
    public ResponseEntity<List<PublicHolidayResponse>> listHolidays(
            @Parameter(description = "Calendar year (e.g. 2026). Defaults to current year.")
            @RequestParam(required = false) Integer year) {

        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        List<PublicHolidayResponse> list = holidayRepository
                .findByYearAndDeletedAtIsNullOrderByHolidayDateAsc(targetYear)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/public-holidays")
    @Transactional
    @Operation(
            summary = "Register a public holiday",
            description = "Adds a date that the session auto-generation scheduler will skip. Returns 422 if the date is already registered."
    )
    public ResponseEntity<PublicHolidayResponse> addHoliday(
            @Valid @RequestBody PublicHolidayRequest req) {

        if (holidayRepository.findByHolidayDateAndDeletedAtIsNull(req.getHolidayDate()).isPresent()) {
            throw new BusinessException("HOLIDAY_ALREADY_EXISTS",
                    "A public holiday is already registered for " + req.getHolidayDate());
        }

        PublicHoliday ph = PublicHoliday.builder()
                .holidayDate(req.getHolidayDate())
                .name(req.getName())
                .year(req.getHolidayDate().getYear())
                .build();

        return ResponseEntity.ok(toResponse(holidayRepository.save(ph)));
    }

    @DeleteMapping("/public-holidays/{id}")
    @Transactional
    @Operation(
            summary = "Remove a public holiday",
            description = "Soft-deletes the holiday. Future scheduler runs will no longer skip that date."
    )
    public ResponseEntity<Void> removeHoliday(
            @Parameter(description = "Public holiday UUID") @PathVariable UUID id) {

        PublicHoliday ph = holidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PublicHoliday", id));

        if (ph.getDeletedAt() != null) {
            throw new BusinessException("HOLIDAY_ALREADY_DELETED", "This holiday has already been removed");
        }

        ph.softDelete();
        holidayRepository.save(ph);
        return ResponseEntity.noContent().build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private PublicHolidayResponse toResponse(PublicHoliday ph) {
        return PublicHolidayResponse.builder()
                .id(ph.getId())
                .holidayDate(ph.getHolidayDate())
                .name(ph.getName())
                .year(ph.getYear())
                .build();
    }
}
