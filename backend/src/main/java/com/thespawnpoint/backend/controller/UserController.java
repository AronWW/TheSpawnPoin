package com.thespawnpoint.backend.controller;

import com.thespawnpoint.backend.dto.UserSummaryDTO;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/search")
    public ResponseEntity<List<UserSummaryDTO>> search(
            @RequestParam String q,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(userService.search(q, currentUser));
    }
}
