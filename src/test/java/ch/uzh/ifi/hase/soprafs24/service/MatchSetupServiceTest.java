package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        when(matchRepository.findByHostIdAndStarted(user.getId(), false)).thenReturn(null);
        when(matchRepository.saveAndFlush(Mockito.any())).thenReturn(match);

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
    public void testDeleteMatchByHostMatchNull() {
        lenient().when(matchRepository.findMatchByMatchId(Mockito.any())).thenReturn(null);
        lenient().when(userRepository.findUserByToken(Mockito.any())).thenReturn(user);

        assertThrows(
                ResponseStatusException.class,
                () -> matchSetupService.deleteMatchByHost(1L, "1234"),
                "Expected deleteMatchByHost to throw an exception");
    }

    @Test
    public void testDeleteMatchByHostUserNull() {
        lenient().when(matchRepository.findMatchByMatchId(Mockito.any())).thenReturn(match);
        lenient().when(userRepository.findUserByToken(Mockito.any())).thenReturn(null);

        assertThrows(
                ResponseStatusException.class,
                () -> matchSetupService.deleteMatchByHost(1L, "1234"),
                "Expected deleteMatchByHost to throw an exception");
    }

    @Test
    public void testDeleteMatchByHostUserNotHost() {
        // Arrange
        user.setId(99L); // Not the host!
        match.setHostId(11L); // Real host has ID 11

        lenient().when(matchRepository.findMatchByMatchId(Mockito.any())).thenReturn(match);
        lenient().when(userRepository.findUserByToken(Mockito.any())).thenReturn(user);

        // Act & Assert
        assertThrows(
                ResponseStatusException.class,
                () -> matchSetupService.deleteMatchByHost(1L, "1234"),
                "Expected deleteMatchByHost to throw an exception when user is not host");
    }

    @Test
    public void testDeleteMatchByHostSuccess() {
        when(matchRepository.findById(Mockito.any())).thenReturn(Optional.of(match));
        when(userRepository.findUserByToken(Mockito.any())).thenReturn(user);

        doNothing().when(matchRepository).delete(match);

        matchSetupService.deleteMatchByHost(1L, "1234");

        verify(matchRepository).delete(Mockito.any());
    }

    @Test
    public void testInvitePlayerToMatchError() {
        InviteRequestDTO inviteRequestDTO = new InviteRequestDTO();
        inviteRequestDTO.setPlayerSlot(1);
        inviteRequestDTO.setUserId(user.getId());
        user.setIsAiPlayer(false);
        user.setStatus(UserStatus.ONLINE);

        when(matchRepository.findById(Mockito.any())).thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> matchSetupService.invitePlayerToMatch(1L, inviteRequestDTO),
                "Expected invitePlayerToMatch to throw an exception");
    }

    @Test
    public void testInvitePlayerToMatchSuccess() {
        InviteRequestDTO inviteRequestDTO = new InviteRequestDTO();
        inviteRequestDTO.setPlayerSlot(2);
        inviteRequestDTO.setUserId(user2.getId()); // not the host!

        when(userRepository.findById(user2.getId())).thenReturn(Optional.of(user2));
        when(matchRepository.findById(Mockito.any())).thenReturn(Optional.of(match));
        when(matchRepository.save(Mockito.any())).thenReturn(match);

        matchSetupService.invitePlayerToMatch(1L, inviteRequestDTO);

        verify(matchRepository).save(Mockito.any());
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

}
