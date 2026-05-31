package com.skillup.attendance.service;

import com.skillup.attendance.domain.AttendanceRecord;
import com.skillup.attendance.domain.AttendanceStatus;
import com.skillup.attendance.dto.*;
import com.skillup.attendance.repository.AttendanceRepository;
import com.skillup.classmanagement.domain.ClassEnrollment;
import com.skillup.classmanagement.domain.ClassSession;
import com.skillup.classmanagement.domain.ClassTemplate;
import com.skillup.classmanagement.domain.EnrollmentStatus;
import com.skillup.classmanagement.repository.ClassEnrollmentRepository;
import com.skillup.classmanagement.repository.ClassSessionRepository;
import com.skillup.common.exception.BusinessException;
import com.skillup.common.exception.ResourceNotFoundException;
import com.skillup.communication.service.NotificationService;
import com.skillup.student.domain.Student;
import com.skillup.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository     attendanceRepository;
    private final ClassSessionRepository   sessionRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final StudentRepository        studentRepository;
    private final NotificationService      notificationService;

    // ═══════════════════════════════════════════════════════════════════════════
    // SESSION ATTENDANCE SHEET
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get the full attendance sheet for a session.
     * Returns ALL actively enrolled students, merged with any existing attendance records.
     * Students not yet marked have status = null.
     */
    @Transactional(readOnly = true)
    public SessionAttendanceResponse getSessionAttendance(UUID sessionId) {
        ClassSession session = requireSession(sessionId);
        return buildAttendanceSheet(session);
    }

    /**
     * Bulk upsert attendance marks for a session.
     * Each record in the request is created if new, or updated if it already exists.
     * Returns the full updated attendance sheet.
     */
    @Transactional
    public SessionAttendanceResponse markAttendance(UUID sessionId, BulkAttendanceRequest req) {
        ClassSession session = requireSession(sessionId);

        for (AttendanceMarkRequest mark : req.getRecords()) {
            AttendanceStatus status = parseStatus(mark.getStatus());

            Student student = studentRepository.findActiveById(mark.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student", mark.getStudentId()));

            // Upsert: update existing or create new
            AttendanceRecord record = attendanceRepository
                    .findBySessionIdAndStudentIdAndDeletedAtIsNull(sessionId, mark.getStudentId())
                    .orElseGet(() -> AttendanceRecord.builder()
                            .session(session)
                            .student(student)
                            .build());

            record.setStatus(status);
            record.setNotes(mark.getNotes());
            AttendanceRecord saved = attendanceRepository.save(record);

            if (status == AttendanceStatus.ABSENT) {
                try {
                    notificationService.sendAttendanceAbsentAlert(saved);
                } catch (Exception e) {
                    log.warn("Failed to send absent alert for student {}: {}",
                            student.getStudentRef(), e.getMessage());
                }
            }
        }

        log.info("Marked attendance for {} student(s) in session {} ({})",
                req.getRecords().size(), sessionId, session.getSessionDate());
        return buildAttendanceSheet(session);
    }

    /**
     * Update a single student's attendance mark in a session.
     */
    @Transactional
    public AttendanceRecordResponse updateStudentMark(UUID sessionId, UUID studentId,
                                                      AttendanceMarkRequest req) {
        ClassSession session = requireSession(sessionId);

        Student student = studentRepository.findActiveById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        AttendanceStatus status = parseStatus(req.getStatus());

        AttendanceRecord record = attendanceRepository
                .findBySessionIdAndStudentIdAndDeletedAtIsNull(sessionId, studentId)
                .orElseGet(() -> AttendanceRecord.builder()
                        .session(session)
                        .student(student)
                        .build());

        record.setStatus(status);
        record.setNotes(req.getNotes());
        AttendanceRecord saved = attendanceRepository.save(record);

        if (status == AttendanceStatus.ABSENT) {
            try {
                notificationService.sendAttendanceAbsentAlert(saved);
            } catch (Exception e) {
                log.warn("Failed to send absent alert for student {}: {}",
                        studentId, e.getMessage());
            }
        }

        log.info("Updated attendance: student {} session {} → {}",
                studentId, sessionId, status);
        return toRecordResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STUDENT HISTORY & SUMMARY
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Full attendance history for a student, optionally filtered by date range.
     */
    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> getStudentHistory(UUID studentId,
                                                            LocalDate from,
                                                            LocalDate to) {
        studentRepository.findActiveById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        List<AttendanceRecord> records = (from != null && to != null)
                ? attendanceRepository.findByStudentIdAndDateRange(studentId, from, to)
                : attendanceRepository.findByStudentId(studentId);

        return records.stream().map(this::toRecordResponse).collect(Collectors.toList());
    }

    /**
     * Attendance summary for a student: overall stats + per-class breakdown.
     * Attendance rate = (PRESENT + LATE) / total marked sessions × 100.
     */
    @Transactional(readOnly = true)
    public StudentAttendanceSummaryResponse getStudentSummary(UUID studentId) {
        Student student = studentRepository.findActiveById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        List<AttendanceRecord> allRecords = attendanceRepository.findByStudentId(studentId);

        // Group by class template
        Map<UUID, List<AttendanceRecord>> byTemplate = allRecords.stream()
                .collect(Collectors.groupingBy(r -> r.getSession().getTemplate().getId()));

        List<ClassAttendanceSummary> perClass = byTemplate.entrySet().stream()
                .map(entry -> buildClassSummary(entry.getValue()))
                .sorted(Comparator.comparing(ClassAttendanceSummary::getTemplateName))
                .collect(Collectors.toList());

        // Overall totals
        int total   = allRecords.size();
        int present = count(allRecords, AttendanceStatus.PRESENT);
        int absent  = count(allRecords, AttendanceStatus.ABSENT);
        int late    = count(allRecords, AttendanceStatus.LATE);
        int excused = count(allRecords, AttendanceStatus.EXCUSED);
        double rate = attendanceRate(present, late, total);

        return StudentAttendanceSummaryResponse.builder()
                .studentId(student.getId())
                .studentRef(student.getStudentRef())
                .studentName(student.getFullName())
                .totalSessions(total)
                .present(present)
                .absent(absent)
                .late(late)
                .excused(excused)
                .attendanceRate(rate)
                .perClass(perClass)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private ClassSession requireSession(UUID sessionId) {
        return sessionRepository.findActiveById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("ClassSession", sessionId));
    }

    private AttendanceStatus parseStatus(String value) {
        try {
            return AttendanceStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_ATTENDANCE_STATUS",
                    "Invalid status: '" + value + "'. Must be PRESENT, ABSENT, LATE, or EXCUSED");
        }
    }

    /** Build the full attendance sheet: enrolled students merged with their marks. */
    private SessionAttendanceResponse buildAttendanceSheet(ClassSession session) {
        // All active enrollments for this class
        List<ClassEnrollment> enrollments = enrollmentRepository
                .findByTemplateId(session.getTemplate().getId())
                .stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .collect(Collectors.toList());

        // Existing attendance records indexed by studentId
        Map<UUID, AttendanceRecord> recordMap = attendanceRepository
                .findBySessionId(session.getId())
                .stream()
                .collect(Collectors.toMap(r -> r.getStudent().getId(), r -> r));

        // Merge: one row per enrolled student
        List<AttendanceRecordResponse> rows = enrollments.stream()
                .map(e -> {
                    Student s = e.getStudent();
                    AttendanceRecord r = recordMap.get(s.getId());
                    return AttendanceRecordResponse.builder()
                            .id(r != null ? r.getId() : null)
                            .sessionId(session.getId())
                            .sessionDate(session.getSessionDate())
                            .startTime(session.getStartTime())
                            .studentId(s.getId())
                            .studentRef(s.getStudentRef())
                            .studentName(s.getFullName())
                            .status(r != null ? r.getStatus().name() : null)
                            .notes(r != null ? r.getNotes() : null)
                            .markedAt(r != null ? r.getUpdatedAt() : null)
                            .build();
                })
                .sorted(Comparator.comparing(AttendanceRecordResponse::getStudentName))
                .collect(Collectors.toList());

        // Summary counts
        long present  = rows.stream().filter(r -> "PRESENT".equals(r.getStatus())).count();
        long absent   = rows.stream().filter(r -> "ABSENT".equals(r.getStatus())).count();
        long late     = rows.stream().filter(r -> "LATE".equals(r.getStatus())).count();
        long excused  = rows.stream().filter(r -> "EXCUSED".equals(r.getStatus())).count();
        long unmarked = rows.stream().filter(r -> r.getStatus() == null).count();

        ClassTemplate t = session.getTemplate();
        return SessionAttendanceResponse.builder()
                .sessionId(session.getId())
                .sessionDate(session.getSessionDate())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .templateId(t.getId())
                .templateName(t.getName())
                .subjectName(t.getSubject().getName())
                .sessionStatus(session.getStatus().name())
                .totalEnrolled((int) enrollments.size())
                .present((int) present)
                .absent((int) absent)
                .late((int) late)
                .excused((int) excused)
                .unmarked((int) unmarked)
                .records(rows)
                .build();
    }

    private ClassAttendanceSummary buildClassSummary(List<AttendanceRecord> records) {
        ClassTemplate t = records.get(0).getSession().getTemplate();
        int total   = records.size();
        int present = count(records, AttendanceStatus.PRESENT);
        int absent  = count(records, AttendanceStatus.ABSENT);
        int late    = count(records, AttendanceStatus.LATE);
        int excused = count(records, AttendanceStatus.EXCUSED);
        return ClassAttendanceSummary.builder()
                .templateId(t.getId())
                .templateName(t.getName())
                .subjectName(t.getSubject().getName())
                .dayOfWeek(t.getDayOfWeek().name())
                .totalSessions(total)
                .present(present)
                .absent(absent)
                .late(late)
                .excused(excused)
                .attendanceRate(attendanceRate(present, late, total))
                .build();
    }

    private AttendanceRecordResponse toRecordResponse(AttendanceRecord r) {
        Student s      = r.getStudent();
        ClassSession cs = r.getSession();
        return AttendanceRecordResponse.builder()
                .id(r.getId())
                .sessionId(cs.getId())
                .sessionDate(cs.getSessionDate())
                .startTime(cs.getStartTime())
                .studentId(s.getId())
                .studentRef(s.getStudentRef())
                .studentName(s.getFullName())
                .status(r.getStatus().name())
                .notes(r.getNotes())
                .markedAt(r.getUpdatedAt())
                .build();
    }

    private static int count(List<AttendanceRecord> records, AttendanceStatus status) {
        return (int) records.stream().filter(r -> r.getStatus() == status).count();
    }

    private static double attendanceRate(int present, int late, int total) {
        if (total == 0) return 0.0;
        double rate = (double) (present + late) / total * 100.0;
        return Math.round(rate * 10.0) / 10.0;  // 1 decimal place
    }
}
