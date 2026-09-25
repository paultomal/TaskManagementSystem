package com.example.taskmanagement.repository;

import com.example.taskmanagement.model.Label;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelRepository extends JpaRepository<Label, String> {

    boolean existsByNameIgnoreCase(String name);

    Optional<Label> findByNameIgnoreCase(String name);
}
