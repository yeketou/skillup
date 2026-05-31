package com.skillup.communication.service;

import com.skillup.attendance.domain.AttendanceRecord;
import com.skillup.communication.domain.*;
import com.skillup.communication.dto.CommunicationLogResponse;
import com.skillup.communication.dto.SendAdHocMessageRequest;
import com.skillup.communication.dto.SendTemplatedMessageRequest;
import com.skillup.communication.repository.CommunicationLogRepository;
import com.skillup.communication.repository.MessageTemplateRepository;
import com.skillup.communication.sender.WhatsAppSender;
import com.skillup.fees.domain.FeeRecord;
import com.skillup.student.domain.Student;
import com.skillup.student.domain.StudentParent;
import com.skillup.student.repository.StudentParentRepository;
import com.skillup.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final WhatsAppSender             whatsAppSender;
    private final MessageTemplateRepository  templateRepository;
    private final CommunicationLogRepository logRepository;
    private final StudentParentRepository    parentRepository;
    private final StudentRepository          studentRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy");

    // ═══════════════════════════════════════════════════════════════════════════
    // AD-HOC SEND
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Send a plain-text message directly — no template rendering.
     * Logs the attempt regardless of success/failure.
     */
    @Transactional
    public CommunicationLogResponse sendAdHoc(SendAdHocMessageRequest req) {
        return dispatchAndLog(
                null,                   // student
                null,                   // template
                req.getRecipientPhone(),
                req.getRecipientName(),
                MessageCategory.GENERAL,
                req.getMessage()
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEMPLATED SEND
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Render a named template with caller-supplied variables and send it.
     */
    @Transactional
    public CommunicationLogResponse sendTemplated(SendTemplatedMessageRequest req) {
        MessageTemplate template = templateRepository.findByName(req.getTemplateName())
                .orElseThrow(() -> new com.skillup.common.exception.ResourceNotFoundException(
                        "MessageTemplate", req.getTemplateName()));

        if (!template.isActive()) {
            throw new com.skillup.common.exception.BusinessException(
                    "TEMPLATE_INACTIVE", "Template '" + req.getTemplateName() + "' is not active");
        }

        String rendered = template.render(req.getVariables());

        // Optionally link to a student (proxy reference — no DB hit)
        Student student = req.getStudentId() != null
                ? studentRepository.getReferenceById(req.getStudentId())
                : null;

        return dispatchAndLog(student, template,
                req.getRecipientPhone(), req.getRecipientName(),
                template.getCategory(), rendered);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DOMAIN-SPECIFIC TRIGGERS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Send fee overdue reminder to the student's primary parent.
     * Called by {@link com.skillup.fees.scheduler.FeeReminderScheduler}.
     */
    @Transactional
    public void sendFeeReminder(FeeRecord fee, long daysOverdue) {
        Student student = fee.getStudent();
        Optional<StudentParent> parentOpt = parentRepository.findPrimaryByStudentId(student.getId());

        if (parentOpt.isEmpty()) {
            log.warn("[FeeReminder] Student {} has no primary parent — skipping WhatsApp reminder",
                    student.getStudentRef());
            return;
        }

        StudentParent parent = parentOpt.get();
        String phone = parent.effectiveWhatsappNumber();
        if (phone == null || phone.isBlank()) {
            log.warn("[FeeReminder] Student {} primary parent has no WhatsApp number — skipping",
                    student.getStudentRef());
            return;
        }

        String templateName = daysOverdue > 3 ? "FEE_OVERDUE" : "FEE_REMINDER";
        Optional<MessageTemplate> tmplOpt = templateRepository.findByName(templateName);

        String rendered;
        MessageTemplate template = null;

        if (tmplOpt.isPresent() && tmplOpt.get().isActive()) {
            template = tmplOpt.get();
            Map<String, String> vars = Map.of(
                    "parentName",    parent.getFullName(),
                    "studentName",   student.getFullName(),
                    "className",     fee.getTemplate().getName(),
                    "billingMonth",  fee.getBillingMonth().format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    "netAmount",     String.format("RM %.2f", fee.getNetAmount()),
                    "balance",       String.format("RM %.2f", fee.getBalance()),
                    "dueDate",       fee.getDueDate().format(DATE_FMT),
                    "daysOverdue",   String.valueOf(daysOverdue)
            );
            rendered = template.render(vars);
        } else {
            // Fallback plain-text if template missing
            rendered = String.format(
                    "Salam %s, yuran %s untuk kelas %s berjumlah RM %.2f telah tertunggak %d hari (tarikh akhir: %s). Sila jelaskan secepat mungkin. - SkillUp",
                    parent.getFullName(), student.getFullName(),
                    fee.getTemplate().getName(), fee.getBalance(),
                    daysOverdue, fee.getDueDate().format(DATE_FMT));
        }

        dispatchAndLog(student, template, phone, parent.getFullName(),
                MessageCategory.FEE_REMINDER, rendered);
    }

    /**
     * Send an absence alert to the student's primary parent.
     * Called by attendance marking when status = ABSENT.
     */
    @Transactional
    public void sendAttendanceAbsentAlert(AttendanceRecord record) {
        Student student = record.getStudent();
        Optional<StudentParent> parentOpt = parentRepository.findPrimaryByStudentId(student.getId());

        if (parentOpt.isEmpty()) return;
        StudentParent parent = parentOpt.get();
        String phone = parent.effectiveWhatsappNumber();
        if (phone == null || phone.isBlank()) return;

        Optional<MessageTemplate> tmplOpt = templateRepository.findByName("ATTENDANCE_ABSENT");
        String rendered;
        MessageTemplate template = null;

        if (tmplOpt.isPresent() && tmplOpt.get().isActive()) {
            template = tmplOpt.get();
            var session = record.getSession();
            Map<String, String> vars = Map.of(
                    "parentName",   parent.getFullName(),
                    "studentName",  student.getFullName(),
                    "className",    session.getTemplate().getName(),
                    "subject",      session.getTemplate().getSubject().getName(),
                    "sessionDate",  session.getSessionDate().format(DATE_FMT)
            );
            rendered = template.render(vars);
        } else {
            rendered = String.format("Salam %s, %s telah ditanda TIDAK HADIR hari ini. - SkillUp",
                    parent.getFullName(), student.getFullName());
        }

        dispatchAndLog(student, template, phone, parent.getFullName(),
                MessageCategory.ATTENDANCE_ALERT, rendered);
    }

    /**
     * Send a welcome message when a new student is registered.
     */
    @Transactional
    public void sendWelcome(Student student, StudentParent primaryParent) {
        String phone = primaryParent.effectiveWhatsappNumber();
        if (phone == null || phone.isBlank()) return;

        Optional<MessageTemplate> tmplOpt = templateRepository.findByName("WELCOME");
        String rendered;
        MessageTemplate template = null;

        if (tmplOpt.isPresent() && tmplOpt.get().isActive()) {
            template = tmplOpt.get();
            Map<String, String> vars = Map.of(
                    "parentName",  primaryParent.getFullName(),
                    "studentName", student.getFullName(),
                    "studentRef",  student.getStudentRef(),
                    "branchName",  student.getBranch().getName()
            );
            rendered = template.render(vars);
        } else {
            rendered = String.format("Selamat datang ke SkillUp! %s (%s) telah berjaya didaftarkan. - SkillUp",
                    student.getFullName(), student.getStudentRef());
        }

        dispatchAndLog(student, template, phone, primaryParent.getFullName(),
                MessageCategory.WELCOME, rendered);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HISTORY / SEARCH
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<CommunicationLogResponse> getStudentHistory(UUID studentId) {
        return logRepository.findByStudent(studentId)
                .stream().map(this::toLogResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CommunicationLogResponse> searchLogs(UUID studentId, String statusStr, String categoryStr) {
        MessageStatus   status   = parseStatusOrNull(statusStr);
        MessageCategory category = parseCategoryOrNull(categoryStr);
        return logRepository.search(studentId, status, category)
                .stream().map(this::toLogResponse).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CORE DISPATCH + LOG
    // ═══════════════════════════════════════════════════════════════════════════

    private CommunicationLogResponse dispatchAndLog(
            Student student, MessageTemplate template,
            String phone, String recipientName,
            MessageCategory category, String renderedContent) {

        // 1. Persist PENDING log
        CommunicationLog logEntry = CommunicationLog.builder()
                .student(student)
                .template(template)
                .recipientPhone(phone)
                .recipientName(recipientName)
                .messageCategory(category)
                .renderedContent(renderedContent)
                .status(MessageStatus.PENDING)
                .build();
        logEntry = logRepository.save(logEntry);

        // 2. Send
        WhatsAppSender.SendResult result = whatsAppSender.send(phone, renderedContent);

        // 3. Update log with outcome
        if (result.success()) {
            logEntry.setStatus(MessageStatus.SENT);
            logEntry.setSentAt(OffsetDateTime.now());
            logEntry.setProviderMessageId(result.messageId());
        } else {
            logEntry.setStatus(MessageStatus.FAILED);
            logEntry.setErrorMessage(result.errorMessage());
            log.warn("[Notification] Failed to send to {}: {}", phone, result.errorMessage());
        }
        logEntry = logRepository.save(logEntry);

        return toLogResponse(logEntry);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private MessageStatus parseStatusOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try { return MessageStatus.valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private MessageCategory parseCategoryOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try { return MessageCategory.valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private CommunicationLogResponse toLogResponse(CommunicationLog l) {
        return CommunicationLogResponse.builder()
                .id(l.getId())
                .studentId(l.getStudent() != null ? l.getStudent().getId() : null)
                .studentRef(l.getStudent() != null ? l.getStudent().getStudentRef() : null)
                .studentName(l.getStudent() != null ? l.getStudent().getFullName() : null)
                .templateId(l.getTemplate() != null ? l.getTemplate().getId() : null)
                .templateName(l.getTemplate() != null ? l.getTemplate().getName() : null)
                .recipientPhone(l.getRecipientPhone())
                .recipientName(l.getRecipientName())
                .messageCategory(l.getMessageCategory().name())
                .renderedContent(l.getRenderedContent())
                .status(l.getStatus().name())
                .sentAt(l.getSentAt())
                .providerMessageId(l.getProviderMessageId())
                .errorMessage(l.getErrorMessage())
                .createdAt(l.getCreatedAt())
                .build();
    }
}
