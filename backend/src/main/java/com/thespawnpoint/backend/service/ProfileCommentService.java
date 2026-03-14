package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.ProfileCommentDTO;
import com.thespawnpoint.backend.entity.user.Profile;
import com.thespawnpoint.backend.entity.user.ProfileComment;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.repository.ProfileCommentRepository;
import com.thespawnpoint.backend.repository.ProfileRepository;
import com.thespawnpoint.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileCommentService {

    private final ProfileCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    public Page<ProfileCommentDTO> getComments(Long profileUserId, int page, int size) {
        return commentRepository
                .findByProfileUserIdOrderByCreatedAtDesc(profileUserId, PageRequest.of(page, size))
                .map(this::toDTO);
    }

    @Transactional
    public ProfileCommentDTO addComment(User author, Long profileUserId, String content) {
        if (content == null || content.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Comment cannot be empty");
        }
        if (content.length() > 1000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Comment is too long (max 1000 characters)");
        }

        User profileOwner = userRepository.findById(profileUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        ProfileComment comment = ProfileComment.builder()
                .profileUser(profileOwner)
                .author(author)
                .content(content.trim())
                .build();

        comment = commentRepository.save(comment);
        return toDTO(comment);
    }

    @Transactional
    public void deleteComment(User user, Long commentId) {
        ProfileComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found"));

        boolean isAuthor = comment.getAuthor().getId().equals(user.getId());
        boolean isProfileOwner = comment.getProfileUser().getId().equals(user.getId());

        if (!isAuthor && !isProfileOwner) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only delete your own comments or comments on your profile");
        }

        commentRepository.delete(comment);
    }

    private ProfileCommentDTO toDTO(ProfileComment c) {
        Profile authorProfile = profileRepository.findByUserId(c.getAuthor().getId()).orElse(null);
        return ProfileCommentDTO.builder()
                .id(c.getId())
                .profileUserId(c.getProfileUser().getId())
                .authorId(c.getAuthor().getId())
                .authorDisplayName(c.getAuthor().getDisplayName())
                .authorAvatarUrl(authorProfile != null ? authorProfile.getAvatarUrl() : null)
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .build();
    }
}

