package com.thespawnpoint.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateGroupChatDTO {

    @NotBlank
    @Size(max = 100)
    private String title;

    @NotEmpty
    @Size(min = 1, max = 49, message = "Group chat must have between 1 and 49 other members")
    private List<String> memberEmails;
}

