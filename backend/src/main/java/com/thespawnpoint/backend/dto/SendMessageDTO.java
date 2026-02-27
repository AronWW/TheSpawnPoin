package com.thespawnpoint.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendMessageDTO {

    @NotBlank
    private String recipientEmail;

    @NotBlank
    @Size(max = 5000)
    private String content;
}
