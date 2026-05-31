package com.skillup.communication.service;

import com.skillup.common.exception.BusinessException;
import com.skillup.common.exception.ResourceNotFoundException;
import com.skillup.communication.domain.MessageCategory;
import com.skillup.communication.domain.MessageTemplate;
import com.skillup.communication.dto.CreateMessageTemplateRequest;
import com.skillup.communication.dto.MessageTemplateResponse;
import com.skillup.communication.dto.UpdateMessageTemplateRequest;
import com.skillup.communication.repository.MessageTemplateRepository;
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
public class MessageTemplateService {

    private final MessageTemplateRepository templateRepository;

    @Transactional(readOnly = true)
    public List<MessageTemplateResponse> listAll() {
        return templateRepository.findAllActive()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MessageTemplateResponse getById(UUID id) {
        return toResponse(require(id));
    }

    @Transactional
    public MessageTemplateResponse create(CreateMessageTemplateRequest req) {
        if (templateRepository.existsByNameAndDeletedAtIsNull(req.getName())) {
            throw new BusinessException("DUPLICATE_TEMPLATE_NAME",
                    "A template named '" + req.getName() + "' already exists");
        }
        MessageCategory category = parseCategory(req.getCategory());

        MessageTemplate template = MessageTemplate.builder()
                .name(req.getName())
                .category(category)
                .description(req.getDescription())
                .content(req.getContent())
                .waTemplateName(req.getWaTemplateName())
                .build();

        MessageTemplate saved = templateRepository.save(template);
        log.info("Message template created: '{}' ({})", saved.getName(), category);
        return toResponse(saved);
    }

    @Transactional
    public MessageTemplateResponse update(UUID id, UpdateMessageTemplateRequest req) {
        MessageTemplate template = require(id);

        if (req.getName() != null && !req.getName().equals(template.getName())) {
            if (templateRepository.existsByNameAndDeletedAtIsNull(req.getName())) {
                throw new BusinessException("DUPLICATE_TEMPLATE_NAME",
                        "A template named '" + req.getName() + "' already exists");
            }
            template.setName(req.getName());
        }
        if (req.getCategory()        != null) template.setCategory(parseCategory(req.getCategory()));
        if (req.getDescription()     != null) template.setDescription(req.getDescription());
        if (req.getContent()         != null) template.setContent(req.getContent());
        if (req.getWaTemplateName()  != null) template.setWaTemplateName(req.getWaTemplateName());
        if (req.getActive()          != null) template.setActive(req.getActive());

        MessageTemplate saved = templateRepository.save(template);
        log.info("Message template updated: '{}' ({})", saved.getName(), id);
        return toResponse(saved);
    }

    private MessageTemplate require(UUID id) {
        return templateRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MessageTemplate", id));
    }

    private MessageCategory parseCategory(String value) {
        try { return MessageCategory.valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_CATEGORY",
                    "Invalid category: '" + value + "'. Must be FEE_REMINDER, ATTENDANCE_ALERT, ASSIGNMENT_REMINDER, WELCOME, or GENERAL");
        }
    }

    private MessageTemplateResponse toResponse(MessageTemplate t) {
        return MessageTemplateResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .category(t.getCategory().name())
                .description(t.getDescription())
                .content(t.getContent())
                .waTemplateName(t.getWaTemplateName())
                .active(t.isActive())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
