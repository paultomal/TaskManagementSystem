package com.example.taskmanagement.dto;

import com.example.taskmanagement.model.Label;

public record LabelResponse(String id, String name, String color) {

    public static LabelResponse from(Label label) {
        return new LabelResponse(label.getId(), label.getName(), label.getColor());
    }
}
