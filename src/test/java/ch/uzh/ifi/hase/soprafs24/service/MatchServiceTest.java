package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

public class MatchServiceTest {
    @Mock
    private MatchRepository matchRepository = Mockito.mock(MatchRepository.class);

    @Mock
    private UserRepository userRepository = Mockito.mock(UserRepository.class);

    @MockBean
    private UserService userService = Mockito.mock(UserService.class);

    @Mock
    private MatchPlayerRepository matchPlayerRepository = Mockito.mock(MatchPlayerRepository.class);

    @InjectMocks
    private MatchService matchService = new MatchService(matchRepository, userService, userRepository,
            matchPlayerRepository);

    private Match match;
    private User user;
    private User user2;
    private MatchPlayer matchPlayer;

    @BeforeEach
    public void setup() {
        user = new User();
        user.setUsername("username");
        user.setPassword("password");
        user.setId(7L); // Users up to id 3 are reserverd for AI Players.
        user.setToken("1234");
        user.setIsAiPlayer(false);
        user.setStatus(UserStatus.ONLINE);

        user2 = new User();
        user2.setUsername("username2");
        user2.setPassword("password2");
        user2.setId(8L); // Users up to id 3 are reserverd for AI Players.
        user2.setToken("12342");
        user2.setIsAiPlayer(false);
        user2.setStatus(UserStatus.ONLINE);

        match = new Match();

        List<MatchPlayer> matchPlayers = new ArrayList<>();
        matchPlayers.add(new MatchPlayer());
        matchPlayers.get(0).setPlayerId(user);
        matchPlayers.get(0).setMatch(match);

        match.setMatchPlayers(matchPlayers);
        match.setHost(user.getUsername());
        match.setMatchGoal(100);
        match.setStarted(false);
        match.setPlayer1(user);

        matchPlayer = new MatchPlayer();
        matchPlayer.setMatch(match);
        matchPlayer.setPlayerId(user);
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
        assertEquals(match.getHost(), result.getHost());
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
    public void testGetMatchInformationError() {
        given(matchRepository.findMatchByMatchId(1L)).willReturn(null);

        assertThrows(
                ResponseStatusException.class,
                () -> matchService.getMatchInformation(1L),
                "Expected getMatchInformation to throw an exception");
    }

    @Test
    public void testGetMatchInformationSuccess() {
        given(matchRepository.findMatchByMatchId(1L)).willReturn(match);

        Match result = matchService.getMatchInformation(1L);

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
        user.setUsername("notHost");

        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);
        given(userRepository.findUserByToken(Mockito.any())).willReturn(user);

        assertThrows(
                ResponseStatusException.class,
                () -> matchService.deleteMatchByHost(1L, "1234"),
                "Expected deleteMatchByHost to throw an exception");
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
        inviteRequestDTO.setPlayerSlot(1);
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
        inviteRequestDTO.setPlayerSlot(1);
        inviteRequestDTO.setUserId(user2.getId()); // Use invited player

        user2.setIsAiPlayer(false);
        user2.setStatus(UserStatus.ONLINE);

        match.setInvites(null); // simulate invites being null
        match.setHost(user.getUsername()); // host is user

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

        user.setId(7L);
        user.setIsAiPlayer(false);
        user.setStatus(UserStatus.ONLINE);

        Map<Integer, Long> invites = new HashMap<>();
        invites.put(1, user.getId()); // assign slot 1 to this user

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
        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);

        Map<String, Integer> body = new HashMap<>();
        body.put("matchGoal", 100);

        given(matchRepository.save(Mockito.any())).willReturn(match);

        matchService.updateMatchGoal(1L, body);

        verify(matchRepository).save(Mockito.any());
    }

    @Test
    public void testAddAiPlayerError() {
        AIPlayerDTO aiPlayerDTO = new AIPlayerDTO();
        aiPlayerDTO.setDifficulty(1);

        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(null);

        assertThrows(
                ResponseStatusException.class,
                () -> matchService.addAiPlayer(1L, aiPlayerDTO),
                "Expected addAiPlayer to throw an exception");
    }

    @Test
    public void testAddAiPlayerSuccess() {
        AIPlayerDTO aiPlayerDTO = new AIPlayerDTO();
        aiPlayerDTO.setDifficulty(1);

        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);

        given(matchRepository.save(Mockito.any())).willReturn(match);

        matchService.addAiPlayer(1L, aiPlayerDTO);

        verify(matchRepository).save(Mockito.any());
    }

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
