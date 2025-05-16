package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.FriendshipStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Friendship;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.FriendshipRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ch.uzh.ifi.hase.soprafs24.rest.dto.FriendshipStatusDTO;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class FriendshipService {

    private final Logger log = LoggerFactory.getLogger(FriendshipService.class);

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    @Autowired
    public FriendshipService(@Qualifier("friendshipRepository") FriendshipRepository friendshipRepository,
            @Qualifier("userRepository") UserRepository userRepository) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
    }

    /**
     * Send a friend request from sender to receiver.
     */
    public Friendship sendFriendRequest(Long senderId, Long receiverId) {
        if (senderId.equals(receiverId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot send a friend request to yourself.");
        }

        User sender = findUserOrThrow(senderId);
        User receiver = findUserOrThrow(receiverId);

        if (friendshipRepository.findByUserAndFriend(sender, receiver).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Friend request already exists.");
        }

        Friendship friendship = new Friendship(sender, receiver, FriendshipStatus.PENDING);
        friendship = friendshipRepository.save(friendship);
        friendshipRepository.flush();

        log.info("Friend request sent from {} to {}", senderId, receiverId);
        return friendship;
    }

    /**
     * Accept an incoming friend request.
     */
    public void acceptFriendRequest(Long receiverId, Long senderId) {
        User receiver = findUserOrThrow(receiverId);
        User sender = findUserOrThrow(senderId);

        Friendship friendship = friendshipRepository.findByUserAndFriend(sender, receiver)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend request not found."));

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Friend request is not pending.");
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);

        log.info("Friend request accepted by {} from {}", receiverId, senderId);
    }

    /**
     * Decline an incoming friend request.
     */
    public void declineFriendRequest(Long receiverId, Long senderId) {
        User receiver = findUserOrThrow(receiverId);
        User sender = findUserOrThrow(senderId);

        Friendship friendship = friendshipRepository.findByUserAndFriend(sender, receiver)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend request not found."));

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot decline a non-pending request.");
        }

        friendship.setStatus(FriendshipStatus.DECLINED);
        friendshipRepository.save(friendship);

        log.info("Friend request declined by {} from {}", receiverId, senderId);
    }

    /**
     * Remove a friend from either direction.
     */
    public void removeFriend(Long userId, Long friendId) {
        User user = findUserOrThrow(userId);
        User friend = findUserOrThrow(friendId);

        Optional<Friendship> friendshipOpt = friendshipRepository.findByUserAndFriend(user, friend);
        if (friendshipOpt.isEmpty()) {
            friendshipOpt = friendshipRepository.findByUserAndFriend(friend, user);
        }

        // If no friendship exists, return silently
        if (friendshipOpt.isEmpty()) {
            log.info("No friendship existed between {} and {}, nothing to delete.", userId, friendId);
            return;
        }

        friendshipRepository.delete(friendshipOpt.get());
        log.info("Friendship removed between {} and {}", userId, friendId);
    }

    /**
     * Get all accepted friends of a user.
     */
    public List<UserGetDTO> getFriends(Long userId) {
        User user = findUserOrThrow(userId);

        List<Friendship> friendships = friendshipRepository.findAllByUserOrFriend(user, user);

        List<User> friends = friendships.stream()
                .filter(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
                .map(f -> f.getUser().equals(user) ? f.getFriend() : f.getUser())
                .collect(Collectors.toList());

        return friends.stream()
                .map(DTOMapper.INSTANCE::convertEntityToUserGetDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all incoming pending friend requests.
     */
    public List<UserGetDTO> getFriendsPending(Long userId) {
        User user = findUserOrThrow(userId);

        List<Friendship> pendingRequests = friendshipRepository.findAllByFriendAndStatus(user,
                FriendshipStatus.PENDING);

        List<User> requestSenders = pendingRequests.stream()
                .map(Friendship::getUser)
                .collect(Collectors.toList());

        return requestSenders.stream()
                .map(DTOMapper.INSTANCE::convertEntityToUserGetDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all incoming pending friend requests.
     */
    public List<UserGetDTO> getFriendsDeclined(Long userId) {
        User user = findUserOrThrow(userId);

        List<Friendship> declinedRequests = friendshipRepository.findAllByFriendAndStatus(user,
                FriendshipStatus.DECLINED);

        List<User> requestSenders = declinedRequests.stream()
                .map(Friendship::getUser)
                .collect(Collectors.toList());

        return requestSenders.stream()
                .map(DTOMapper.INSTANCE::convertEntityToUserGetDTO)
                .collect(Collectors.toList());
    }

    /**
     * Helper method to fetch user or throw 404.
     */
    private User findUserOrThrow(Long userId) {
        User user = userRepository.findUserById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found.");
        }
        return user;
    }

    public void updateFriendshipStatus(Long userId1, Long userId2, FriendshipStatus newStatus) {
        User user1 = findUserOrThrow(userId1);
        User user2 = findUserOrThrow(userId2);

        Optional<Friendship> friendshipOpt = friendshipRepository.findByUserAndFriend(user1, user2);
        if (friendshipOpt.isEmpty()) {
            friendshipOpt = friendshipRepository.findByUserAndFriend(user2, user1);
        }

        Friendship friendship = friendshipOpt
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friendship not found."));

        friendship.setStatus(newStatus);
        friendshipRepository.save(friendship);
    }

    public FriendshipStatusDTO getFriendshipStatus(Long currentUserId, Long otherUserId) {
        User currentUser = findUserOrThrow(currentUserId);
        User otherUser = findUserOrThrow(otherUserId);

        Optional<Friendship> friendshipOpt = friendshipRepository.findByUserAndFriend(currentUser, otherUser);

        if (friendshipOpt.isEmpty()) {
            // Try reversed direction
            friendshipOpt = friendshipRepository.findByUserAndFriend(otherUser, currentUser);
        }

        FriendshipStatusDTO dto = new FriendshipStatusDTO();

        if (friendshipOpt.isEmpty()) {
            // No friendship exists
            dto.setStatus(FriendshipStatus.UNDEFINED);
            dto.setInitiatedByCurrentUser(false);
            return dto;
        }

        Friendship friendship = friendshipOpt.get();
        dto.setStatus(friendship.getStatus());
        dto.setInitiatedByCurrentUser(friendship.getUser().getId().equals(currentUserId));

        return dto;
    }

}
