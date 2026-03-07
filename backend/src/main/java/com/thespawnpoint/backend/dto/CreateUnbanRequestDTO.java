package com.thespawnpoint.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateUnbanRequestDTO {

    @NotBlank(message = "Причина обов'язкова")
    private String reason;
}

