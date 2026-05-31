package com.skillup.fees.service;

import com.skillup.classmanagement.domain.ClassEnrollment;
import com.skillup.classmanagement.domain.ClassTemplate;
import com.skillup.classmanagement.domain.EnrollmentStatus;
import com.skillup.classmanagement.repository.ClassEnrollmentRepository;
import com.skillup.common.exception.BusinessException;
import com.skillup.common.exception.ResourceNotFoundException;
import com.skillup.fees.domain.*;
import com.skillup.fees.dto.*;
import com.skillup.fees.repository.FeePaymentRepository;
import com.skillup.fees.repository.FeeRecordRepository;
import com.skillup.student.domain.Student;
import com.skillup.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeService {

    private final FeeRecordRepository      feeRecordRepository;
    private final FeePaymentRepository     feePaymentRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final StudentRepository        studentRepository;

    // ═══════════════════════════════════════════════════════════════════════════
    // FEE GENERATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Generate monthly fee records for every active enrolment.
     * Idempotent — skips enrolments that already have a record for this billing month
     * or where the class fee is RM 0.
     */
    @Transactional
    public GenerateFeesResult generateMonthlyFees(GenerateFeesRequest req) {
        LocalDate billingMonth = req.getBillingMonth().withDayOfMonth(1);
        LocalDate dueDate = req.getDueDate() != null
                ? req.getDueDate()
                : billingMonth.withDayOfMonth(7);

        if (dueDate.isBefore(billingMonth)) {
            throw new BusinessException("INVALID_DUE_DATE",
                    "Due date cannot be before the billing month start");
        }

        List<ClassEnrollment> enrollments = enrollmentRepository
                .findActiveEnrollments(EnrollmentStatus.ACTIVE, req.getBranchId());

        int created = 0, skipped = 0;
        for (ClassEnrollment enrollment : enrollments) {
            ClassTemplate template = enrollment.getTemplate();

            // Skip free classes
            if (template.getFeeAmount().compareTo(BigDecimal.ZERO) == 0) {
                skipped++;
                continue;
            }

            // Idempotency: skip if record already exists for this student-template-month
            if (feeRecordRepository.existsByStudentIdAndTemplateIdAndBillingMonthAndDeletedAtIsNull(
                    enrollment.getStudent().getId(), template.getId(), billingMonth)) {
                skipped++;
                continue;
            }

            FeeRecord fee = FeeRecord.builder()
                    .student(enrollment.getStudent())
                    .template(template)
                    .billingMonth(billingMonth)
                    .amount(template.getFeeAmount())
                    .dueDate(dueDate)
                    .status(FeeStatus.PENDING)
                    .build();

            feeRecordRepository.save(fee);
            created++;
        }

        log.info("Fee generation for {}: created={}, skipped={}", billingMonth, created, skipped);
        return GenerateFeesResult.builder()
                .billingMonth(billingMonth)
                .dueDate(dueDate)
                .created(created)
                .skipped(skipped)
                .message(String.format("Generated %d fee record(s) for %s. %d skipped (already existed or free class).",
                        created, billingMonth, skipped))
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FEE RECORDS — READ
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public FeeRecordResponse getById(UUID id) {
        return toResponse(requireFeeRecord(id), true);
    }

    @Transactional(readOnly = true)
    public List<FeeRecordResponse> search(String status, UUID studentId, UUID branchId, LocalDate billingMonth) {
        FeeStatus feeStatus = parseStatusOrNull(status);
        return feeRecordRepository.search(feeStatus, studentId, branchId, billingMonth)
                .stream()
                .map(f -> toResponse(f, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FeeRecordResponse> getStudentFees(UUID studentId) {
        studentRepository.findActiveById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
        return feeRecordRepository.findByStudentId(studentId)
                .stream()
                .map(f -> toResponse(f, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudentBalanceResponse getStudentBalance(UUID studentId) {
        Student student = studentRepository.findActiveById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        List<FeeRecord> unpaid = feeRecordRepository.findUnpaidByStudentId(studentId);

        BigDecimal totalDue = unpaid.stream()
                .map(FeeRecord::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaid = unpaid.stream()
                .map(FeeRecord::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOutstanding = totalDue.subtract(totalPaid);

        long overdueCount = unpaid.stream()
                .filter(f -> f.getStatus() == FeeStatus.OVERDUE)
                .count();

        List<FeeRecordResponse> unpaidResponses = unpaid.stream()
                .map(f -> toResponse(f, false))
                .collect(Collectors.toList());

        return StudentBalanceResponse.builder()
                .studentId(student.getId())
                .studentRef(student.getStudentRef())
                .studentName(student.getFullName())
                .totalDue(totalDue)
                .totalPaid(totalPaid)
                .totalOutstanding(totalOutstanding)
                .overdueCount((int) overdueCount)
                .unpaidFees(unpaidResponses)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PAYMENTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public FeeRecordResponse recordPayment(UUID feeRecordId, RecordPaymentRequest req) {
        FeeRecord feeRecord = requireFeeRecord(feeRecordId);

        if (feeRecord.getStatus() == FeeStatus.PAID) {
            throw new BusinessException("ALREADY_PAID", "This fee record is already fully paid");
        }
        if (feeRecord.getStatus() == FeeStatus.WAIVED) {
            throw new BusinessException("FEE_WAIVED", "Cannot record payment against a waived fee");
        }

        BigDecimal balance = feeRecord.getBalance();
        if (req.getAmountPaid().compareTo(balance) > 0) {
            throw new BusinessException("OVERPAYMENT",
                    String.format("Payment RM %.2f exceeds outstanding balance RM %.2f",
                            req.getAmountPaid(), balance));
        }

        PaymentMethod method = parsePaymentMethod(req.getPaymentMethod());

        FeePayment payment = FeePayment.builder()
                .feeRecord(feeRecord)
                .amountPaid(req.getAmountPaid())
                .paymentDate(req.getPaymentDate() != null ? req.getPaymentDate() : LocalDate.now())
                .paymentMethod(method)
                .referenceNumber(req.getReferenceNumber())
                .notes(req.getNotes())
                .build();

        feePaymentRepository.save(payment);

        // Update running paid total and recalculate status
        feeRecord.setPaidAmount(feeRecord.getPaidAmount().add(req.getAmountPaid()));
        feeRecord.recalculateStatus(LocalDate.now());
        feeRecordRepository.save(feeRecord);

        log.info("Payment recorded: RM {} for fee {} (student {} — {})",
                req.getAmountPaid(), feeRecordId,
                feeRecord.getStudent().getStudentRef(),
                feeRecord.getTemplate().getName());

        return toResponse(feeRecord, true);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // WAIVE / OVERDUE
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public FeeRecordResponse waiveFee(UUID id, WaiveFeeRequest req) {
        FeeRecord feeRecord = requireFeeRecord(id);

        if (feeRecord.getStatus() == FeeStatus.PAID) {
            throw new BusinessException("ALREADY_PAID", "Cannot waive a fee that has already been paid");
        }
        if (feeRecord.getStatus() == FeeStatus.WAIVED) {
            throw new BusinessException("ALREADY_WAIVED", "This fee is already waived");
        }

        feeRecord.setStatus(FeeStatus.WAIVED);
        String reason = req.getReason() != null ? req.getReason() : "Waived by admin";
        String note = "[WAIVED] " + reason;
        feeRecord.setNotes(feeRecord.getNotes() != null
                ? feeRecord.getNotes() + "\n" + note : note);

        FeeRecord saved = feeRecordRepository.save(feeRecord);
        log.info("Fee {} waived: {}", id, reason);
        return toResponse(saved, true);
    }

    /**
     * Mark all PENDING/PARTIAL fees past their due date as OVERDUE.
     * Called by the scheduler — also callable manually from the API.
     *
     * @return number of records updated
     */
    @Transactional
    public int markOverdueFees() {
        LocalDate today = LocalDate.now();
        List<FeeRecord> pastDue = feeRecordRepository.findPendingPastDue(today);
        for (FeeRecord fee : pastDue) {
            fee.setStatus(FeeStatus.OVERDUE);
            feeRecordRepository.save(fee);
        }
        if (!pastDue.isEmpty()) {
            log.info("Marked {} fee record(s) as OVERDUE", pastDue.size());
        }
        return pastDue.size();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private FeeRecord requireFeeRecord(UUID id) {
        return feeRecordRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FeeRecord", id));
    }

    private FeeStatus parseStatusOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try { return FeeStatus.valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private PaymentMethod parsePaymentMethod(String value) {
        if (value == null || value.isBlank()) return PaymentMethod.CASH;
        try { return PaymentMethod.valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_PAYMENT_METHOD",
                    "Invalid payment method: '" + value + "'. Must be CASH, BANK_TRANSFER, ONLINE, or CARD");
        }
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private FeeRecordResponse toResponse(FeeRecord f, boolean includePayments) {
        Student s      = f.getStudent();
        ClassTemplate t = f.getTemplate();

        List<PaymentResponse> payments = null;
        if (includePayments) {
            payments = feePaymentRepository.findByFeeRecordId(f.getId())
                    .stream()
                    .map(this::toPaymentResponse)
                    .collect(Collectors.toList());
        }

        return FeeRecordResponse.builder()
                .id(f.getId())
                .studentId(s.getId())
                .studentRef(s.getStudentRef())
                .studentName(s.getFullName())
                .templateId(t.getId())
                .templateName(t.getName())
                .subjectName(t.getSubject().getName())
                .billingMonth(f.getBillingMonth())
                .amount(f.getAmount())
                .discountAmount(f.getDiscountAmount())
                .netAmount(f.getNetAmount())
                .paidAmount(f.getPaidAmount())
                .balance(f.getBalance())
                .dueDate(f.getDueDate())
                .status(f.getStatus().name())
                .notes(f.getNotes())
                .payments(payments)
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .build();
    }

    private PaymentResponse toPaymentResponse(FeePayment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .feeRecordId(p.getFeeRecord().getId())
                .amountPaid(p.getAmountPaid())
                .paymentDate(p.getPaymentDate())
                .paymentMethod(p.getPaymentMethod().name())
                .referenceNumber(p.getReferenceNumber())
                .notes(p.getNotes())
                .recordedBy(p.getCreatedBy())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
