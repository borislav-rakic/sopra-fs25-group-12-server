package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.FriendshipStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Friendship;
import ch.uzh.ifi.hase.soprafs24.rest.dto.FriendshipStatusDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MessageDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.service.FriendshipService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FriendshipController.class)
class FriendshipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FriendshipService friendshipService;

    @MockBean
    private UserService userService;

    private final String AUTH_HEADER = "Bearer validToken";
    private final Long currentUserId = 1L;
    private final Long otherUserId = 2L;

    @BeforeEach
    void setup() {
        Mockito.when(userService.getUserIdFromToken("validToken")).thenReturn(currentUserId);
    }

    @Test
    void getMyFriends_success() throws Exception {
        UserGetDTO friend = new UserGetDTO();
        friend.setId(2L);
        friend.setUsername("friendUser");

        Mockito.when(friendshipService.getFriends(currentUserId)).thenReturn(List.of(friend));

        mockMvc.perform(get("/users/me/friends")
                .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username", is("friendUser")));
    }

    @Test
    void sendFriendRequest_success() throws Exception {
        Mockito.when(friendshipService.sendFriendRequest(currentUserId, otherUserId))
                .thenReturn(new Friendship()); // mock Friendship object

        mockMvc.perform(post("/users/{userId}/friends", otherUserId)
                .header("Authorization", AUTH_HEADER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message", is("Friend request sent.")));
    }

    @Test
    void removeFriend_success() throws Exception {
        Mockito.doNothing().when(friendshipService).removeFriend(currentUserId, otherUserId);

        mockMvc.perform(delete("/users/{userId}/friends", otherUserId)
                .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Friend removed.")));
    }

    @Test
    void acceptFriendRequest_success() throws Exception {
        FriendshipStatusDTO dto = new FriendshipStatusDTO();
        dto.setStatus(FriendshipStatus.ACCEPTED);

        Mockito.doNothing().when(friendshipService).acceptFriendRequest(currentUserId, otherUserId);

        mockMvc.perform(put("/users/{userId}/friends", otherUserId)
                .header("Authorization", AUTH_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Friend request accepted.")));
    }

    @Test
    void declineFriendRequest_success() throws Exception {
        FriendshipStatusDTO dto = new FriendshipStatusDTO();
        dto.setStatus(FriendshipStatus.DECLINED);

        Mockito.doNothing().when(friendshipService).declineFriendRequest(currentUserId, otherUserId);

        mockMvc.perform(put("/users/{userId}/friends", otherUserId)
                .header("Authorization", AUTH_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DECLINED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Friend request declined.")));
    }

    @Test
    void getFriendshipStatus_success() throws Exception {
        FriendshipStatusDTO statusDTO = new FriendshipStatusDTO();
        statusDTO.setStatus(FriendshipStatus.ACCEPTED);
        statusDTO.setInitiatedByCurrentUser(true);

        Mockito.when(friendshipService.getFriendshipStatus(currentUserId, otherUserId)).thenReturn(statusDTO);

        mockMvc.perform(get("/users/{userId}/friends/status", otherUserId)
                .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACCEPTED")))
                .andExpect(jsonPath("$.initiatedByCurrentUser", is(true)));
    }

    @Test
    void getMyPendingFriends_success() throws Exception {
        UserGetDTO pendingFriend = new UserGetDTO();
        pendingFriend.setId(3L);
        pendingFriend.setUsername("pendingFriend");

        Mockito.when(friendshipService.getFriendsPending(currentUserId)).thenReturn(List.of(pendingFriend));

        mockMvc.perform(get("/users/me/friends/pending")
                .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username", is("pendingFriend")));
    }

    @Test
    void getMyDeclinedFriends_success() throws Exception {
        UserGetDTO declinedFriend = new UserGetDTO();
        declinedFriend.setId(4L);
        declinedFriend.setUsername("declinedFriend");

        Mockito.when(friendshipService.getFriendsDeclined(currentUserId)).thenReturn(List.of(declinedFriend));

        mockMvc.perform(get("/users/me/friends/declined")
                .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username", is("declinedFriend")));
    }

    @Test
    void unauthorized_noTokenProvided() throws Exception {
        mockMvc.perform(get("/users/me/friends"))
                .andExpect(status().isUnauthorized());
    }
}
