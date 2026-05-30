package com.skillup.branch.service;

import com.skillup.branch.domain.Branch;
import com.skillup.branch.dto.BranchRequest;
import com.skillup.branch.dto.BranchResponse;
import com.skillup.branch.repository.BranchRepository;
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
public class BranchService {

    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public List<BranchResponse> findAll() {
        return branchRepository.findAllActive()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BranchResponse findById(UUID id) {
        Branch branch = branchRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", id));
        return toResponse(branch);
    }

    @Transactional
    public BranchResponse create(BranchRequest request) {
        String code = request.getCode().toUpperCase();
        if (branchRepository.existsByCodeAndDeletedAtIsNull(code)) {
            throw new BusinessException("DUPLICATE_BRANCH_CODE",
                    "A branch with code '" + code + "' already exists");
        }

        Branch branch = Branch.builder()
                .name(request.getName())
                .code(code)
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .active(true)
                .build();

        branch = branchRepository.save(branch);
        log.info("Branch created: {} ({})", branch.getName(), branch.getId());
        return toResponse(branch);
    }

    @Transactional
    public BranchResponse update(UUID id, BranchRequest request) {
        Branch branch = branchRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", id));

        String newCode = request.getCode().toUpperCase();

        // Check code collision only if code is being changed
        if (!newCode.equals(branch.getCode()) &&
                branchRepository.existsByCodeAndDeletedAtIsNull(newCode)) {
            throw new BusinessException("DUPLICATE_BRANCH_CODE",
                    "A branch with code '" + newCode + "' already exists");
        }

        branch.setName(request.getName());
        branch.setCode(newCode);
        branch.setAddress(request.getAddress());
        branch.setPhone(request.getPhone());
        branch.setEmail(request.getEmail());

        branch = branchRepository.save(branch);
        log.info("Branch updated: {} ({})", branch.getName(), branch.getId());
        return toResponse(branch);
    }

    @Transactional
    public void deactivate(UUID id) {
        Branch branch = branchRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", id));
        branch.setActive(false);
        branchRepository.save(branch);
        log.info("Branch deactivated: {}", id);
    }

    @Transactional
    public void delete(UUID id) {
        Branch branch = branchRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", id));
        branch.softDelete();
        branchRepository.save(branch);
        log.info("Branch soft-deleted: {}", id);
    }

    // ── Mapping ────────────────────────────────────────────────────────────────

    private BranchResponse toResponse(Branch b) {
        return BranchResponse.builder()
                .id(b.getId())
                .name(b.getName())
                .code(b.getCode())
                .address(b.getAddress())
                .phone(b.getPhone())
                .email(b.getEmail())
                .logoUrl(b.getLogoUrl())
                .active(b.isActive())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}
