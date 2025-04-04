package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.rest.dto.FriendshipStatusDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MessageDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.service.FriendshipService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/users")
public class FriendshipController {

    private final FriendshipService friendshipService;
    private final UserService userService;

    @Autowired
    public FriendshipController(FriendshipService friendshipService, UserService userService) {
        this.friendshipService = friendshipService;
        this.userService = userService;
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
    }

    // GET /users/me/friends
    @GetMapping("/me/friends")
    public ResponseEntity<List<UserGetDTO>> getMyFriends(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        Long currentUserId = userService.getUserIdFromToken(token);

        List<UserGetDTO> friends = friendshipService.getFriends(currentUserId);
        return ResponseEntity.ok(friends);
    }

    // GET /users/me/friends/pending
    @GetMapping("/me/friends/pending")
    public ResponseEntity<List<UserGetDTO>> getMyPendingFriends(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        Long currentUserId = userService.getUserIdFromToken(token);

        List<UserGetDTO> pendingFriends = friendshipService.getFriendsPending(currentUserId);
        return ResponseEntity.ok(pendingFriends);
    }

    // GET /users/me/friends/declined
    @GetMapping("/me/friends/declined")
    public ResponseEntity<List<UserGetDTO>> getMyDeclinedFriends(@RequestHeader("Authorization") String authHeader) {
        String token = extractToken(authHeader);
        Long currentUserId = userService.getUserIdFromToken(token);

        List<UserGetDTO> declinedFriends = friendshipService.getFriendsDeclined(currentUserId);
        return ResponseEntity.ok(declinedFriends);
    }

    // GET /users/{userId}/friends (public view of another user's friends)
    @GetMapping("/{userId}/friends")
    public ResponseEntity<List<UserGetDTO>> getUserFriends(@PathVariable Long userId) {
        List<UserGetDTO> friends = friendshipService.getFriends(userId);
        return ResponseEntity.ok(friends);
    }

    // POST /users/{userId}/friends (send friend request)
    @PostMapping("/{userId}/friends")
    public ResponseEntity<MessageDTO> sendFriendRequest(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) {

        String token = extractToken(authHeader);
        Long currentUserId = userService.getUserIdFromToken(token);

        friendshipService.sendFriendRequest(currentUserId, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageDTO("Friend request sent."));
    }

    // DELETE /users/{userId}/friends (remove friend)
    @DeleteMapping("/{userId}/friends")
    public ResponseEntity<MessageDTO> removeFriend(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) {

        String token = extractToken(authHeader);
        Long currentUserId = userService.getUserIdFromToken(token);

        friendshipService.removeFriend(currentUserId, userId);
        return ResponseEntity.ok(new MessageDTO("Friend removed."));
    }

    // PUT /users/{userId}/friends (accept/decline friend request)
    @PutMapping("/{userId}/friends")
    public ResponseEntity<MessageDTO> updateFriendship(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody FriendshipStatusDTO statusDTO) {

        String token = extractToken(authHeader);
        Long currentUserId = userService.getUserIdFromToken(token);

        switch (statusDTO.getStatus()) {
            case ACCEPTED:
                friendshipService.acceptFriendRequest(currentUserId, userId);
                return ResponseEntity.ok(new MessageDTO("Friend request accepted."));
            case DECLINED:
                friendshipService.declineFriendRequest(currentUserId, userId);
                return ResponseEntity.ok(new MessageDTO("Friend request declined."));
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid friendship status update.");
        }
    }

    // GET /users/{userId}/friends/status
    @GetMapping("/{userId}/friends/status")
    public ResponseEntity<FriendshipStatusDTO> getFriendshipStatus(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) {

        String token = extractToken(authHeader);
        Long currentUserId = userService.getUserIdFromToken(token);

        FriendshipStatusDTO dto = friendshipService.getFriendshipStatus(currentUserId, userId);
        return ResponseEntity.ok(dto);
    }
}
