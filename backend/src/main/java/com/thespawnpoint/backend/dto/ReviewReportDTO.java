package com.thespawnpoint.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewReportDTO {

    @NotBlank(message = "Статус обов'язковий")
    private String status;

    private String adminComment;
}

