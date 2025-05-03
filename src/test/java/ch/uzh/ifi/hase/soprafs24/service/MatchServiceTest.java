package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

public class MatchServiceTest {

    @Mock
    private CardRulesService cardRulesService = Mockito.mock(CardRulesService.class);

    @Mock
    private GameRepository gameRepository = Mockito.mock(GameRepository.class);

    @Mock
    private GameSetupService gameSetupService = Mockito.mock(GameSetupService.class);

    @Mock
    private GameService gameService = Mockito.mock(GameService.class);

    @Mock
    private HtmlSummaryService htmlSummaryService = Mockito.mock(HtmlSummaryService.class);

    @Mock
    private MatchPlayerRepository matchPlayerRepository = Mockito.mock(MatchPlayerRepository.class);

    @Mock
    private MatchRepository matchRepository = Mockito.mock(MatchRepository.class);

    @Mock
    private PollingService pollingService = Mockito.mock(PollingService.class);

    @Mock
    private UserRepository userRepository = Mockito.mock(UserRepository.class);

    @MockBean
    private UserService userService = Mockito.mock(UserService.class);

    @InjectMocks
    private MatchService matchService = new MatchService(
            cardRulesService,
            gameRepository,
            gameSetupService,
            gameService,
            htmlSummaryService,
            matchPlayerRepository,
            matchRepository,
            pollingService,
            userRepository,
            userService
    // alphabetical order
    );

    private Match match;
    private User user;
    private User user2;
    private MatchPlayer matchPlayer;

    @BeforeEach
    public void setup() {
        user = new User();
        user.setUsername("username");
        user.setPassword("password");
        user.setId(11L); // Users up to id 10 are reserverd for AI Players.
        user.setToken("1234");
        user.setIsAiPlayer(false);
        user.setStatus(UserStatus.ONLINE);

        user2 = new User();
        user2.setUsername("username2");
        user2.setPassword("password2");
        user2.setId(12L); // Users up to id 10 are reserverd for AI Players.
        user2.setToken("12342");
        user2.setIsAiPlayer(false);
        user2.setStatus(UserStatus.ONLINE);

        match = new Match();
        match.setPhase(MatchPhase.SETUP);

        List<MatchPlayer> matchPlayers = new ArrayList<>();
        matchPlayers.add(new MatchPlayer());
        matchPlayers.get(0).setUser(user);
        matchPlayers.get(0).setMatch(match);

        match.setMatchPlayers(matchPlayers);
        match.setHostId(user.getId());
        match.setMatchGoal(100);
        match.setStarted(false);
        match.setPlayer1(user);

        matchPlayer = new MatchPlayer();
        matchPlayer.setMatch(match);
        matchPlayer.setUser(user);
        matchPlayer.setMatchPlayerId(1L);
    }

    @Test
    public void testCreateMatch() {
        given(userService.getUserByToken(Mockito.any())).willReturn(user);

        given(matchRepository.save(Mockito.any())).willReturn(null);
        doNothing().when(matchRepository).flush();

        Match result = matchService.createNewMatch("1234");

        assertEquals(match.getMatchId(), result.getMatchId());
        assertEquals(match.getMatchGoal(), result.getMatchGoal());
        assertEquals(match.getPlayer1(), result.getPlayer1());
        assertEquals(match.getPlayer2(), result.getPlayer2());
        assertEquals(match.getPlayer3(), result.getPlayer3());
        assertEquals(match.getPlayer4(), result.getPlayer4());
        assertEquals(match.getHostId(), result.getHostId());
        assertEquals(match.getMatchGoal(), result.getMatchGoal());
        assertEquals(match.getStarted(), result.getStarted());
    }

    @Test
    public void testGetMatchesInformation() {
        List<Match> matchList = new ArrayList<>();
        matchList.add(match);

        given(matchRepository.findAll()).willReturn(matchList);

        List<Match> result = matchService.getMatchesInformation();

        assertEquals(matchList, result);
    }

    @Test
    public void testGetPollingError() {
        given(matchRepository.findMatchByMatchId(1L)).willReturn(null);

        assertThrows(
                ResponseStatusException.class,
                () -> matchService.getPolling(1L),
                "Expected getPolling to throw an exception");
    }

    @Test
    public void testGetPollingSuccess() {
        given(matchRepository.findMatchByMatchId(1L)).willReturn(match);

        Match result = matchService.getPolling(1L);

        assertEquals(match, result);
    }

    @Test
    public void testDeleteMatchByHostMatchNull() {
        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(null);
        given(userRepository.findUserByToken(Mockito.any())).willReturn(user);

        assertThrows(
                ResponseStatusException.class,
                () -> matchService.deleteMatchByHost(1L, "1234"),
                "Expected deleteMatchByHost to throw an exception");
    }

    @Test
    public void testDeleteMatchByHostUserNull() {
        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);
        given(userRepository.findUserByToken(Mockito.any())).willReturn(null);

        assertThrows(
                ResponseStatusException.class,
                () -> matchService.deleteMatchByHost(1L, "1234"),
                "Expected deleteMatchByHost to throw an exception");
    }

    @Test
    public void testDeleteMatchByHostUserNotHost() {
        // Arrange
        user.setId(99L); // Not the host!
        match.setHostId(11L); // Real host has ID 11

        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);
        given(userRepository.findUserByToken(Mockito.any())).willReturn(user);

        // Act & Assert
        assertThrows(
                ResponseStatusException.class,
                () -> matchService.deleteMatchByHost(1L, "1234"),
                "Expected deleteMatchByHost to throw an exception when user is not host");
    }

    @Test
    public void testDeleteMatchByHostSuccess() {
        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);
        given(userRepository.findUserByToken(Mockito.any())).willReturn(user);

        doNothing().when(matchRepository).delete(match);

        matchService.deleteMatchByHost(1L, "1234");

        verify(matchRepository).delete(Mockito.any());
    }

    @Test
    public void testInvitePlayerToMatchError() {
        InviteRequestDTO inviteRequestDTO = new InviteRequestDTO();
        inviteRequestDTO.setPlayerSlot(1);
        inviteRequestDTO.setUserId(user.getId());
        user.setIsAiPlayer(false);
        user.setStatus(UserStatus.ONLINE);

        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(null);

        assertThrows(
                ResponseStatusException.class,
                () -> matchService.invitePlayerToMatch(1L, inviteRequestDTO),
                "Expected invitePlayerToMatch to throw an exception");
    }

    @Test
    public void testInvitePlayerToMatchSuccess() {
        InviteRequestDTO inviteRequestDTO = new InviteRequestDTO();
        inviteRequestDTO.setPlayerSlot(2);
        inviteRequestDTO.setUserId(user2.getId()); // not the host!

        given(userRepository.findById(user2.getId())).willReturn(Optional.of(user2));
        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);
        given(matchRepository.save(Mockito.any())).willReturn(match);

        matchService.invitePlayerToMatch(1L, inviteRequestDTO);

        verify(matchRepository).save(Mockito.any());
    }

    @Test
    public void testInvitePlayerToMatchSuccessInvitesNull() {
        InviteRequestDTO inviteRequestDTO = new InviteRequestDTO();
        inviteRequestDTO.setPlayerSlot(2);
        inviteRequestDTO.setUserId(user2.getId()); // Use invited player

        user2.setIsAiPlayer(false);
        user2.setStatus(UserStatus.ONLINE);

        match.setInvites(null); // simulate invites being null
        match.setHostId(user.getId()); // host is user

        given(userRepository.findById(user2.getId())).willReturn(Optional.of(user2));
        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);
        given(matchRepository.save(Mockito.any())).willReturn(match);

        matchService.invitePlayerToMatch(7L, inviteRequestDTO);

        verify(matchRepository).save(Mockito.any());
    }

    @Test
    public void testRespondToInviteAccepted() {
        InviteResponseDTO inviteResponseDTO = new InviteResponseDTO();
        inviteResponseDTO.setAccepted(true);

        user.setId(11L);
        user.setIsAiPlayer(false);
        user.setStatus(UserStatus.ONLINE);

        Map<Integer, Long> invites = new HashMap<>();
        invites.put(1, user.getId()); // assign matchPlayerSlot 1 to this user

        match.setInvites(invites);

        given(userRepository.findUserByToken("1234")).willReturn(user);
        given(matchRepository.findMatchByMatchId(1L)).willReturn(match);
        given(matchRepository.save(Mockito.any())).willReturn(match);

        given(matchPlayerRepository.save(Mockito.any())).willReturn(new MatchPlayer());

        matchService.respondToInvite(1L, "1234", inviteResponseDTO);

        verify(matchRepository).save(Mockito.any());
    }

    @Test
    public void testUpdateMatchGoalError() {
        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(null);

        assertThrows(
                ResponseStatusException.class,
                () -> matchService.updateMatchGoal(1L, new HashMap<>()),
                "Expected updateMatchGoal to throw an exception");
    }

    @Test
    public void testUpdateMatchGoalSuccess() {
        Match match = new Match();
        match.setMatchId(1L);

        given(matchRepository.findMatchByMatchId(eq(1L))).willReturn(match);
        given(matchRepository.save(any())).willReturn(match);

        Map<String, Integer> body = new HashMap<>();
        body.put("matchGoal", 100);

        matchService.updateMatchGoal(1L, body);

        verify(matchRepository).save(match); // You can also verify exact object

        assertEquals(100, match.getMatchGoal());
    }

    @Test
    public void testAddAiPlayerError() {
        AIPlayerDTO aiPlayerDTO = new AIPlayerDTO();

        aiPlayerDTO.setDifficulty(1);
        aiPlayerDTO.setPlayerSlot(0); // there is no PlayerSlot "0" for an AI player (only 1, 2 or 3, client-side)

        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(null);

        assertThrows(
                ResponseStatusException.class,
                () -> matchService.addAiPlayer(1L, aiPlayerDTO),
                "Expected addAiPlayer to throw an exception");
    }

    /*
     * @Test
     * public void testAddAiPlayerSuccess() {
     * AIPlayerDTO aiPlayerDTO = new AIPlayerDTO();
     * aiPlayerDTO.setDifficulty(1);
     * aiPlayerDTO.setPlayerSlot(1); // frontend slot 1 -> backend matchPlayerSlot 2
     * 
     * Match match = new Match();
     * match.setMatchId(1L);
     * match.setAiPlayers(new HashMap<>());
     * match.setMatchPlayers(new ArrayList<>());
     * 
     * // Host player
     * User hostUser = new User();
     * hostUser.setId(99L);
     * match.setPlayer1(hostUser);
     * match.setHostId(99L);
     * 
     * MatchPlayer hostMatchPlayer = new MatchPlayer();
     * hostMatchPlayer.setUser(hostUser);
     * hostMatchPlayer.setMatch(match);
     * hostMatchPlayer.setMatchPlayerSlot(1);
     * match.getMatchPlayers().add(hostMatchPlayer);
     * 
     * // AI player to be added in slot 2
     * User aiUser = new User();
     * aiUser.setId(1L);
     * aiUser.setIsAiPlayer(true);
     * 
     * // Dummy players to fill slots 3 and 4
     * User dummyUser3 = new User();
     * dummyUser3.setId(5L);
     * match.setPlayer3(dummyUser3);
     * User dummyUser4 = new User();
     * dummyUser4.setId(9L);
     * match.setPlayer4(dummyUser4);
     * 
     * given(matchRepository.findMatchByMatchId(1L)).willReturn(match);
     * given(userRepository.findUserById(1L)).willReturn(aiUser);
     * given(matchRepository.save(any(Match.class))).willReturn(match);
     * given(matchRepository.findById(1L)).willReturn(Optional.of(match));
     * 
     * assertNotNull(userRepository.findUserById(1L)); // Confirm mock holds
     * try {
     * matchService.addAiPlayer(1L, aiPlayerDTO);
     * fail("Expected exception not thrown");
     * } catch (ResponseStatusException e) {
     * System.out.println("### Exception: " + e.getReason());
     * // Optional: assert the expected error
     * assertEquals("PlayerSlot 1 is already taken.", e.getReason());
     * }
     * 
     * verify(matchRepository).save(match);
     * 
     * assertEquals(aiUser, match.getPlayer2());
     * assertTrue(match.getMatchPlayers().stream()
     * .anyMatch(mp -> mp.getUser().equals(aiUser) && mp.getIsAiPlayer()));
     * assertTrue(match.getAiPlayers().containsKey(2));
     * assertEquals(MatchPhase.READY, match.getPhase());
     * }
     */

    @Test
    public void testSendJoinRequestMatchNull() {
        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(null);
        given(userRepository.findUserByToken(Mockito.any())).willReturn(user);

        assertThrows(
                ResponseStatusException.class,
                () -> matchService.sendJoinRequest(1L, 1L),
                "Expected sendJoinRequest to throw an exception");
    }

    @Test
    public void testSendJoinRequestUserNull() {
        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);
        given(userRepository.findUserByToken(Mockito.any())).willReturn(null);

        assertThrows(
                ResponseStatusException.class,
                () -> matchService.sendJoinRequest(1L, 1L),
                "Expected sendJoinRequest to throw an exception");
    }

    @Test
    public void testSendJoinRequestSuccess() {
        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);
        given(userRepository.findUserById(Mockito.any())).willReturn(user);
        given(matchPlayerRepository.findByUserAndMatch(Mockito.any(), Mockito.any())).willReturn(null);
        given(matchRepository.save(Mockito.any())).willReturn(match);

        matchService.sendJoinRequest(1L, 1L);

        verify(matchRepository).save(Mockito.any());
    }

    @Test
    public void testAcceptJoinRequestMatchNull() {
        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(null);
        given(userRepository.findUserByToken(Mockito.any())).willReturn(user);

        assertThrows(
                ResponseStatusException.class,
                () -> matchService.acceptJoinRequest(1L, 1L),
                "Expected acceptJoinRequest to throw an exception");
    }

    @Test
    public void testAcceptJoinRequestUserNull() {
        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);
        given(userRepository.findUserByToken(Mockito.any())).willReturn(null);

        assertThrows(
                ResponseStatusException.class,
                () -> matchService.acceptJoinRequest(1L, 1L),
                "Expected acceptJoinRequest to throw an exception");
    }

    @Test
    public void testAcceptJoinRequestSuccess() {
        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);
        given(userRepository.findUserById(Mockito.any())).willReturn(user);
        given(matchRepository.save(Mockito.any())).willReturn(match);

        matchService.acceptJoinRequest(1L, 1L);

        verify(matchRepository).save(Mockito.any());
    }

    @Test
    public void testDeclineJoinRequestMatchNull() {
        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(null);

        assertThrows(
                ResponseStatusException.class,
                () -> matchService.declineJoinRequest(1L, 1L),
                "Expected declineJoinRequest to throw an exception");
    }

    @Test
    public void testDeclineJoinRequestSuccess() {
        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);
        given(matchRepository.save(Mockito.any())).willReturn(match);

        matchService.declineJoinRequest(1L, 1L);

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

        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);

        List<JoinRequestDTO> result = matchService.getJoinRequests(1L);

        assertEquals(joinRequestDTOs.size(), result.size());
        assertEquals(joinRequestDTOs.get(0).getUserId(), result.get(0).getUserId());
        assertEquals(joinRequestDTOs.get(0).getStatus(), result.get(0).getStatus());
    }
}
