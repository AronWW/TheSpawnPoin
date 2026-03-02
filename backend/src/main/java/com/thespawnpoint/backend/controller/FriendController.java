package com.thespawnpoint.backend.controller;

import com.thespawnpoint.backend.dto.FriendDTO;
import com.thespawnpoint.backend.dto.FriendRequestDTO;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @GetMapping
    public ResponseEntity<List<FriendDTO>> getFriends(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(friendService.getFriends(currentUser));
    }

    @GetMapping("/requests/incoming")
    public ResponseEntity<List<FriendRequestDTO>> getIncomingRequests(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(friendService.getIncomingRequests(currentUser));
    }

    @GetMapping("/requests/outgoing")
    public ResponseEntity<List<FriendRequestDTO>> getOutgoingRequests(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(friendService.getOutgoingRequests(currentUser));
    }

    @PostMapping("/request/{userId}")
    public ResponseEntity<Void> sendFriendRequest(
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        friendService.sendFriendRequest(currentUser, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/accept/{inviteId}")
    public ResponseEntity<Void> acceptFriendRequest(
            @PathVariable Long inviteId,
            @AuthenticationPrincipal User currentUser) {
        friendService.acceptFriendRequest(currentUser, inviteId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/decline/{inviteId}")
    public ResponseEntity<Void> declineFriendRequest(
            @PathVariable Long inviteId,
            @AuthenticationPrincipal User currentUser) {
        friendService.declineFriendRequest(currentUser, inviteId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/cancel/{inviteId}")
    public ResponseEntity<Void> cancelFriendRequest(
            @PathVariable Long inviteId,
            @AuthenticationPrincipal User currentUser) {
        friendService.cancelFriendRequest(currentUser, inviteId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> unfriend(
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        friendService.unfriend(currentUser, userId);
        return ResponseEntity.ok().build();
    }
}

