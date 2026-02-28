package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.ProfileDTO;
import com.thespawnpoint.backend.dto.UpdateProfileDTO;
import com.thespawnpoint.backend.entity.*;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;

    @Value("${app.upload.dir:uploads/avatars}")
    private String uploadDir;

    @Value("${app.upload.max-size:2097152}")
    private long maxFileSize; // 2 MB default

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private static final List<String> DEFAULT_AVATARS = List.of(
            "/avatars/default/avatar-1.png",
            "/avatars/default/avatar-2.png",
            "/avatars/default/avatar-3.png",
            "/avatars/default/avatar-4.png",
            "/avatars/default/avatar-5.png",
            "/avatars/default/avatar-6.png",
            "/avatars/default/avatar-7.png",
            "/avatars/default/avatar-8.png",
            "/avatars/default/avatar-9.png",
            "/avatars/default/avatar-10.png"
    );

    // ----------------------------------------------------------------
    // Get profile
    // ----------------------------------------------------------------

    public ProfileDTO getMyProfile(User user) {
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profile not found"));
        return toDTO(profile, user);
    }

    public ProfileDTO getProfileByUserId(Long userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profile not found"));
        return toDTO(profile, profile.getUser());
    }

    // ----------------------------------------------------------------
    // Update profile
    // ----------------------------------------------------------------

    @Transactional
    public ProfileDTO updateMyProfile(User user, UpdateProfileDTO dto) {
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profile not found"));

        if (dto.getFullName() != null) {
            profile.setFullName(dto.getFullName());
        }
        if (dto.getBio() != null) {
            profile.setBio(dto.getBio());
        }
        if (dto.getBirthDate() != null) {
            profile.setBirthDate(dto.getBirthDate());
        }
        if (dto.getPlatforms() != null) {
            validatePlatforms(dto.getPlatforms());
            profile.setPlatforms(dto.getPlatforms());
        }
        if (dto.getSkillLevel() != null) {
            profile.setSkillLevel(parseEnum(SkillLevel.class, dto.getSkillLevel(), "skill level"));
        }
        if (dto.getPlayStyle() != null) {
            profile.setPlayStyle(parseEnum(PlayStyle.class, dto.getPlayStyle(), "play style"));
        }
        if (dto.getLanguages() != null) {
            profile.setLanguages(dto.getLanguages());
        }
        if (dto.getCountry() != null) {
            profile.setCountry(dto.getCountry());
        }
        if (dto.getRegion() != null) {
            profile.setRegion(dto.getRegion());
        }

        profileRepository.save(profile);
        return toDTO(profile, user);
    }

    // ----------------------------------------------------------------
    // Avatar upload
    // ----------------------------------------------------------------

    @Transactional
    public ProfileDTO uploadAvatar(User user, MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        if (file.getSize() > maxFileSize) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File size exceeds 2 MB limit");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only JPEG, PNG, WebP and GIF are allowed");
        }

        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profile not found"));

        try {
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);

            String ext = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + ext;
            Path filePath = uploadPath.resolve(filename);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            profile.setAvatarUrl("/avatars/" + filename);
            profileRepository.save(profile);

            return toDTO(profile, user);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save avatar");
        }
    }

    // ----------------------------------------------------------------
    // Select default avatar
    // ----------------------------------------------------------------

    @Transactional
    public ProfileDTO selectDefaultAvatar(User user, int index) {
        if (index < 1 || index > DEFAULT_AVATARS.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Avatar index must be between 1 and " + DEFAULT_AVATARS.size());
        }

        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profile not found"));

        profile.setAvatarUrl(DEFAULT_AVATARS.get(index - 1));
        profileRepository.save(profile);

        return toDTO(profile, user);
    }

    // ----------------------------------------------------------------
    // Default avatars list
    // ----------------------------------------------------------------

    public List<String> getDefaultAvatars() {
        return DEFAULT_AVATARS;
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private ProfileDTO toDTO(Profile profile, User user) {
        return ProfileDTO.builder()
                .userId(user.getId())
                .displayName(user.getDisplayName())
                .fullName(profile.getFullName())
                .avatarUrl(profile.getAvatarUrl())
                .bio(profile.getBio())
                .birthDate(profile.getBirthDate())
                .platforms(profile.getPlatforms())
                .skillLevel(profile.getSkillLevel() != null ? profile.getSkillLevel().name() : null)
                .playStyle(profile.getPlayStyle() != null ? profile.getPlayStyle().name() : null)
                .languages(profile.getLanguages())
                .country(profile.getCountry())
                .region(profile.getRegion())
                .build();
    }

    private void validatePlatforms(List<String> platforms) {
        Set<String> valid = Set.of("PC", "PLAYSTATION", "XBOX", "NINTENDO", "MOBILE");
        for (String p : platforms) {
            if (!valid.contains(p.toUpperCase())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Invalid platform: " + p + ". Allowed: " + valid);
            }
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String fieldName) {
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Invalid " + fieldName + ": " + value);
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".jpg";
    }
}

