package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.ProfileDTO;
import com.thespawnpoint.backend.dto.UpdateProfileDTO;
import com.thespawnpoint.backend.entity.user.*;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.repository.ProfileRepository;
import com.thespawnpoint.backend.repository.UserRepository;
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
    private final UserRepository userRepository;

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

    public ProfileDTO getMyProfile(User user) {
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profile not found"));
        return toDTO(profile, user);
    }

    public ProfileDTO getProfileByUserId(Long userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profile not found"));

        if (profile.getUser().getRole() == com.thespawnpoint.backend.entity.user.Role.ADMIN) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Profile not found");
        }

        return toDTO(profile, profile.getUser());
    }

    @Transactional
    public ProfileDTO updateMyProfile(User user, UpdateProfileDTO dto) {
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profile not found"));

        if (dto.getDisplayName() != null && !dto.getDisplayName().isBlank()) {
            String newName = dto.getDisplayName().trim();
            if (!newName.equalsIgnoreCase(user.getDisplayName())) {
                if (userRepository.existsByDisplayNameIgnoreCase(newName)) {
                    throw new ApiException(HttpStatus.CONFLICT, "Display name already taken");
                }
                user.setDisplayName(newName);
                userRepository.save(user);
            }
        }

        if (dto.getFullName() != null)   profile.setFullName(dto.getFullName());
        if (dto.getBio() != null)        profile.setBio(dto.getBio());
        if (dto.getBirthDate() != null)  profile.setBirthDate(dto.getBirthDate());
        if (dto.getCountry() != null)    profile.setCountry(dto.getCountry());
        if (dto.getDiscord() != null)    profile.setDiscord(dto.getDiscord().isBlank() ? null : dto.getDiscord().trim());
        if (dto.getSteam() != null)      profile.setSteam(dto.getSteam().isBlank() ? null : dto.getSteam().trim());
        if (dto.getTwitch() != null)     profile.setTwitch(dto.getTwitch().isBlank() ? null : dto.getTwitch().trim());
        if (dto.getXbox() != null)       profile.setXbox(dto.getXbox().isBlank() ? null : dto.getXbox().trim());
        if (dto.getPlaystation() != null) profile.setPlaystation(dto.getPlaystation().isBlank() ? null : dto.getPlaystation().trim());
        if (dto.getNintendo() != null)   profile.setNintendo(dto.getNintendo().isBlank() ? null : dto.getNintendo().trim());

        if (dto.getPlatforms() != null) {
            validatePlatforms(dto.getPlatforms());
            profile.setPlatforms(dto.getPlatforms());
        }
        if (dto.getSkillLevel() != null) {
            profile.setSkillLevel(
                    dto.getSkillLevel().isBlank() ? null
                            : parseEnum(SkillLevel.class, dto.getSkillLevel(), "skill level")
            );
        }
        if (dto.getPlayStyle() != null) {
            profile.setPlayStyle(
                    dto.getPlayStyle().isBlank() ? null
                            : parseEnum(PlayStyle.class, dto.getPlayStyle(), "play style")
            );
        }
        if (dto.getRegion() != null) {
            profile.setRegion(
                    dto.getRegion().isBlank() ? null
                            : parseEnum(Region.class, dto.getRegion(), "region")
            );
        }
        if (dto.getLanguages() != null) {
            validateLanguages(dto.getLanguages());
            profile.setLanguages(dto.getLanguages());
        }

        profileRepository.save(profile);
        return toDTO(profile, user);
    }

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

    public List<String> getDefaultAvatars() {
        return DEFAULT_AVATARS;
    }

    private ProfileDTO toDTO(Profile profile, User user) {
        return ProfileDTO.builder()
                .userId(user.getId())
                .email(user.getEmail())
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
                .region(profile.getRegion() != null ? profile.getRegion().name() : null)
                .discord(profile.getDiscord())
                .steam(profile.getSteam())
                .twitch(profile.getTwitch())
                .xbox(profile.getXbox())
                .playstation(profile.getPlaystation())
                .nintendo(profile.getNintendo())
                .build();
    }

    private void validateLanguages(java.util.List<String> languages) {
        for (String lang : languages) {
            try {
                Language.valueOf(lang.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Invalid language code: " + lang + ". Allowed: " + java.util.Arrays.toString(Language.values()));
            }
        }
    }

    private void validatePlatforms(List<String> platforms) {
        for (String p : platforms) {
            try {
                Platform.valueOf(p.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Invalid platform: " + p + ". Allowed: " + java.util.Arrays.toString(Platform.values()));
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