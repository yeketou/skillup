package com.skillup.classmanagement.service;

import com.skillup.classmanagement.domain.Subject;
import com.skillup.classmanagement.dto.SubjectRequest;
import com.skillup.classmanagement.dto.SubjectResponse;
import com.skillup.classmanagement.repository.SubjectRepository;
import com.skillup.common.exception.BusinessException;
import com.skillup.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public SubjectResponse create(SubjectRequest req) {
        String code = req.getCode().toUpperCase();
        if (subjectRepository.existsByCodeAndDeletedAtIsNull(code)) {
            throw new BusinessException("DUPLICATE_SUBJECT_CODE",
                    "A subject with code '" + code + "' already exists");
        }

        Subject subject = Subject.builder()
                .name(req.getName())
                .code(code)
                .description(req.getDescription())
                .active(req.getActive() == null || req.getActive())
                .build();

        Subject saved = subjectRepository.save(subject);
        log.info("Subject created: {} ({})", saved.getName(), saved.getCode());
        return toResponse(saved);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SubjectResponse findById(UUID id) {
        return toResponse(requireActive(id));
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> findAll(boolean activeOnly) {
        List<Subject> subjects = activeOnly
                ? subjectRepository.findAllActiveAndEnabled()
                : subjectRepository.findAllActive();
        return subjects.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public SubjectResponse update(UUID id, SubjectRequest req) {
        Subject subject = requireActive(id);
        String code = req.getCode().toUpperCase();

        // Check code uniqueness only if it changed
        if (!code.equals(subject.getCode()) && subjectRepository.existsByCodeAndDeletedAtIsNull(code)) {
            throw new BusinessException("DUPLICATE_SUBJECT_CODE",
                    "A subject with code '" + code + "' already exists");
        }

        subject.setName(req.getName());
        subject.setCode(code);
        if (req.getDescription() != null) subject.setDescription(req.getDescription());
        if (req.getActive()      != null) subject.setActive(req.getActive());

        Subject saved = subjectRepository.save(subject);
        log.info("Subject updated: {}", id);
        return toResponse(saved);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Transactional
    public void delete(UUID id) {
        Subject subject = requireActive(id);
        subject.softDelete();
        subjectRepository.save(subject);
        log.info("Subject soft-deleted: {}", id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Subject requireActive(UUID id) {
        return subjectRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", id));
    }

    SubjectResponse toResponse(Subject s) {
        return SubjectResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .code(s.getCode())
                .description(s.getDescription())
                .active(s.isActive())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
