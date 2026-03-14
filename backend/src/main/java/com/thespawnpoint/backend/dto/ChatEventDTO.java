package com.thespawnpoint.backend.dto;

import lombok.Data;

@Data
public class ChatEventDTO {
    private String type;
    private Long chatId;
    private Object payload;

    public ChatEventDTO(String type, Long chatId, Object payload) {
        this.type = type;
        this.chatId = chatId;
        this.payload = payload;
    }
}

