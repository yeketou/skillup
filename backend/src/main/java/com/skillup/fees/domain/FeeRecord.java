package com.skillup.fees.domain;

import com.skillup.classmanagement.domain.ClassTemplate;
import com.skillup.common.BaseEntity;
import com.skillup.student.domain.Student;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Monthly fee charge for one student in one class.
 *
 * Generated at the start of each month for every active enrolment.
 * Updated as payments come in via {@link FeePayment} records.
 */
@Entity
@Table(name = "fee_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ClassTemplate template;

    /** First day of the billing month (e.g. 2026-06-01 represents June 2026). */
    @Column(name = "billing_month", nullable = false)
    private LocalDate billingMonth;

    /** Gross fee copied from the class template at generation time. */
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** Discount granted by admin (e.g. sibling discount, scholarship). */
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** Running total of all payments received against this record. Updated on each payment. */
    @Column(name = "paid_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private FeeStatus status = FeeStatus.PENDING;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "feeRecord", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FeePayment> payments = new ArrayList<>();

    // ── Computed helpers ──────────────────────────────────────────────────────

    /** Net amount after discount: what the student actually owes. */
    public BigDecimal getNetAmount() {
        return amount.subtract(discountAmount);
    }

    /** Remaining balance: net amount minus what's already been paid. */
    public BigDecimal getBalance() {
        return getNetAmount().subtract(paidAmount);
    }

    /**
     * Recalculate and set the status based on current paidAmount vs netAmount and due date.
     * Call this after every payment and overdue sweep.
     */
    public void recalculateStatus(LocalDate today) {
        if (this.status == FeeStatus.WAIVED) return;  // waived is terminal

        BigDecimal net = getNetAmount();
        if (paidAmount.compareTo(net) >= 0) {
            this.status = FeeStatus.PAID;
        } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.status = FeeStatus.PARTIAL;
        } else if (today.isAfter(dueDate)) {
            this.status = FeeStatus.OVERDUE;
        } else {
            this.status = FeeStatus.PENDING;
        }
    }
}
