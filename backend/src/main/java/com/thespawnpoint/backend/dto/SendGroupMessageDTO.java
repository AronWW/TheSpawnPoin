package com.thespawnpoint.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendGroupMessageDTO {

    @NotNull
    private Long chatId;

    @NotBlank
    @Size(max = 5000)
    private String content;

    private Long replyToId;
}

