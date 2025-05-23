package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.FriendshipStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Friendship;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.FriendshipRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.util.TestUserFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

class FriendshipServiceTest {

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FriendshipService friendshipService;

    private User userA;
    private User userB;
    private Friendship friendship;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        userA = TestUserFactory.createValidUser("UserA");
        userA.setId(9991L);

        userB = TestUserFactory.createValidUser("UserB");
        userB.setId(9992L);

        friendship = new Friendship(userA, userB, FriendshipStatus.PENDING);
    }

    @Test
    void sendFriendRequest_success() {
        Mockito.when(userRepository.findUserById(9991L)).thenReturn(userA);
        Mockito.when(userRepository.findUserById(9992L)).thenReturn(userB);
        Mockito.when(friendshipRepository.findByUserAndFriend(userA, userB)).thenReturn(Optional.empty());
        Mockito.when(friendshipRepository.save(any())).thenReturn(friendship);

        Friendship result = friendshipService.sendFriendRequest(9991L, 9992L);

        assertNotNull(result);
        assertEquals(FriendshipStatus.PENDING, result.getStatus());
    }

    @Test
    void sendFriendRequest_duplicateRequest_throwsException() {
        Mockito.when(userRepository.findUserById(anyLong())).thenReturn(userA);
        Mockito.when(friendshipRepository.findByUserAndFriend(any(), any()))
                .thenReturn(Optional.of(friendship));

        assertThrows(ResponseStatusException.class, () -> friendshipService.sendFriendRequest(1L, 2L));
    }

    @Test
    void acceptFriendRequest_success() {
        Mockito.when(userRepository.findUserById(9992L)).thenReturn(userB);
        Mockito.when(userRepository.findUserById(9991L)).thenReturn(userA);
        Mockito.when(friendshipRepository.findByUserAndFriend(userA, userB))
                .thenReturn(Optional.of(friendship));

        friendshipService.acceptFriendRequest(9992L, 9991L);

        assertEquals(FriendshipStatus.ACCEPTED, friendship.getStatus());
        Mockito.verify(friendshipRepository).save(friendship);
    }

    @Test
    void declineFriendRequest_success() {
        Mockito.when(userRepository.findUserById(9992L)).thenReturn(userB);
        Mockito.when(userRepository.findUserById(9991L)).thenReturn(userA);
        Mockito.when(friendshipRepository.findByUserAndFriend(userA, userB))
                .thenReturn(Optional.of(friendship));

        friendshipService.declineFriendRequest(9992L, 9991L);

        assertEquals(FriendshipStatus.DECLINED, friendship.getStatus());
        Mockito.verify(friendshipRepository).save(friendship);
    }

    @Test
    void removeFriend_existingFriendship_success() {
        Mockito.when(userRepository.findUserById(9991L)).thenReturn(userA);
        Mockito.when(userRepository.findUserById(9992L)).thenReturn(userB);
        Mockito.when(friendshipRepository.findByUserAndFriend(userA, userB)).thenReturn(Optional.of(friendship));

        friendshipService.removeFriend(9991L, 9992L);

        Mockito.verify(friendshipRepository).delete(friendship);
    }

    @Test
    void removeFriend_nonExistingFriendship_noAction() {
        Mockito.when(userRepository.findUserById(anyLong())).thenReturn(userA);
        Mockito.when(friendshipRepository.findByUserAndFriend(any(), any())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> friendshipService.removeFriend(1L, 3L));
    }

    @Test
    void getFriends_success() {
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        Mockito.when(userRepository.findUserById(9991L)).thenReturn(userA);
        Mockito.when(friendshipRepository.findAllByUserOrFriend(userA, userA)).thenReturn(List.of(friendship));

        var friendsDTO = friendshipService.getFriends(9991L);

        assertEquals(1, friendsDTO.size());
        assertEquals("UserB", friendsDTO.get(0).getUsername());
    }

    @Test
    void getFriendshipStatus_noFriendship_returnsUndefined() {
        Mockito.when(userRepository.findUserById(9991L)).thenReturn(userA);
        Mockito.when(userRepository.findUserById(9992L)).thenReturn(userB);
        Mockito.when(friendshipRepository.findByUserAndFriend(any(), any())).thenReturn(Optional.empty());

        var statusDTO = friendshipService.getFriendshipStatus(9991L, 9992L);

        assertEquals(FriendshipStatus.UNDEFINED, statusDTO.getStatus());
        assertFalse(statusDTO.isInitiatedByCurrentUser());
    }

    @Test
    void getFriendshipStatus_existingFriendshipInitiatedByCurrentUser() {
        friendship.setStatus(FriendshipStatus.PENDING);
        Mockito.when(userRepository.findUserById(9991L)).thenReturn(userA);
        Mockito.when(userRepository.findUserById(9992L)).thenReturn(userB);
        Mockito.when(friendshipRepository.findByUserAndFriend(userA, userB)).thenReturn(Optional.of(friendship));

        var statusDTO = friendshipService.getFriendshipStatus(9991L, 9992L);

        assertEquals(FriendshipStatus.PENDING, statusDTO.getStatus());
        assertTrue(statusDTO.isInitiatedByCurrentUser());
    }

    @Test
    void updateFriendshipStatus_success() {
        Mockito.when(userRepository.findUserById(9991L)).thenReturn(userA);
        Mockito.when(userRepository.findUserById(9992L)).thenReturn(userB);
        Mockito.when(friendshipRepository.findByUserAndFriend(any(), any())).thenReturn(Optional.of(friendship));

        friendshipService.updateFriendshipStatus(9991L, 9992L, FriendshipStatus.ACCEPTED);

        assertEquals(FriendshipStatus.ACCEPTED, friendship.getStatus());
        Mockito.verify(friendshipRepository).save(friendship);
    }
}
