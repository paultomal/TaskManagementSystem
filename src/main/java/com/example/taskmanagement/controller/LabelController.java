package com.example.taskmanagement.controller;

import com.example.taskmanagement.dto.LabelCreateRequest;
import com.example.taskmanagement.dto.LabelResponse;
import com.example.taskmanagement.service.LabelService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/labels")
public class LabelController {

    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    @GetMapping
    public List<LabelResponse> getAll() {
        return labelService.findAll();
    }

    @GetMapping("/{id}")
    public LabelResponse getById(@PathVariable String id) {
        return labelService.findById(id);
    }

    @PostMapping
    public ResponseEntity<LabelResponse> create(@Valid @RequestBody LabelCreateRequest request) {
        LabelResponse created = labelService.create(request);
        return ResponseEntity.created(URI.create("/api/labels/" + created.id())).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        labelService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
