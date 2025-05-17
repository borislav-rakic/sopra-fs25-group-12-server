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
     * Sends a friend request from one user to another.
     * Validates that the sender and receiver are different users, and that no
     * existing
     * friendship or pending request already exists between them.
     * Persists the new {@link Friendship} with status {@code PENDING}.
     *
     * @param senderId   the ID of the user sending the friend request
     * @param receiverId the ID of the user receiving the friend request
     * @return the created Friendship entity representing the pending friend request
     * @throws ResponseStatusException if:
     *                                 - the sender and receiver are the same user
     *                                 ({@code BAD_REQUEST}),
     *                                 - either user is not found
     *                                 ({@code NOT_FOUND}),
     *                                 - a friend request already exists between the
     *                                 users ({@code CONFLICT})
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
     * Accepts a pending friend request from one user to another.
     * Validates that the friend request exists and is in the {@code PENDING} state
     * before marking it as {@code ACCEPTED} and saving the change.
     *
     * @param receiverId the ID of the user accepting the friend request
     * @param senderId   the ID of the user who originally sent the friend request
     * @throws ResponseStatusException if:
     *                                 - either user is not found
     *                                 ({@code NOT_FOUND}),
     *                                 - no friend request exists from the sender to
     *                                 the receiver ({@code NOT_FOUND}),
     *                                 - the request is not in a {@code PENDING}
     *                                 state ({@code BAD_REQUEST})
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
     * Declines a pending friend request sent by one user to another.
     * Ensures the friend request exists and is in the {@code PENDING} state before
     * updating
     * its status to {@code DECLINED}.
     *
     * @param receiverId the ID of the user declining the friend request
     * @param senderId   the ID of the user who originally sent the friend request
     * @throws ResponseStatusException if:
     *                                 - either user is not found
     *                                 ({@code NOT_FOUND}),
     *                                 - no pending friend request exists from the
     *                                 sender to the receiver ({@code NOT_FOUND}),
     *                                 - the request is not in a {@code PENDING}
     *                                 state ({@code BAD_REQUEST})
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
     * Removes an existing friendship between two users, regardless of who initiated
     * it.
     * If no friendship exists, the method logs the event and completes silently.
     *
     * @param userId   the ID of one user
     * @param friendId the ID of the other user
     * @throws ResponseStatusException if either user is not found
     *                                 ({@code NOT_FOUND})
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
     * Retrieves a list of accepted friends for the given user.
     * Searches for all friendships where the user is either the sender or receiver,
     * filters to those with status {@code ACCEPTED}, and maps the other party to a
     * DTO.
     *
     * @param userId the ID of the user whose friends are being retrieved
     * @return a list of {@link UserGetDTO} representing the user's friends
     * @throws ResponseStatusException if the user is not found ({@code NOT_FOUND})
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
     * Retrieves all pending friend requests received by the given user.
     * Filters friendships where the user is the recipient and the status is
     * {@code PENDING},
     * then returns the list of users who sent those requests.
     *
     * @param userId the ID of the user receiving pending friend requests
     * @return a list of {@link UserGetDTO} representing users who sent friend
     *         requests
     * @throws ResponseStatusException if the user is not found ({@code NOT_FOUND})
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
     * Retrieves all friend requests that were declined by the given user.
     * Searches for friendships where the user is the recipient and the status is
     * {@code DECLINED},
     * then returns the list of users who originally sent those requests.
     *
     * @param userId the ID of the user who declined the friend requests
     * @return a list of {@link UserGetDTO} representing users whose requests were
     *         declined
     * @throws ResponseStatusException if the user is not found ({@code NOT_FOUND})
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
     * Retrieves a user by their ID or throws an exception if the user does not
     * exist.
     *
     * @param userId the ID of the user to retrieve
     * @return the {@link User} entity corresponding to the given ID
     * @throws ResponseStatusException if no user with the given ID is found
     *                                 ({@code NOT_FOUND})
     */
    private User findUserOrThrow(Long userId) {
        User user = userRepository.findUserById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found.");
        }
        return user;
    }

    /**
     * Updates the status of an existing friendship between two users.
     * Searches for the friendship in either direction, and if found, sets the new
     * status.
     *
     * @param userId1   the ID of one user in the friendship
     * @param userId2   the ID of the other user in the friendship
     * @param newStatus the new {@link FriendshipStatus} to apply (e.g., ACCEPTED,
     *                  DECLINED)
     * @throws ResponseStatusException if either user is not found
     *                                 ({@code NOT_FOUND}),
     *                                 or if no friendship exists between the two
     *                                 users ({@code NOT_FOUND})
     */
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

    /**
     * Retrieves the current friendship status between two users, including
     * directionality.
     * If a friendship exists in either direction, returns its status and whether
     * the current user initiated it.
     * If no friendship exists, returns {@code UNDEFINED} status.
     *
     * @param currentUserId the ID of the user making the request
     * @param otherUserId   the ID of the user being checked for friendship
     * @return a {@link FriendshipStatusDTO} containing the friendship status and
     *         initiator information
     * @throws ResponseStatusException if either user is not found
     *                                 ({@code NOT_FOUND})
     */
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
