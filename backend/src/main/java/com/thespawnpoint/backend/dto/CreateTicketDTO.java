package com.thespawnpoint.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTicketDTO {

    @NotBlank(message = "Тема обов'язкова")
    private String subject;

    @NotBlank(message = "Повідомлення обов'язкове")
    private String message;
}

