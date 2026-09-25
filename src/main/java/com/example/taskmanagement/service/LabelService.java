package com.example.taskmanagement.service;

import com.example.taskmanagement.dto.LabelCreateRequest;
import com.example.taskmanagement.dto.LabelResponse;
import com.example.taskmanagement.exception.DuplicateResourceException;
import com.example.taskmanagement.exception.ResourceNotFoundException;
import com.example.taskmanagement.model.Label;
import com.example.taskmanagement.repository.LabelRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LabelService {

    private static final Logger log = LoggerFactory.getLogger(LabelService.class);

    private final LabelRepository labelRepository;

    public LabelService(LabelRepository labelRepository) {
        this.labelRepository = labelRepository;
    }

    @Transactional(readOnly = true)
    public List<LabelResponse> findAll() {
        return labelRepository.findAll().stream().map(LabelResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public LabelResponse findById(String id) {
        return LabelResponse.from(getLabelOrThrow(id));
    }

    public LabelResponse create(LabelCreateRequest request) {
        if (labelRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Label already exists: " + request.name());
        }
        Label label = Label.builder()
                .name(request.name())
                .color(request.color())
                .build();
        Label saved = labelRepository.saveAndFlush(label);
        log.info("Label created: [{}] '{}'", saved.getId(), saved.getName());
        return LabelResponse.from(saved);
    }

    public void delete(String id) {
        Label label = getLabelOrThrow(id);
        labelRepository.delete(label);
        log.info("Label deleted: [{}] '{}'", id, label.getName());
    }

    private Label getLabelOrThrow(String id) {
        return labelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Label not found with id: " + id));
    }
}
