package com.thespawnpoint.backend.controller;

import com.thespawnpoint.backend.dto.ProfileDTO;
import com.thespawnpoint.backend.dto.UpdateProfileDTO;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    public ResponseEntity<ProfileDTO> getMyProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(profileService.getMyProfile(user));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ProfileDTO> getProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(profileService.getProfileByUserId(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<ProfileDTO> updateMyProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileDTO dto) {
        return ResponseEntity.ok(profileService.updateMyProfile(user, dto));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileDTO> uploadAvatar(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(profileService.uploadAvatar(user, file));
    }

    @PostMapping("/me/avatar/default")
    public ResponseEntity<ProfileDTO> selectDefaultAvatar(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Integer> body) {
        int index = body.getOrDefault("index", 0);
        return ResponseEntity.ok(profileService.selectDefaultAvatar(user, index));
    }

    @GetMapping("/avatars/defaults")
    public ResponseEntity<List<String>> getDefaultAvatars() {
        return ResponseEntity.ok(profileService.getDefaultAvatars());
    }
}

