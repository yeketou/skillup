package com.skillup.report.service;

import com.skillup.academic.domain.ExamResult;
import com.skillup.academic.dto.StudentAcademicSummaryResponse;
import com.skillup.academic.repository.ExamResultRepository;
import com.skillup.academic.service.AcademicResultService;
import com.skillup.assignment.domain.SubmissionStatus;
import com.skillup.assignment.repository.AssignmentSubmissionRepository;
import com.skillup.attendance.domain.AttendanceRecord;
import com.skillup.attendance.domain.AttendanceStatus;
import com.skillup.attendance.repository.AttendanceRepository;
import com.skillup.branch.domain.Branch;
import com.skillup.branch.repository.BranchRepository;
import com.skillup.classmanagement.domain.ClassEnrollment;
import com.skillup.classmanagement.domain.ClassTemplate;
import com.skillup.classmanagement.domain.EnrollmentStatus;
import com.skillup.classmanagement.repository.ClassEnrollmentRepository;
import com.skillup.classmanagement.repository.ClassTemplateRepository;
import com.skillup.common.exception.ResourceNotFoundException;
import com.skillup.fees.domain.FeeRecord;
import com.skillup.fees.domain.FeeStatus;
import com.skillup.fees.repository.FeeRecordRepository;
import com.skillup.report.dto.*;
import com.skillup.student.domain.Student;
import com.skillup.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final BranchRepository            branchRepository;
    private final StudentRepository           studentRepository;
    private final FeeRecordRepository         feeRecordRepository;
    private final AttendanceRepository        attendanceRepository;
    private final ExamResultRepository        examResultRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final ClassEnrollmentRepository   enrollmentRepository;
    private final ClassTemplateRepository     templateRepository;
    private final AcademicResultService       academicResultService;

    // ═══════════════════════════════════════════════════════════════════════════
    // DASHBOARD
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(UUID branchId) {
        LocalDate today        = LocalDate.now();
        LocalDate currentMonth = today.withDayOfMonth(1);
        LocalDate monthEnd     = today.withDayOfMonth(today.lengthOfMonth());

        // ── Students ──────────────────────────────────────────────────────────
        long totalActive = studentRepository.countActive(branchId);

        List<Branch> branches = branchId != null
                ? List.of(branchRepository.findActiveById(branchId)
                        .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId)))
                : branchRepository.findAllActive();

        List<BranchSummaryRow> branchRows = branches.stream()
                .map(b -> BranchSummaryRow.builder()
                        .branchId(b.getId())
                        .branchName(b.getName())
                        .branchCode(b.getCode())
                        .activeStudents(studentRepository.countActiveByBranch(b.getId()))
                        .build())
                .collect(Collectors.toList());

        // ── Fees (current month) ──────────────────────────────────────────────
        List<FeeRecord> monthFees = feeRecordRepository.findByMonth(currentMonth, branchId);
        BigDecimal feeGenerated    = sumNetAmount(monthFees);
        BigDecimal feeCollected    = monthFees.stream().map(FeeRecord::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal feeOutstanding  = monthFees.stream().map(FeeRecord::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        double collectionRate = feeGenerated.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                : feeCollected.doubleValue() / feeGenerated.doubleValue() * 100.0;

        int countPaid    = (int) monthFees.stream().filter(f -> f.getStatus() == FeeStatus.PAID).count();
        int countPending = (int) monthFees.stream().filter(f -> f.getStatus() == FeeStatus.PENDING || f.getStatus() == FeeStatus.PARTIAL).count();
        int countOverdue = (int) monthFees.stream().filter(f -> f.getStatus() == FeeStatus.OVERDUE).count();

        // ── Attendance (current month) ─────────────────────────────────────────
        List<AttendanceRecord> monthAttendance =
                attendanceRepository.findByDateRange(currentMonth, monthEnd, branchId);
        int totalMarked = monthAttendance.size();
        long presentLate = monthAttendance.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.LATE)
                .count();
        double attRate = totalMarked == 0 ? 0.0 : (double) presentLate / totalMarked * 100.0;

        // ── Unique sessions count (proxy: distinct session IDs) ─────────────
        long sessionCount = monthAttendance.stream()
                .map(a -> a.getSession().getId()).distinct().count();

        // ── Pending assignment submissions ─────────────────────────────────────
        long pendingSubs = submissionRepository.countByStatusAndBranchId(
                SubmissionStatus.PENDING, branchId);

        return DashboardResponse.builder()
                .asOf(today)
                .currentBillingMonth(currentMonth)
                .totalActiveStudents(totalActive)
                .branches(branchRows)
                .feesGenerated(feeGenerated)
                .feesCollected(feeCollected)
                .feesOutstanding(feeOutstanding)
                .feeCollectionRate(round2(collectionRate))
                .feeCountPaid(countPaid)
                .feeCountPending(countPending)
                .feeCountOverdue(countOverdue)
                .attendanceTotalSessions((int) sessionCount)
                .attendanceTotalMarked(totalMarked)
                .attendanceOverallRate(round2(attRate))
                .pendingSubmissions(pendingSubs)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FINANCIAL REPORT
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public FinancialReportResponse getFinancialReport(UUID branchId, LocalDate fromMonth, LocalDate toMonth) {
        LocalDate from = fromMonth.withDayOfMonth(1);
        LocalDate to   = toMonth.withDayOfMonth(1);

        String branchName = "All Branches";
        if (branchId != null) {
            branchName = branchRepository.findActiveById(branchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId))
                    .getName();
        }

        List<FeeRecord> all = feeRecordRepository.findByMonthRange(from, to, branchId);

        // Group by billing month
        Map<LocalDate, List<FeeRecord>> byMonth = all.stream()
                .collect(Collectors.groupingBy(FeeRecord::getBillingMonth, TreeMap::new, Collectors.toList()));

        List<MonthlyFeeRow> rows = new ArrayList<>();
        for (Map.Entry<LocalDate, List<FeeRecord>> entry : byMonth.entrySet()) {
            rows.add(buildMonthRow(entry.getKey(), entry.getValue()));
        }

        BigDecimal totalGenerated   = rows.stream().map(MonthlyFeeRow::getGenerated).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCollected   = rows.stream().map(MonthlyFeeRow::getCollected).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOutstanding = rows.stream().map(MonthlyFeeRow::getOutstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
        double overallRate = totalGenerated.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                : totalCollected.doubleValue() / totalGenerated.doubleValue() * 100.0;

        return FinancialReportResponse.builder()
                .branchId(branchId)
                .branchName(branchName)
                .fromMonth(from)
                .toMonth(to)
                .months(rows)
                .totalGenerated(totalGenerated)
                .totalCollected(totalCollected)
                .totalOutstanding(totalOutstanding)
                .overallCollectionRate(round2(overallRate))
                .totalRecords(all.size())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ATTENDANCE REPORT
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public AttendanceReportResponse getAttendanceReport(UUID branchId,
                                                        LocalDate fromDate, LocalDate toDate,
                                                        int lowThreshold) {
        String branchName = "All Branches";
        if (branchId != null) {
            branchName = branchRepository.findActiveById(branchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId))
                    .getName();
        }

        List<AttendanceRecord> all = attendanceRepository.findByDateRange(fromDate, toDate, branchId);

        // ── Centre-wide totals ─────────────────────────────────────────────────
        int totalMarked  = all.size();
        int present      = count(all, AttendanceStatus.PRESENT);
        int late         = count(all, AttendanceStatus.LATE);
        int absent       = count(all, AttendanceStatus.ABSENT);
        int excused      = count(all, AttendanceStatus.EXCUSED);
        double overallRate = totalMarked == 0 ? 0.0 : (double)(present + late) / totalMarked * 100.0;

        // ── Per-class breakdown ────────────────────────────────────────────────
        Map<UUID, List<AttendanceRecord>> byTemplate = all.stream()
                .collect(Collectors.groupingBy(a -> a.getSession().getTemplate().getId()));

        List<ClassAttendanceRow> classRows = byTemplate.entrySet().stream()
                .map(e -> buildClassRow(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(ClassAttendanceRow::getClassName))
                .collect(Collectors.toList());

        // ── Low-attendance students ───────────────────────────────────────────
        // Group by (student, template)
        Map<String, List<AttendanceRecord>> byStudentTemplate = all.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getStudent().getId() + ":" + a.getSession().getTemplate().getId()));

        List<LowAttendanceStudent> lowStudents = byStudentTemplate.values().stream()
                .map(records -> {
                    AttendanceRecord first = records.get(0);
                    Student s      = first.getStudent();
                    ClassTemplate t = first.getSession().getTemplate();
                    int p = count(records, AttendanceStatus.PRESENT);
                    int l = count(records, AttendanceStatus.LATE);
                    int ab = count(records, AttendanceStatus.ABSENT);
                    int ex = count(records, AttendanceStatus.EXCUSED);
                    double rate = records.isEmpty() ? 0.0 : (double)(p + l) / records.size() * 100.0;
                    return LowAttendanceStudent.builder()
                            .studentId(s.getId()).studentRef(s.getStudentRef()).studentName(s.getFullName())
                            .templateId(t.getId()).className(t.getName())
                            .totalMarked(records.size()).present(p).late(l).absent(ab).excused(ex)
                            .attendanceRate(round2(rate))
                            .build();
                })
                .filter(s -> s.getAttendanceRate() < lowThreshold)
                .sorted(Comparator.comparingDouble(LowAttendanceStudent::getAttendanceRate))
                .collect(Collectors.toList());

        return AttendanceReportResponse.builder()
                .branchId(branchId).branchName(branchName)
                .fromDate(fromDate).toDate(toDate)
                .totalMarked(totalMarked).totalPresent(present)
                .totalLate(late).totalAbsent(absent).totalExcused(excused)
                .overallAttendanceRate(round2(overallRate))
                .classes(classRows)
                .lowAttendanceThreshold(lowThreshold)
                .lowAttendanceStudents(lowStudents)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ACADEMIC REPORT
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public AcademicReportResponse getAcademicReport(UUID branchId, LocalDate fromDate, LocalDate toDate) {
        String branchName = "All Branches";
        if (branchId != null) {
            branchName = branchRepository.findActiveById(branchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId))
                    .getName();
        }

        // Load all results in the date range (filtered by branch via template)
        List<ExamResult> all = examResultRepository.findByDateRange(fromDate, toDate, branchId);

        if (all.isEmpty()) {
            return AcademicReportResponse.builder()
                    .branchId(branchId).branchName(branchName)
                    .fromDate(fromDate).toDate(toDate)
                    .totalExams(0).overallAveragePercentage(0).overallPassRate(0)
                    .subjects(List.of()).build();
        }

        double overallAvg  = all.stream().mapToDouble(ExamResult::percentage).average().orElse(0.0);
        double passRate    = all.stream().filter(r -> r.percentage() >= 50.0).count() * 100.0 / all.size();

        // Group by subject key (same logic as academic module summary)
        Map<String, List<ExamResult>> bySubject = new LinkedHashMap<>();
        for (ExamResult r : all) {
            String key = r.getSubject() != null
                    ? "id:" + r.getSubject().getId()
                    : "label:" + Objects.toString(r.getSubjectLabel(), "_unknown");
            bySubject.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        List<SubjectPerformanceRow> subjectRows = bySubject.values().stream()
                .map(this::buildSubjectRow)
                .sorted(Comparator.comparing(SubjectPerformanceRow::getSubjectName,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        return AcademicReportResponse.builder()
                .branchId(branchId).branchName(branchName)
                .fromDate(fromDate).toDate(toDate)
                .totalExams(all.size())
                .overallAveragePercentage(round2(overallAvg))
                .overallAverageGrade(AcademicResultService.calculateGrade(
                        BigDecimal.valueOf(overallAvg), BigDecimal.valueOf(100)))
                .overallPassRate(round2(passRate))
                .subjects(subjectRows)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STUDENT REPORT
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public StudentReportResponse getStudentReport(UUID studentId) {
        Student student = studentRepository.findActiveById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        // ── Enrolled classes ──────────────────────────────────────────────────
        List<ClassEnrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        List<StudentReportResponse.EnrolledClassRow> classRows = enrollments.stream()
                .map(e -> {
                    ClassTemplate t = e.getTemplate();
                    return StudentReportResponse.EnrolledClassRow.builder()
                            .templateId(t.getId()).className(t.getName())
                            .subjectName(t.getSubject().getName())
                            .dayOfWeek(t.getDayOfWeek().name())
                            .timeSlot(t.getStartTime() + "–" + t.getEndTime())
                            .enrollmentStatus(e.getStatus().name())
                            .enrolledDate(e.getEnrolledDate())
                            .build();
                })
                .collect(Collectors.toList());

        // ── Fees ──────────────────────────────────────────────────────────────
        List<FeeRecord> fees = feeRecordRepository.findByStudentId(studentId);
        BigDecimal totalBilled = sumNetAmount(fees);
        BigDecimal totalPaid   = fees.stream().map(FeeRecord::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstanding = fees.stream().map(FeeRecord::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        int overdueCount = (int) fees.stream().filter(f -> f.getStatus() == FeeStatus.OVERDUE).count();
        LocalDate lastPayment = fees.stream()
                .filter(f -> f.getPaidAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(FeeRecord::getUpdatedAt).filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(dt -> dt.toLocalDate()).orElse(null);

        // ── Attendance ────────────────────────────────────────────────────────
        List<AttendanceRecord> attRecords = attendanceRepository.findByStudentId(studentId);
        int attTotal   = attRecords.size();
        int attPresent = count(attRecords, AttendanceStatus.PRESENT);
        int attLate    = count(attRecords, AttendanceStatus.LATE);
        int attAbsent  = count(attRecords, AttendanceStatus.ABSENT);
        int attExcused = count(attRecords, AttendanceStatus.EXCUSED);
        double attRate = attTotal == 0 ? 0.0 : (double)(attPresent + attLate) / attTotal * 100.0;

        // Per-class attendance
        Map<UUID, List<AttendanceRecord>> attByTemplate = attRecords.stream()
                .collect(Collectors.groupingBy(a -> a.getSession().getTemplate().getId()));
        List<ClassAttendanceRow> attByClass = attByTemplate.entrySet().stream()
                .map(e -> buildClassRow(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(ClassAttendanceRow::getClassName))
                .collect(Collectors.toList());

        // ── Assignments ───────────────────────────────────────────────────────
        var subs = submissionRepository.findByStudent(studentId);
        int subTotal    = subs.size();
        int subSubmit   = (int) subs.stream().filter(s -> s.getStatus() == SubmissionStatus.SUBMITTED || s.getStatus() == SubmissionStatus.LATE).count();
        int subGraded   = (int) subs.stream().filter(s -> s.getStatus() == SubmissionStatus.GRADED).count();
        int subPending  = (int) subs.stream().filter(s -> s.getStatus() == SubmissionStatus.PENDING).count();
        int subExcused  = (int) subs.stream().filter(s -> s.getStatus() == SubmissionStatus.EXCUSED).count();
        double completionRate = subTotal == 0 ? 0.0 : (double)(subSubmit + subGraded) / subTotal * 100.0;
        OptionalDouble avgScore = subs.stream()
                .filter(s -> s.getScore() != null)
                .mapToDouble(s -> s.getScore().doubleValue())
                .average();

        // ── Exam results ──────────────────────────────────────────────────────
        StudentAcademicSummaryResponse examSummary = academicResultService.getStudentSummary(studentId);

        return StudentReportResponse.builder()
                .studentId(student.getId())
                .studentRef(student.getStudentRef())
                .fullName(student.getFullName())
                .preferredName(student.getPreferredName())
                .currentGrade(student.getCurrentGrade())
                .schoolName(student.getSchoolName())
                .branchName(student.getBranch().getName())
                .enrolledDate(student.getEnrolledDate())
                .status(student.getStatus().name())
                .classes(classRows)
                // fees
                .feesTotalBilled(totalBilled)
                .feesTotalPaid(totalPaid)
                .feesOutstanding(outstanding)
                .feesOverdueCount(overdueCount)
                .feesLastPaymentDate(lastPayment)
                // attendance
                .attendanceTotalSessions(attTotal)
                .attendancePresent(attPresent)
                .attendanceLate(attLate)
                .attendanceAbsent(attAbsent)
                .attendanceExcused(attExcused)
                .attendanceOverallRate(round2(attRate))
                .attendanceByClass(attByClass)
                // assignments
                .assignmentTotal(subTotal)
                .assignmentSubmitted(subSubmit)
                .assignmentGraded(subGraded)
                .assignmentPending(subPending)
                .assignmentExcused(subExcused)
                .assignmentCompletionRate(round2(completionRate))
                .assignmentAverageScore(avgScore.isPresent() ? round2(avgScore.getAsDouble()) : null)
                // exams
                .examTotal(examSummary.getTotalExams())
                .examOverallAverage(examSummary.getOverallAveragePercentage())
                .examOverallGrade(examSummary.getOverallGrade())
                .examBySubject(examSummary.getSubjects())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private MonthlyFeeRow buildMonthRow(LocalDate month, List<FeeRecord> records) {
        BigDecimal generated   = sumNetAmount(records);
        BigDecimal collected   = records.stream().map(FeeRecord::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstanding = records.stream().map(FeeRecord::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        double rate = generated.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                : collected.doubleValue() / generated.doubleValue() * 100.0;

        return MonthlyFeeRow.builder()
                .billingMonth(month)
                .generated(generated).collected(collected).outstanding(outstanding)
                .collectionRate(round2(rate))
                .countTotal(records.size())
                .countPaid   ((int) records.stream().filter(f -> f.getStatus() == FeeStatus.PAID).count())
                .countPartial((int) records.stream().filter(f -> f.getStatus() == FeeStatus.PARTIAL).count())
                .countPending((int) records.stream().filter(f -> f.getStatus() == FeeStatus.PENDING).count())
                .countOverdue((int) records.stream().filter(f -> f.getStatus() == FeeStatus.OVERDUE).count())
                .countWaived ((int) records.stream().filter(f -> f.getStatus() == FeeStatus.WAIVED).count())
                .build();
    }

    private ClassAttendanceRow buildClassRow(UUID templateId, List<AttendanceRecord> records) {
        AttendanceRecord first = records.get(0);
        ClassTemplate    t     = first.getSession().getTemplate();
        int p  = count(records, AttendanceStatus.PRESENT);
        int l  = count(records, AttendanceStatus.LATE);
        int ab = count(records, AttendanceStatus.ABSENT);
        int ex = count(records, AttendanceStatus.EXCUSED);
        double rate = records.isEmpty() ? 0.0 : (double)(p + l) / records.size() * 100.0;

        return ClassAttendanceRow.builder()
                .templateId(templateId)
                .className(t.getName())
                .subjectName(t.getSubject().getName())
                .branchName(t.getBranch().getName())
                .dayOfWeek(t.getDayOfWeek().name())
                .timeSlot(t.getStartTime() + "–" + t.getEndTime())
                .totalMarked(records.size())
                .present(p).late(l).absent(ab).excused(ex)
                .attendanceRate(round2(rate))
                .build();
    }

    private SubjectPerformanceRow buildSubjectRow(List<ExamResult> results) {
        ExamResult first  = results.get(0);
        UUID subjectId    = first.getSubject() != null ? first.getSubject().getId() : null;
        String subjectName = first.resolveSubjectName();

        double avg     = results.stream().mapToDouble(ExamResult::percentage).average().orElse(0.0);
        double passRate = results.stream().filter(r -> r.percentage() >= 50.0).count() * 100.0 / results.size();

        Map<String, Long> gradeDist = results.stream()
                .collect(Collectors.groupingBy(r -> r.getGrade() != null ? r.getGrade() : "?",
                        Collectors.counting()));

        return SubjectPerformanceRow.builder()
                .subjectId(subjectId).subjectName(subjectName)
                .examCount(results.size())
                .averagePercentage(round2(avg))
                .averageGrade(AcademicResultService.calculateGrade(BigDecimal.valueOf(avg), BigDecimal.valueOf(100)))
                .passRate(round2(passRate))
                .gradeDistribution(gradeDist)
                .build();
    }

    private BigDecimal sumNetAmount(List<FeeRecord> records) {
        return records.stream().map(FeeRecord::getNetAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int count(List<AttendanceRecord> records, AttendanceStatus status) {
        return (int) records.stream().filter(a -> a.getStatus() == status).count();
    }

    private static double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
