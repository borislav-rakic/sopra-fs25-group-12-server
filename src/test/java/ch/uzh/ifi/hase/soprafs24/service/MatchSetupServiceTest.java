package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.AIPlayerDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.InviteRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.JoinRequestDTO;

/**
 * Unit tests for MatchSetupService.
 */
@ExtendWith(MockitoExtension.class)
public class MatchSetupServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameSetupService gameSetupService;

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private MatchSetupService matchSetupService;

    private Match match;
    private User user;
    private User user2;
    private MatchPlayer matchPlayer;

    @BeforeEach
    public void setup() {
        match = new Match();
        match.setMatchId(1L);
        user = new User();
        user.setUsername("username");
        user.setPassword("password");
        user.setId(11L);
        user.setToken("1234");
        user.setIsAiPlayer(false);
        user.setStatus(UserStatus.ONLINE);

        user2 = new User();
        user2.setUsername("username2");
        user2.setPassword("password2");
        user2.setId(12L);
        user2.setToken("5678");
        user2.setIsAiPlayer(false);
        user2.setStatus(UserStatus.ONLINE);

        match = new Match();
        match.setPhase(MatchPhase.SETUP);
        match.setHostId(user.getId());
        match.setMatchGoal(100);
        match.setStarted(false);
        match.setPlayer1(user);

        matchPlayer = new MatchPlayer();
        matchPlayer.setMatch(match);
        matchPlayer.setUser(user);
        matchPlayer.setMatchPlayerSlot(1);

        List<MatchPlayer> matchPlayers = new ArrayList<>();
        matchPlayers.add(matchPlayer);
        match.setMatchPlayers(matchPlayers);
    }

    @Test
    public void testCreateMatch() {
        // Arrange
        when(userService.getUserByToken("1234")).thenReturn(user);
        when(matchRepository.findActiveMatchesByHostId(user.getId())).thenReturn(null);
        when(matchRepository.saveAndFlush(Mockito.any())).thenReturn(match);
        when(matchRepository.findActiveMatchesByHostId(user.getId())).thenReturn(Collections.emptyList());

        // Act
        Match result = matchSetupService.createNewMatch("1234");

        // Assert
        assertEquals(match.getMatchId(), result.getMatchId());
        assertEquals(match.getMatchGoal(), result.getMatchGoal());
        assertEquals(match.getPlayer1(), result.getPlayer1());
        assertEquals(match.getHostId(), result.getHostId());
        assertEquals(match.getStarted(), result.getStarted());
    }

    @Test
    public void testInvitePlayerToMatchError() {
        InviteRequestDTO inviteRequestDTO = new InviteRequestDTO();
        inviteRequestDTO.setPlayerSlot(1);
        inviteRequestDTO.setUserId(user.getId());
        user.setIsAiPlayer(false);
        user.setStatus(UserStatus.ONLINE);

        assertThrows(
                ResponseStatusException.class,
                () -> matchSetupService.invitePlayerToMatch(1L, inviteRequestDTO),
                "Expected invitePlayerToMatch to throw an exception");
    }

    @Test
    public void testInvitePlayerToMatchSuccess() {
        InviteRequestDTO inviteRequestDTO = new InviteRequestDTO();
        inviteRequestDTO.setPlayerSlot(2);
        inviteRequestDTO.setUserId(user2.getId());

        // Setup mock match
        Match mockMatch = new Match();
        mockMatch.setMatchId(1L);
        mockMatch.setInvites(new HashMap<>());

        when(userRepository.findById(user2.getId()))
                .thenReturn(Optional.of(user2));

        when(matchRepository.findAllMatchesByMatchIdWithInvites(1L))
                .thenReturn(List.of(mockMatch));

        when(matchRepository.saveAndFlush(Mockito.any()))
                .thenReturn(mockMatch);

        matchSetupService.invitePlayerToMatch(1L, inviteRequestDTO);

        verify(matchRepository).saveAndFlush(Mockito.any());
    }

    @Test
    public void testUpdateMatchGoalError() {
        lenient().when(matchRepository.findMatchByMatchId(Mockito.any())).thenReturn(null);

        assertThrows(
                ResponseStatusException.class,
                () -> matchSetupService.updateMatchGoal(1L, new HashMap<>()),
                "Expected updateMatchGoal to throw an exception");
    }

    @Test
    public void testUpdateMatchGoalSuccess() {
        Match match = new Match();
        match.setMatchId(1L);

        when(matchRepository.findById(eq(1L))).thenReturn(Optional.of(match));
        when(matchRepository.save(any())).thenReturn(match);

        Map<String, Integer> body = new HashMap<>();
        body.put("matchGoal", 100);

        matchSetupService.updateMatchGoal(1L, body);

        verify(matchRepository).save(match);
        assertEquals(100, match.getMatchGoal());
    }

    @Test
    public void testAddAiPlayerError() {
        AIPlayerDTO aiPlayerDTO = new AIPlayerDTO();

        aiPlayerDTO.setDifficulty(1);
        aiPlayerDTO.setPlayerSlot(0); // there is no PlayerSlot "0" for an AI player (only 1, 2 or 3, client-side)

        lenient().when(matchRepository.findMatchByMatchId(Mockito.any())).thenReturn(null);

        assertThrows(
                ResponseStatusException.class,
                () -> matchSetupService.addAiPlayer(1L, aiPlayerDTO),
                "Expected addAiPlayer to throw an exception");
    }

    @Test
    public void testSendJoinRequestMatchNull() {
        when(matchRepository.findById(Mockito.any())).thenReturn(Optional.empty()); // Match not found

        assertThrows(
                ResponseStatusException.class,
                () -> matchSetupService.sendJoinRequest(1L, 1L),
                "Expected sendJoinRequest to throw an exception");
    }

    @Test
    public void testSendJoinRequestUserNull() {
        lenient().when(matchRepository.findMatchByMatchId(Mockito.any())).thenReturn(match);
        lenient().when(userRepository.findUserByToken(Mockito.any())).thenReturn(null);

        assertThrows(
                ResponseStatusException.class,
                () -> matchSetupService.sendJoinRequest(1L, 1L),
                "Expected sendJoinRequest to throw an exception");
    }

    @Test
    public void testSendJoinRequestSuccess() {
        match.setJoinRequests(new HashMap<>()); // Ensure joinRequests map is not null

        when(matchRepository.findById(Mockito.any())).thenReturn(Optional.of(match));
        when(userRepository.findById(Mockito.any())).thenReturn(Optional.of(user));
        when(matchPlayerRepository.findByUserAndMatch(Mockito.any(), Mockito.any())).thenReturn(null);
        when(matchRepository.save(Mockito.any())).thenReturn(match);

        matchSetupService.sendJoinRequest(1L, 1L);

        verify(matchRepository).save(Mockito.any());
    }

    @Test
    public void testAcceptJoinRequestMatchNull() {
        lenient().when(matchRepository.findMatchByMatchId(Mockito.any())).thenReturn(null);
        lenient().when(userRepository.findUserByToken(Mockito.any())).thenReturn(user);

        assertThrows(
                ResponseStatusException.class,
                () -> matchSetupService.acceptJoinRequest(1L, 1L),
                "Expected acceptJoinRequest to throw an exception");
    }

    @Test
    public void testAcceptJoinRequestUserNull() {
        when(matchRepository.findById(Mockito.any())).thenReturn(Optional.of(match));
        when(userRepository.findById(Mockito.any())).thenReturn(Optional.empty()); // <- simulate user not found

        assertThrows(
                ResponseStatusException.class,
                () -> matchSetupService.acceptJoinRequest(1L, 1L),
                "Expected acceptJoinRequest to throw an exception");
    }

    @Test
    public void testAcceptJoinRequestSuccess() {
        match.setJoinRequests(new HashMap<>()); // Ensure joinRequests map exists
        match.getJoinRequests().put(1L, "pending"); // Simulate a valid pending request
        match.setMatchPlayers(new ArrayList<>());

        when(matchRepository.findById(Mockito.any())).thenReturn(Optional.of(match));
        when(userRepository.findById(Mockito.any())).thenReturn(Optional.of(user));
        when(matchRepository.save(Mockito.any())).thenReturn(match);

        matchSetupService.acceptJoinRequest(1L, 1L);

        verify(matchRepository).save(Mockito.any());
    }

    @Test
    public void testDeclineJoinRequestMatchNull() {
        lenient().when(matchRepository.findMatchByMatchId(Mockito.any())).thenReturn(null);

        assertThrows(
                ResponseStatusException.class,
                () -> matchSetupService.declineJoinRequest(1L, 1L),
                "Expected declineJoinRequest to throw an exception");
    }

    @Test
    public void testDeclineJoinRequestSuccess() {
        Map<Long, String> joinRequests = new HashMap<>();
        joinRequests.put(1L, "pending");
        match.setJoinRequests(joinRequests);

        when(matchRepository.findById(Mockito.any())).thenReturn(Optional.of(match));
        when(matchRepository.save(Mockito.any())).thenReturn(match);

        matchSetupService.declineJoinRequest(1L, 1L);

        verify(matchRepository).save(Mockito.any());
    }

    @Test
    public void testGetJoinRequests() {
        List<JoinRequestDTO> joinRequestDTOs = new ArrayList<>();
        JoinRequestDTO joinRequestDTO = new JoinRequestDTO();
        joinRequestDTO.setUserId(1L);
        joinRequestDTO.setStatus("1");
        joinRequestDTOs.add(joinRequestDTO);

        Map<Long, String> joinRequests = new HashMap<>();
        joinRequests.put(1L, "1");

        match.setJoinRequests(joinRequests);

        when(matchRepository.findById(Mockito.any())).thenReturn(Optional.of(match));

        List<JoinRequestDTO> result = matchSetupService.getJoinRequests(1L);

        assertEquals(joinRequestDTOs.size(), result.size());
        assertEquals(joinRequestDTOs.get(0).getUserId(), result.get(0).getUserId());
        assertEquals(joinRequestDTOs.get(0).getStatus(), result.get(0).getStatus());
    }

    @Test
    public void testRemovePlayer() {
        MatchPlayer matchPlayer2 = new MatchPlayer();
        matchPlayer2.setMatchPlayerSlot(2);
        matchPlayer2.setUser(new User());
        matchPlayer2.getUser().setId(12L);
        match.setPlayer2(matchPlayer2.getUser());
        match.getMatchPlayers().add(matchPlayer2);

        when(matchRepository.findById(Mockito.anyLong())).thenReturn(Optional.of(match));
        when(matchRepository.save(Mockito.any())).thenReturn(match);

        assertEquals(match.getPlayer2(), matchPlayer2.getUser());

        matchSetupService.removePlayer(1L, 1);

        assertNull(match.getPlayer2());
    }

    @Test
    public void testSetMatchPhaseToReadyIfAppropriate_inProgress() {
        match.setHostId(user.getId());
        match.setPhase(MatchPhase.SETUP);
        match.setPlayer2(new User());
        match.setPlayer3(new User());
        match.setPlayer4(new User());

        when(matchRepository.save(Mockito.any())).thenReturn(match);

        assertEquals(match.getPhase(), MatchPhase.SETUP);

        matchSetupService.setMatchPhaseToReadyIfAppropriate(match, user);

        assertEquals(match.getPhase(), MatchPhase.READY);
    }

    @Test
    public void testStartMatch() {
        match.setHostId(user.getId());
        match.setPhase(MatchPhase.SETUP);
        match.setPlayer2(new User());
        match.setPlayer3(new User());
        match.setPlayer4(new User());

        when(matchRepository.save(Mockito.any())).thenReturn(match);

        when(userService.requireUserByToken(Mockito.anyString())).thenReturn(user);
        when(matchRepository.findMatchForUpdate(Mockito.anyLong())).thenReturn(match);
        when(gameSetupService.createAndStartGameForMatch(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(new Game());
        when(gameRepository.save(Mockito.any())).thenReturn(new Game());

        matchSetupService.startMatch(1L, "1234", null);

        verify(matchRepository, times(2)).save(Mockito.any());
        verify(userService, times(1)).requireUserByToken(Mockito.anyString());
        verify(gameSetupService, times(1))
                .createAndStartGameForMatch(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        verify(gameRepository, times(1)).save(Mockito.any());
    }
}
