package com.skillup.communication.controller;

import com.skillup.communication.dto.*;
import com.skillup.communication.service.MessageTemplateService;
import com.skillup.communication.service.NotificationService;
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
@RequiredArgsConstructor
@Tag(name = "Communications", description = "WhatsApp messaging — ad-hoc sends, template management, and delivery logs")
public class CommunicationController {

    private final NotificationService    notificationService;
    private final MessageTemplateService templateService;

    // ── Ad-hoc send ───────────────────────────────────────────────────────────

    @PostMapping("/communications/send")
    @Operation(summary = "Send a plain-text WhatsApp message to any phone number")
    public ResponseEntity<CommunicationLogResponse> sendAdHoc(
            @Valid @RequestBody SendAdHocMessageRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.sendAdHoc(req));
    }

    @PostMapping("/communications/send-template")
    @Operation(summary = "Render a named template with variables and send it via WhatsApp")
    public ResponseEntity<CommunicationLogResponse> sendTemplated(
            @Valid @RequestBody SendTemplatedMessageRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.sendTemplated(req));
    }

    // ── Communication logs ────────────────────────────────────────────────────

    @GetMapping("/communications/logs")
    @Operation(summary = "Search all communication logs (filter by studentId, status, category)")
    public ResponseEntity<List<CommunicationLogResponse>> searchLogs(
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(notificationService.searchLogs(studentId, status, category));
    }

    @GetMapping("/students/{studentId}/communications")
    @Operation(summary = "Get full WhatsApp message history for a student")
    public ResponseEntity<List<CommunicationLogResponse>> studentHistory(
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(notificationService.getStudentHistory(studentId));
    }

    // ── Message templates ─────────────────────────────────────────────────────

    @GetMapping("/communication-templates")
    @Operation(summary = "List all message templates (including inactive)")
    public ResponseEntity<List<MessageTemplateResponse>> listTemplates() {
        return ResponseEntity.ok(templateService.listAll());
    }

    @PostMapping("/communication-templates")
    @Operation(summary = "Create a new message template with {{placeholder}} tokens")
    public ResponseEntity<MessageTemplateResponse> createTemplate(
            @Valid @RequestBody CreateMessageTemplateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.create(req));
    }

    @GetMapping("/communication-templates/{id}")
    @Operation(summary = "Get a message template by ID")
    public ResponseEntity<MessageTemplateResponse> getTemplate(@PathVariable UUID id) {
        return ResponseEntity.ok(templateService.getById(id));
    }

    @PutMapping("/communication-templates/{id}")
    @Operation(summary = "Update a message template (content, active flag, wa_template_name, etc.)")
    public ResponseEntity<MessageTemplateResponse> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMessageTemplateRequest req) {
        return ResponseEntity.ok(templateService.update(id, req));
    }
}
