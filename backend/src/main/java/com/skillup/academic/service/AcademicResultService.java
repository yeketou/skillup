package com.skillup.academic.service;

import com.skillup.academic.domain.ExamResult;
import com.skillup.academic.domain.ExamType;
import com.skillup.academic.dto.*;
import com.skillup.academic.repository.ExamResultRepository;
import com.skillup.branch.domain.Branch;
import com.skillup.branch.repository.BranchRepository;
import com.skillup.classmanagement.domain.ClassTemplate;
import com.skillup.classmanagement.domain.Subject;
import com.skillup.classmanagement.repository.ClassTemplateRepository;
import com.skillup.classmanagement.repository.SubjectRepository;
import com.skillup.common.exception.BusinessException;
import com.skillup.common.exception.ResourceNotFoundException;
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
public class AcademicResultService {

    private final ExamResultRepository  examResultRepository;
    private final StudentRepository     studentRepository;
    private final SubjectRepository     subjectRepository;
    private final ClassTemplateRepository templateRepository;
    private final BranchRepository      branchRepository;

    // ═══════════════════════════════════════════════════════════════════════════
    // RECORD RESULTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public ExamResultResponse recordResult(RecordExamResultRequest req) {
        Student student = studentRepository.findActiveById(req.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", req.getStudentId()));

        ExamResult result = buildResult(
                student,
                req.getSubjectId(), req.getTemplateId(), req.getBranchId(),
                req.getExamName(), req.getSubjectLabel(),
                req.getExamType(), req.getExamDate(),
                req.getScore(), req.getMaxScore() != null ? req.getMaxScore() : new BigDecimal("100"),
                req.getGrade(), req.getNotes()
        );

        ExamResult saved = examResultRepository.save(result);
        log.info("Exam result recorded: student={}, exam='{}', score={}/{}",
                student.getStudentRef(), req.getExamName(), req.getScore(), result.getMaxScore());
        return toResponse(saved);
    }

    @Transactional
    public List<ExamResultResponse> bulkRecordResults(BulkRecordExamResultRequest req) {
        if (req.getScores() == null || req.getScores().isEmpty()) {
            throw new BusinessException("EMPTY_SCORES", "At least one student score is required");
        }

        BigDecimal maxScore = req.getMaxScore();
        ExamType   examType = parseExamType(req.getExamType());

        // Optional shared lookups
        Subject       subject  = resolveSubjectOptional(req.getSubjectId());
        ClassTemplate template = resolveTemplateOptional(req.getTemplateId());
        Branch        branch   = resolveBranchOptional(req.getBranchId());

        List<ExamResultResponse> results = new ArrayList<>();
        for (StudentScoreEntry entry : req.getScores()) {
            Student student = studentRepository.findActiveById(entry.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student", entry.getStudentId()));

            BigDecimal score = entry.getScore();
            if (score.compareTo(maxScore) > 0) {
                throw new BusinessException("SCORE_EXCEEDS_MAX",
                        String.format("Student %s: score %.1f exceeds max score %.1f",
                                student.getStudentRef(), score, maxScore));
            }

            String grade = entry.getGrade() != null
                    ? entry.getGrade()
                    : calculateGrade(score, maxScore);

            ExamResult result = ExamResult.builder()
                    .student(student)
                    .subject(subject)
                    .template(template)
                    .branch(branch)
                    .examName(req.getExamName())
                    .subjectLabel(req.getSubjectLabel())
                    .examType(examType)
                    .examDate(req.getExamDate())
                    .score(score)
                    .maxScore(maxScore)
                    .grade(grade)
                    .notes(entry.getNotes())
                    .build();

            results.add(toResponse(examResultRepository.save(result)));
        }

        log.info("Bulk exam results: exam='{}', {} student(s)", req.getExamName(), results.size());
        return results;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // READ
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ExamResultResponse getById(UUID id) {
        return toResponse(requireResult(id));
    }

    @Transactional(readOnly = true)
    public List<ExamResultResponse> getStudentResults(UUID studentId, UUID subjectId,
                                                       String examTypeStr,
                                                       LocalDate from, LocalDate to) {
        studentRepository.findActiveById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
        ExamType examType = parseExamTypeOrNull(examTypeStr);
        return examResultRepository.findByStudent(studentId, subjectId, examType, from, to)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExamResultResponse> getClassResults(UUID templateId, String examName) {
        templateRepository.findActiveById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("ClassTemplate", templateId));
        return examResultRepository.findByTemplate(templateId, examName)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudentAcademicSummaryResponse getStudentSummary(UUID studentId) {
        Student student = studentRepository.findActiveById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        List<ExamResult> all = examResultRepository.findAllForSummary(studentId);

        // Group by subject key: subjectId (preferred) or subjectLabel string
        Map<String, List<ExamResult>> grouped = new LinkedHashMap<>();
        for (ExamResult r : all) {
            String key = r.getSubject() != null
                    ? "id:" + r.getSubject().getId()
                    : "label:" + (r.getSubjectLabel() != null ? r.getSubjectLabel() : "_unknown");
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        List<SubjectPerformanceResponse> subjects = grouped.values().stream()
                .map(this::toSubjectPerformance)
                .collect(Collectors.toList());

        double overallAvg = all.isEmpty() ? 0.0
                : all.stream().mapToDouble(ExamResult::percentage).average().orElse(0.0);

        return StudentAcademicSummaryResponse.builder()
                .studentId(student.getId())
                .studentRef(student.getStudentRef())
                .studentName(student.getFullName())
                .currentGrade(student.getCurrentGrade())
                .totalExams(all.size())
                .overallAveragePercentage(round2(overallAvg))
                .overallGrade(calculateGradeFromPercentage(overallAvg))
                .subjects(subjects)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public ExamResultResponse updateResult(UUID id, UpdateExamResultRequest req) {
        ExamResult result = requireResult(id);

        if (req.getExamName() != null)  result.setExamName(req.getExamName());
        if (req.getExamDate() != null)  result.setExamDate(req.getExamDate());
        if (req.getNotes()    != null)  result.setNotes(req.getNotes().isBlank() ? null : req.getNotes());

        // If score or maxScore changes, recalculate grade unless an explicit override is provided
        boolean scoreChanged = false;
        if (req.getScore()    != null) { result.setScore(req.getScore());       scoreChanged = true; }
        if (req.getMaxScore() != null) { result.setMaxScore(req.getMaxScore()); scoreChanged = true; }

        if (req.getScore() != null && result.getScore().compareTo(result.getMaxScore()) > 0) {
            throw new BusinessException("SCORE_EXCEEDS_MAX",
                    String.format("Score %.1f exceeds max score %.1f",
                            result.getScore(), result.getMaxScore()));
        }

        if (req.getGrade() != null) {
            result.setGrade(req.getGrade().isBlank() ? calculateGrade(result.getScore(), result.getMaxScore()) : req.getGrade());
        } else if (scoreChanged) {
            result.setGrade(calculateGrade(result.getScore(), result.getMaxScore()));
        }

        ExamResult saved = examResultRepository.save(result);
        log.info("Exam result {} updated", id);
        return toResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GRADING LOGIC — MALAYSIAN SPM SCALE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Malaysian SPM/PT3-aligned grading scale.
     * <pre>
     *  A+ ≥ 90%    A  ≥ 80%    A- ≥ 75%
     *  B+ ≥ 70%    B  ≥ 65%    B- ≥ 60%
     *  C+ ≥ 55%    C  ≥ 50%    C- ≥ 45%
     *  D  ≥ 40%    E  &lt; 40%
     * </pre>
     */
    public static String calculateGrade(BigDecimal score, BigDecimal maxScore) {
        if (maxScore.compareTo(BigDecimal.ZERO) == 0) return "E";
        double pct = score.doubleValue() / maxScore.doubleValue() * 100.0;
        return calculateGradeFromPercentage(pct);
    }

    private static String calculateGradeFromPercentage(double pct) {
        if (pct >= 90) return "A+";
        if (pct >= 80) return "A";
        if (pct >= 75) return "A-";
        if (pct >= 70) return "B+";
        if (pct >= 65) return "B";
        if (pct >= 60) return "B-";
        if (pct >= 55) return "C+";
        if (pct >= 50) return "C";
        if (pct >= 45) return "C-";
        if (pct >= 40) return "D";
        return "E";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS — BUILDERS
    // ═══════════════════════════════════════════════════════════════════════════

    private ExamResult buildResult(Student student,
                                    UUID subjectId, UUID templateId, UUID branchId,
                                    String examName, String subjectLabel,
                                    String examTypeStr, LocalDate examDate,
                                    BigDecimal score, BigDecimal maxScore,
                                    String gradeOverride, String notes) {
        if (score.compareTo(maxScore) > 0) {
            throw new BusinessException("SCORE_EXCEEDS_MAX",
                    String.format("Score %.1f exceeds max score %.1f", score, maxScore));
        }

        String grade = gradeOverride != null && !gradeOverride.isBlank()
                ? gradeOverride
                : calculateGrade(score, maxScore);

        return ExamResult.builder()
                .student(student)
                .subject(resolveSubjectOptional(subjectId))
                .template(resolveTemplateOptional(templateId))
                .branch(resolveBranchOptional(branchId))
                .examName(examName)
                .subjectLabel(subjectLabel)
                .examType(parseExamType(examTypeStr))
                .examDate(examDate)
                .score(score)
                .maxScore(maxScore)
                .grade(grade)
                .notes(notes)
                .build();
    }

    private Subject resolveSubjectOptional(UUID id) {
        if (id == null) return null;
        return subjectRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", id));
    }

    private ClassTemplate resolveTemplateOptional(UUID id) {
        if (id == null) return null;
        return templateRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClassTemplate", id));
    }

    private Branch resolveBranchOptional(UUID id) {
        if (id == null) return null;
        return branchRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", id));
    }

    private ExamResult requireResult(UUID id) {
        return examResultRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExamResult", id));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS — ENUM PARSING
    // ═══════════════════════════════════════════════════════════════════════════

    private ExamType parseExamType(String value) {
        if (value == null || value.isBlank()) return ExamType.INTERNAL_EXAM;
        try { return ExamType.valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_EXAM_TYPE",
                    "Invalid exam type: '" + value + "'. Must be SCHOOL_EXAM, INTERNAL_EXAM, TRIAL_EXAM, or MOCK_EXAM");
        }
    }

    private ExamType parseExamTypeOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try { return ExamType.valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS — MAPPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private ExamResultResponse toResponse(ExamResult r) {
        Student       st = r.getStudent();
        Subject       su = r.getSubject();
        ClassTemplate t  = r.getTemplate();
        Branch        b  = r.getBranch();

        return ExamResultResponse.builder()
                .id(r.getId())
                .studentId(st.getId())
                .studentRef(st.getStudentRef())
                .studentName(st.getFullName())
                .subjectId(su != null ? su.getId() : null)
                .subjectName(r.resolveSubjectName())
                .templateId(t != null ? t.getId() : null)
                .templateName(t != null ? t.getName() : null)
                .branchId(b != null ? b.getId() : null)
                .branchName(b != null ? b.getName() : null)
                .examName(r.getExamName())
                .examType(r.getExamType().name())
                .examDate(r.getExamDate())
                .score(r.getScore())
                .maxScore(r.getMaxScore())
                .percentage(round2(r.percentage()))
                .grade(r.getGrade())
                .notes(r.getNotes())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .recordedBy(r.getCreatedBy())
                .build();
    }

    private SubjectPerformanceResponse toSubjectPerformance(List<ExamResult> results) {
        // Results are already newest-first within this group
        ExamResult latest = results.get(0);

        UUID   subjectId   = latest.getSubject() != null ? latest.getSubject().getId() : null;
        String subjectName = latest.resolveSubjectName();

        double avgPct = results.stream()
                .mapToDouble(ExamResult::percentage)
                .average()
                .orElse(0.0);

        double latestPct = latest.percentage();

        // Trend: compare latest vs second-most-recent
        String trend;
        if (results.size() < 2) {
            trend = "INSUFFICIENT_DATA";
        } else {
            double prevPct = results.get(1).percentage();
            double diff    = latestPct - prevPct;
            if (diff > 3.0)       trend = "IMPROVING";
            else if (diff < -3.0) trend = "DECLINING";
            else                  trend = "STABLE";
        }

        List<ExamResultResponse> recent = results.stream()
                .limit(5)
                .map(this::toResponse)
                .collect(Collectors.toList());

        return SubjectPerformanceResponse.builder()
                .subjectId(subjectId)
                .subjectName(subjectName)
                .resultCount(results.size())
                .averagePercentage(round2(avgPct))
                .averageGrade(calculateGradeFromPercentage(avgPct))
                .latestScore(latest.getScore())
                .latestMaxScore(latest.getMaxScore())
                .latestPercentage(round2(latestPct))
                .latestGrade(latest.getGrade())
                .trend(trend)
                .recentResults(recent)
                .build();
    }

    private static double round2(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
