package com.thespawnpoint.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateReportDTO {

    @NotNull(message = "ID користувача обов'язковий")
    private Long reportedUserId;

    @NotBlank(message = "Причина обов'язкова")
    private String reason;

    private String description;
}

