package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchSummaryRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.AIPlayerDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.InviteRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.InviteResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MatchSetupServiceExpandedTest {

    @Mock
    private GameRepository gameRepository;
    @Mock
    private GameSetupService gameSetupService;
    @Mock
    private MatchPlayerRepository matchPlayerRepository;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MatchSummaryRepository matchSummaryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;

    @InjectMocks
    private MatchSetupService matchSetupService;

    private Match match;
    private User host;
    private User user;

    @BeforeEach
    void setup() {
        host = new User();
        host.setId(1L);
        host.setUsername("host");
        host.setStatus(UserStatus.ONLINE);

        user = new User();
        user.setId(2L);
        user.setUsername("user");
        user.setStatus(UserStatus.ONLINE);

        match = new Match();
        match.setMatchId(10L);
        match.setHostId(host.getId());
        match.setPlayer1(host);
        match.setMatchPlayers(new ArrayList<>());
        match.setJoinRequests(new HashMap<>());
        match.setInvites(new HashMap<>());
        match.setAiPlayers(new HashMap<>());
    }

    @Test
    void testCancelInviteRemovesSlot() {
        match.getInvites().put(2, user.getId());
        when(matchRepository.findById(10L)).thenReturn(Optional.of(match));

        matchSetupService.cancelInvite(10L, 2);

        assertFalse(match.getInvites().containsKey(2));
        verify(matchRepository).save(match);
    }

    @Test
    void testRemoveAiPlayerSuccess() {
        User aiUser = new User();
        aiUser.setId(5L);
        aiUser.setIsAiPlayer(true);

        MatchPlayer aiPlayer = new MatchPlayer();
        aiPlayer.setUser(aiUser);
        aiPlayer.setMatchPlayerSlot(2);
        aiPlayer.setMatch(match);

        match.getMatchPlayers().add(aiPlayer);
        match.setPlayer2(aiUser);
        match.getAiPlayers().put(2, 2);

        when(matchRepository.findById(10L)).thenReturn(Optional.of(match));

        AIPlayerDTO dto = new AIPlayerDTO();
        dto.setPlayerSlot(1); // Corresponds to slot 2
        matchSetupService.removeAiPlayer(10L, dto);

        assertNull(match.getPlayer2());
        assertTrue(match.getMatchPlayers().isEmpty());
        assertFalse(match.getAiPlayers().containsKey(2));
        verify(matchRepository).save(match);
    }

    @Test
    void testRemovePlayerSuccess() {
        MatchPlayer player = new MatchPlayer();
        player.setMatchPlayerSlot(3);
        player.setUser(user);
        match.setPlayer3(user);
        match.getMatchPlayers().add(player);
        match.getJoinRequests().put(user.getId(), "accepted");

        when(matchRepository.findById(10L)).thenReturn(Optional.of(match));

        matchSetupService.removePlayer(10L, 2); // 2 = playerSlot for player3

        assertNull(match.getPlayer3());
        assertFalse(match.getJoinRequests().containsKey(user.getId()));
        verify(matchRepository).save(match);
    }

    @Test
    void testStartMatchThrowsWhenNotReady() {
        match.setPhase(null); // Not READY
        when(userService.requireUserByToken("token")).thenReturn(host);
        when(matchRepository.findMatchForUpdate(10L)).thenReturn(match);

        assertThrows(ResponseStatusException.class, () -> matchSetupService.startMatch(10L, "token", 123L));
    }

    @Test
    void testGetEligibleUsers() {
        match.setPlayer1(host);
        when(userRepository.findUserByToken("token")).thenReturn(host);
        when(matchRepository.findById(10L)).thenReturn(Optional.of(match));

        User eligibleUser = new User();
        eligibleUser.setId(99L);
        eligibleUser.setUsername("eligible");
        eligibleUser.setStatus(UserStatus.ONLINE);
        eligibleUser.setIsAiPlayer(false);

        when(userRepository.findByStatusAndIsAiPlayerFalse(UserStatus.ONLINE)).thenReturn(List.of(eligibleUser));
        when(matchRepository.findAll()).thenReturn(List.of(match));

        var results = matchSetupService.getEligibleUsers(10L, "token");
        assertEquals(1, results.size());
        assertEquals(99L, results.get(0).getId());
    }

    @Test
    void testAddAiPlayerSuccess() {
        AIPlayerDTO dto = new AIPlayerDTO();
        dto.setDifficulty(1);
        dto.setPlayerSlot(1); // corresponds to slot 2

        User aiUser = new User();
        aiUser.setId(1L);
        aiUser.setIsAiPlayer(true);

        when(matchRepository.findById(10L)).thenReturn(Optional.of(match));
        when(userRepository.findUserById(1L)).thenReturn(aiUser);

        matchSetupService.addAiPlayer(10L, dto);

        assertEquals(aiUser, match.getPlayer2());
        verify(matchRepository).save(match);
    }

    @Test
    void testStartMatchValidFlow() {
        user.setId(1L);
        match.setHostId(1L);
        match.setPhase(MatchPhase.READY);
        match.setPlayer1(user);
        match.setPlayer2(new User());
        match.setPlayer3(new User());
        match.setPlayer4(new User());

        when(userService.requireUserByToken("token")).thenReturn(user);
        when(matchRepository.findMatchForUpdate(10L)).thenReturn(match);

        matchSetupService.startMatch(10L, "token", null);

        assertEquals(MatchPhase.BEFORE_GAMES, match.getPhase());
        verify(matchRepository, atLeastOnce()).save(match);
    }

    @Test
    void testSetMatchPhaseToReadyFailsOnInvitePending() {
        match.setPlayer1(user);
        match.setPlayer2(new User());
        match.setPlayer3(new User());
        match.setPlayer4(new User());
        match.getInvites().put(2, 99L);
        match.getJoinRequests().put(99L, "pending");

        assertThrows(ResponseStatusException.class,
                () -> matchSetupService.setMatchPhaseToReadyIfAppropriate(match, user));
    }

    @Test
    void testRespondToInviteAccepted() {
        InviteResponseDTO response = new InviteResponseDTO();
        response.setAccepted(true);

        match.getInvites().put(2, user.getId());

        when(userRepository.findUserByToken("token")).thenReturn(user);
        when(matchRepository.findById(10L)).thenReturn(Optional.of(match));

        matchSetupService.respondToInvite(10L, "Bearer token", response);

        assertEquals(user, match.getPlayer2());
        assertFalse(match.getInvites().containsKey(2));
        verify(matchRepository).save(match);
    }

    @Test
    void testCancelInvite() {
        match.getInvites().put(2, 11L);
        when(matchRepository.findById(10L)).thenReturn(Optional.of(match));

        matchSetupService.cancelInvite(10L, 2);

        assertFalse(match.getInvites().containsKey(2));
        verify(matchRepository).save(match);
    }

    @Test
    void testInvitePlayerToMatchFailsWhenUserOffline() {
        user.setStatus(UserStatus.OFFLINE); // ❗ Simulate offline user
        match.setInvites(new HashMap<>());

        InviteRequestDTO inviteRequest = new InviteRequestDTO();
        inviteRequest.setUserId(user.getId());
        inviteRequest.setPlayerSlot(1); // Player 2

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(matchRepository.findById(match.getMatchId())).thenReturn(Optional.of(match));

        assertThrows(ResponseStatusException.class,
                () -> matchSetupService.invitePlayerToMatch(match.getMatchId(), inviteRequest));
    }

}