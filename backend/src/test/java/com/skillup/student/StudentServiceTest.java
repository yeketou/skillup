package com.skillup.student;

import com.skillup.branch.domain.Branch;
import com.skillup.branch.repository.BranchRepository;
import com.skillup.common.exception.BusinessException;
import com.skillup.common.exception.ResourceNotFoundException;
import com.skillup.student.domain.*;
import com.skillup.student.dto.*;
import com.skillup.student.repository.*;
import com.skillup.student.service.StudentService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudentService unit tests")
class StudentServiceTest {

    @Mock StudentRepository          studentRepository;
    @Mock StudentParentRepository    parentRepository;
    @Mock StudentPortalAccountRepository portalAccountRepository;
    @Mock BranchRepository           branchRepository;
    @Mock EntityManager              entityManager;

    @InjectMocks
    StudentService studentService;

    private Branch testBranch;

    @BeforeEach
    void setUp() {
        testBranch = Branch.builder()
                .name("Main Branch")
                .code("HQ")
                .build();
        // Manually set the UUID (normally done by JPA)
        try {
            var idField = com.skillup.common.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testBranch, UUID.randomUUID());
        } catch (Exception ignored) {}
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create student — happy path generates STU ref and saves")
    void createStudent_happyPath() {
        // Arrange
        CreateStudentRequest req = new CreateStudentRequest();
        req.setFullName("Ahmad Bin Ali");
        req.setBranchId(testBranch.getId());
        req.setEnrolledDate(LocalDate.now());

        ParentRequest parent = new ParentRequest();
        parent.setFullName("Ali Bin Abu");
        parent.setPhone("60123456789");
        parent.setEmail("ali@example.com");
        parent.setPrimary(true);
        req.setParents(List.of(parent));

        when(branchRepository.findActiveById(testBranch.getId()))
                .thenReturn(Optional.of(testBranch));
        when(entityManager.createNativeQuery(anyString()))
                .thenReturn(mock(jakarta.persistence.Query.class));

        // Simulate sequence returning 1
        var mockQuery = mock(jakarta.persistence.Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.getSingleResult()).thenReturn(1L);

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        when(studentRepository.save(captor.capture())).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            // Simulate JPA setting the ID
            try {
                var idField = com.skillup.common.BaseEntity.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(s, UUID.randomUUID());
                var createdAtField = com.skillup.common.BaseEntity.class.getDeclaredField("createdAt");
                createdAtField.setAccessible(true);
                createdAtField.set(s, java.time.OffsetDateTime.now());
                var updatedAtField = com.skillup.common.BaseEntity.class.getDeclaredField("updatedAt");
                updatedAtField.setAccessible(true);
                updatedAtField.set(s, java.time.OffsetDateTime.now());
            } catch (Exception ignored) {}
            return s;
        });
        // First call: account not found → triggers creation
        // Second call (inside toResponse): account now exists → portalAccountExists = true
        StudentPortalAccount savedPortalAccount = StudentPortalAccount.builder()
                .email("ali@example.com")
                .fullName("Ali Bin Abu")
                .active(true)
                .build();
        when(portalAccountRepository.findActiveByEmail(anyString()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(savedPortalAccount));
        when(portalAccountRepository.save(any())).thenReturn(savedPortalAccount);

        // Act
        StudentResponse response = studentService.create(req);

        // Assert
        assertThat(response.getFullName()).isEqualTo("Ahmad Bin Ali");
        assertThat(response.getStudentRef()).startsWith("STU-");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getBranchName()).isEqualTo("Main Branch");
        assertThat(response.getParents()).hasSize(1);
        assertThat(response.isPortalAccountExists()).isTrue();

        verify(studentRepository).save(any(Student.class));
        // save fires twice: once to create the account, once to link the student
        verify(portalAccountRepository, times(2)).save(any(StudentPortalAccount.class));
    }

    @Test
    @DisplayName("create student — fails when branch not found")
    void createStudent_branchNotFound() {
        CreateStudentRequest req = new CreateStudentRequest();
        req.setFullName("Test Student");
        req.setBranchId(UUID.randomUUID());

        ParentRequest parent = new ParentRequest();
        parent.setFullName("Parent");
        parent.setPhone("60111111111");
        parent.setPrimary(true);
        req.setParents(List.of(parent));

        when(branchRepository.findActiveById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.create(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Branch");
    }

    @Test
    @DisplayName("create student — fails when no primary parent marked")
    void createStudent_noPrimaryParent() {
        CreateStudentRequest req = new CreateStudentRequest();
        req.setFullName("Test Student");
        req.setBranchId(testBranch.getId());

        ParentRequest parent = new ParentRequest();
        parent.setFullName("Parent");
        parent.setPhone("60111111111");
        parent.setPrimary(false);   // ← not primary!
        req.setParents(List.of(parent));

        when(branchRepository.findActiveById(testBranch.getId()))
                .thenReturn(Optional.of(testBranch));

        assertThatThrownBy(() -> studentService.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("primary");
    }

    // ── Status changes ────────────────────────────────────────────────────────

    @Test
    @DisplayName("changeStatus — GRADUATED sets graduated_date automatically")
    void changeStatus_graduatedSetsDate() {
        Student student = Student.builder()
                .fullName("Ahmad Bin Ali")
                .studentRef("STU-2026-0001")
                .status(StudentStatus.ACTIVE)
                .branch(testBranch)
                .enrolledDate(LocalDate.now())
                .build();
        try {
            var idField = com.skillup.common.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(student, UUID.randomUUID());
            var createdAtField = com.skillup.common.BaseEntity.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(student, java.time.OffsetDateTime.now());
            var updatedAtField = com.skillup.common.BaseEntity.class.getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(student, java.time.OffsetDateTime.now());
        } catch (Exception ignored) {}

        when(studentRepository.findActiveById(student.getId())).thenReturn(Optional.of(student));
        when(studentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StudentStatusRequest req = new StudentStatusRequest();
        req.setStatus("GRADUATED");
        req.setReason("Completed Form 5");

        StudentResponse response = studentService.changeStatus(student.getId(), req);

        assertThat(response.getStatus()).isEqualTo("GRADUATED");
        assertThat(response.getGraduatedDate()).isNotNull();
        assertThat(response.getNotes()).contains("Completed Form 5");
    }

    @Test
    @DisplayName("changeStatus — invalid status throws BusinessException")
    void changeStatus_invalidStatus() {
        Student student = Student.builder()
                .fullName("Ahmad")
                .studentRef("STU-2026-0001")
                .status(StudentStatus.ACTIVE)
                .branch(testBranch)
                .enrolledDate(LocalDate.now())
                .build();
        try {
            var idField = com.skillup.common.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(student, UUID.randomUUID());
        } catch (Exception ignored) {}

        when(studentRepository.findActiveById(any())).thenReturn(Optional.of(student));

        StudentStatusRequest req = new StudentStatusRequest();
        req.setStatus("EXPELLED");  // not a valid status

        assertThatThrownBy(() -> studentService.changeStatus(student.getId(), req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid status");
    }

    // ── Soft delete ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete — soft-deletes by setting deleted_at")
    void deleteStudent_setsDeletedAt() {
        Student student = Student.builder()
                .fullName("Ahmad")
                .studentRef("STU-2026-0001")
                .status(StudentStatus.ACTIVE)
                .branch(testBranch)
                .enrolledDate(LocalDate.now())
                .build();
        try {
            var idField = com.skillup.common.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(student, UUID.randomUUID());
        } catch (Exception ignored) {}

        when(studentRepository.findActiveById(student.getId())).thenReturn(Optional.of(student));
        when(studentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        studentService.delete(student.getId());

        assertThat(student.getDeletedAt()).isNotNull();
        verify(studentRepository).save(student);
    }
}
