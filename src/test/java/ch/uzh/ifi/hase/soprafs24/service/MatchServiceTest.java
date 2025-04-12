package ch.uzh.ifi.hase.soprafs24.service;

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
    private MatchService matchService = new MatchService(matchRepository, userService, userRepository, matchPlayerRepository);

    private Match match;
    private User user;
    private MatchPlayer matchPlayer;

    @BeforeEach
    public void setup() {
        user = new User();
        user.setUsername("username");
        user.setPassword("password");
        user.setId(1L);
        user.setToken("1234");

        match = new Match();

        List<MatchPlayer> matchPlayers = new ArrayList<>();
        matchPlayers.add(new MatchPlayer());
        matchPlayers.get(0).setPlayerId(user);
        matchPlayers.get(0).setMatch(match);

        match.setMatchPlayers(matchPlayers);
        match.setHost(user.getUsername());
        match.setLength(100);
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
        assertEquals(match.getLength(), result.getLength());
        assertEquals(match.getPlayer1(), result.getPlayer1());
        assertEquals(match.getPlayer2(), result.getPlayer2());
        assertEquals(match.getPlayer3(), result.getPlayer3());
        assertEquals(match.getPlayer4(), result.getPlayer4());
        assertEquals(match.getHost(), result.getHost());
        assertEquals(match.getLength(), result.getLength());
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
                "Expected getMatchInformation to throw an exception"
        );
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
                "Expected deleteMatchByHost to throw an exception"
        );
    }

    @Test public void testDeleteMatchByHostUserNull() {
        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);
        given(userRepository.findUserByToken(Mockito.any())).willReturn(null);

        assertThrows(
                ResponseStatusException.class,
                () -> matchService.deleteMatchByHost(1L, "1234"),
                "Expected deleteMatchByHost to throw an exception"
        );
    }

    @Test public void testDeleteMatchByHostUserNotHost() {
        user.setUsername("notHost");

        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);
        given(userRepository.findUserByToken(Mockito.any())).willReturn(user);

        assertThrows(
                ResponseStatusException.class,
                () -> matchService.deleteMatchByHost(1L, "1234"),
                "Expected deleteMatchByHost to throw an exception"
        );
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

        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(null);

        assertThrows(
                ResponseStatusException.class,
                () -> matchService.invitePlayerToMatch(1L, inviteRequestDTO),
                "Expected invitePlayerToMatch to throw an exception"
        );
    }

    @Test
    public void testInvitePlayerToMatchSuccess() {
        InviteRequestDTO inviteRequestDTO = new InviteRequestDTO();
        inviteRequestDTO.setPlayerSlot(1);
        inviteRequestDTO.setUserId(user.getId());

        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);

        given(matchRepository.save(Mockito.any())).willReturn(match);

        matchService.invitePlayerToMatch(1L, inviteRequestDTO);

        verify(matchRepository).save(Mockito.any());
    }

    @Test
    public void testInvitePlayerToMatchSuccessInvitesNull() {
        InviteRequestDTO inviteRequestDTO = new InviteRequestDTO();
        inviteRequestDTO.setPlayerSlot(1);
        inviteRequestDTO.setUserId(user.getId());

        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);

        given(matchRepository.save(Mockito.any())).willReturn(match);

        match.setInvites(null);

        matchService.invitePlayerToMatch(1L, inviteRequestDTO);

        verify(matchRepository).save(Mockito.any());
    }

    @Test
    public void testRespondToInviteAccepted() {
        InviteResponseDTO inviteResponseDTO = new InviteResponseDTO();
        inviteResponseDTO.setAccepted(true);

        Map<Integer, Long> invites = new HashMap<>();
        invites.put(1, 1L);

        match.setInvites(invites);

        given(userRepository.findUserByToken(Mockito.any())).willReturn(user);

        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);

        matchService.respondToInvite(1L, "1234", inviteResponseDTO);

        verify(matchRepository).save(Mockito.any());
    }

    @Test
    public void testUpdateMatchLengthError() {
        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(null);

        assertThrows(
                ResponseStatusException.class,
                () -> matchService.updateMatchLength(1L, new HashMap<>()),
                "Expected updateMatchLength to throw an exception"
        );
    }

    @Test
    public void testUpdateMatchLengthSuccess() {
        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);

        Map<String, Integer> body = new HashMap<>();
        body.put("length", 100);

        given(matchRepository.save(Mockito.any())).willReturn(match);

        matchService.updateMatchLength(1L, body);

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
                "Expected addAiPlayer to throw an exception"
        );
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
                "Expected sendJoinRequest to throw an exception"
        );
    }

    @Test
    public void testSendJoinRequestUserNull() {
        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);
        given(userRepository.findUserByToken(Mockito.any())).willReturn(null);

        assertThrows(
                ResponseStatusException.class,
                () -> matchService.sendJoinRequest(1L, 1L),
                "Expected sendJoinRequest to throw an exception"
        );
    }

    @Test
    public void testSendJoinRequestSuccess() {
        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);
        given(userRepository.findUserById(Mockito.any())).willReturn(user);
        given(matchPlayerRepository.findMatchPlayerByUser(Mockito.any())).willReturn(null);
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
                "Expected acceptJoinRequest to throw an exception"
        );
    }

    @Test
    public void testAcceptJoinRequestUserNull() {
        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);
        given(userRepository.findUserByToken(Mockito.any())).willReturn(null);

        assertThrows(
                ResponseStatusException.class,
                () -> matchService.acceptJoinRequest(1L, 1L),
                "Expected acceptJoinRequest to throw an exception"
        );
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
                "Expected declineJoinRequest to throw an exception"
        );
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
