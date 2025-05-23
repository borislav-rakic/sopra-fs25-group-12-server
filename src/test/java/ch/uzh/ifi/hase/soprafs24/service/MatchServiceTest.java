package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.constant.TrickPhase;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MatchDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayedCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PollingDTO;

public class MatchServiceTest {

    @Mock
    private GameRepository gameRepository = Mockito.mock(GameRepository.class);

    @Mock
    private GameService gameService = Mockito.mock(GameService.class);

    @Mock
    private GameSimulationService gameSimulationService = Mockito.mock(GameSimulationService.class);

    @Mock
    private GameSetupService gameSetupService = Mockito.mock(GameSetupService.class);

    @Mock
    private MatchSummaryService matchSummaryService = Mockito.mock(MatchSummaryService.class);

    @Mock
    private MatchPlayerRepository matchPlayerRepository = Mockito.mock(MatchPlayerRepository.class);

    @Mock
    private MatchRepository matchRepository = Mockito.mock(MatchRepository.class);

    @Mock
    private PollingService pollingService = Mockito.mock(PollingService.class);

    @Mock
    private UserRepository userRepository = Mockito.mock(UserRepository.class);

    @Mock
    private MatchSetupService matchSetupService = Mockito.mock(MatchSetupService.class);

    @MockBean
    private UserService userService = Mockito.mock(UserService.class);

    @InjectMocks
    private MatchService matchService = new MatchService(
            gameRepository,
            gameService,
            gameSetupService,
            gameSimulationService,
            matchSummaryService,
            matchPlayerRepository,
            matchRepository,
            pollingService,
            userRepository,
            userService,
            matchSetupService
    // alphabetical order
    );

    private Match match;
    private Game game;
    private User user;
    private User user2;
    private MatchPlayer matchPlayer;
    private MatchPlayer matchPlayer2;

    @BeforeEach
    public void setup() {
        match = new Match();
        match.setPhase(MatchPhase.SETUP);
        match.setMatchGoal(100);
        match.setStarted(false);

        List<MatchPlayer> matchPlayers = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            User u = new User();
            u.setId(11L + i);
            u.setUsername("username" + i);
            u.setPassword("password" + i);
            u.setToken("token" + i);
            u.setIsAiPlayer(false);
            u.setStatus(UserStatus.ONLINE);

            MatchPlayer mp = new MatchPlayer();
            mp.setMatch(match);
            mp.setUser(u);
            mp.setMatchPlayerId((long) (i + 1));
            mp.setMatchPlayerSlot(i + 1);
            mp.setReady(true);

            matchPlayers.add(mp);

            if (i == 0) {
                match.setPlayer1(u);
                match.setHostId(u.getId());
                user = u;
                matchPlayer = mp;
            }

            if (i == 1) {
                user2 = u;
                matchPlayer2 = mp;
            }
        }

        match.setMatchPlayers(matchPlayers);

        game = new Game();
        game.setGameId(1L);
        game.setMatch(match);
        match.getGames().add(game);
    }

    @Test
    public void testGetMatchesInformation() {
        List<Match> matchList = new ArrayList<>();
        matchList.add(match);

        when(matchRepository.findAll()).thenReturn(matchList);

        List<Match> result = matchService.getMatchesInformation();

        assertEquals(matchList, result);
    }

    @Test
    public void testGetMatchDTOError() {
        String dummyToken = "dummyToken";

        // Mock token lookup to return a valid user (or null if testing auth failure)
        User dummyUser = new User();
        dummyUser.setId(99L); // some test ID
        when(userService.getUserByToken(dummyToken)).thenReturn(dummyUser);

        // Mock match repository to simulate missing match
        when(matchRepository.findMatchByMatchId(1L)).thenReturn(null);

        assertThrows(
                ResponseStatusException.class,
                () -> matchService.getMatchDTO(1L, dummyToken),
                "Expected getMatchDTO to throw ResponseStatusException for nonexistent match");
    }

    @Test
    public void testGetMatchDTOSuccess() {
        String token = "dummyToken";

        // 1. Prepare mock user
        User user = new User();
        user.setId(11L);
        user.setUsername("user1");
        user.setToken(token);

        // 2. Prepare match and assign user to slot 1
        Match match = new Match();
        match.setMatchId(1L);
        match.setHostId(11L);
        match.setHostUsername("user1");
        match.setMatchGoal(100);
        match.setStarted(false);
        match.setPlayer1(user); // <-- This is important to pass the "isUserInMatch" check

        // 3. Prepare MatchPlayer
        MatchPlayer mp1 = new MatchPlayer();
        mp1.setMatchPlayerId(42L);
        mp1.setMatchPlayerSlot(1);
        mp1.setUser(user);
        List<MatchPlayer> matchPlayers = List.of(mp1);
        match.setMatchPlayers(matchPlayers);

        // 4. Mock dependencies
        when(userService.getUserByToken(token)).thenReturn(user);
        when(matchRepository.findMatchByMatchId(1L)).thenReturn(match);

        // 5. Call service
        MatchDTO result = matchService.getMatchDTO(1L, token);

        // 6. Verify result
        assertEquals(1L, result.getMatchId());
        assertEquals(11L, result.getHostId());
        assertEquals("user1", result.getHostUsername());
        assertEquals(100, result.getMatchGoal());
        assertFalse(result.getStarted());

        // 7. Verify player names
        assertEquals("user1", result.getPlayerNames().get(0));
        assertEquals("", result.getPlayerNames().get(1));
        assertEquals("", result.getPlayerNames().get(2));
        assertEquals("", result.getPlayerNames().get(3));

        // 8. Verify matchPlayerIds (should be list of Longs!)
        assertEquals(List.of(42L), result.getMatchPlayerIds());
    }

    @Test
    public void testFindNewHumanHostOrAbortMatch() {
        match.setMatchId(1L);
        given(matchRepository.findById(1L)).willReturn(Optional.of(match));
        given(userRepository.findUserById(Mockito.any())).willReturn(user);
        given(matchPlayerRepository.findByUserAndMatch(Mockito.any(), Mockito.any())).willReturn(matchPlayer);

        doNothing().when(gameService).relayMessageToMatchMessageService(Mockito.any(), Mockito.any(), Mockito.any());

        given(matchPlayerRepository.save(Mockito.any())).willReturn(matchPlayer);
        given(matchRepository.save(Mockito.any())).willReturn(match);

        matchService.findNewHumanHostOrAbortMatch(match);

        verify(userRepository, times(2)).findUserById(Mockito.any());
        verify(matchPlayerRepository).findByUserAndMatch(Mockito.any(), Mockito.any());
        verify(gameService, times(3)).relayMessageToMatchMessageService(Mockito.any(), Mockito.any(), Mockito.any());
        verify(matchPlayerRepository, times(2)).save(Mockito.any());
        verify(matchRepository, times(2)).save(Mockito.any());
    }

    @Test
    public void testLeaveMatch() {
        // Replace one of the existing 4 players with an AI
        MatchPlayer toReplace = match.getMatchPlayers().get(2); // Slot 3
        match.setMatchId(1L);
        given(matchRepository.findById(1L)).willReturn(Optional.of(match));
        User aiPlayer = new User();
        aiPlayer.setUsername("ai");
        aiPlayer.setIsAiPlayer(true);
        aiPlayer.setId(1L);
        aiPlayer.setToken("ai-token");

        MatchPlayer aiMatchPlayer = new MatchPlayer();
        aiMatchPlayer.setMatchPlayerId(toReplace.getMatchPlayerId());
        aiMatchPlayer.setMatchPlayerSlot(toReplace.getMatchPlayerSlot()); // Keep slot 3
        aiMatchPlayer.setMatch(match);
        aiMatchPlayer.setUser(aiPlayer);

        match.getMatchPlayers().set(2, aiMatchPlayer); // Replace at index 2 (slot 3)

        given(matchRepository.findMatchByMatchId(Mockito.anyLong())).willReturn(match);
        given(userRepository.findUserByToken(Mockito.any())).willReturn(user2); // The leaver
        given(userRepository.findUserById(Mockito.anyLong())).willReturn(aiPlayer);
        given(matchPlayerRepository.findByUserAndMatch(user2, match)).willReturn(matchPlayer);

        doNothing().when(gameService).relayMessageToMatchMessageService(Mockito.any(), Mockito.any(), Mockito.any());

        matchService.leaveMatch(1L, "token1", null); // token1 = user2

        verify(matchRepository).findMatchByMatchId(Mockito.anyLong());
        verify(userRepository, atLeastOnce()).findUserByToken(any());
        verify(userRepository).findUserById(Mockito.anyLong());
        verify(gameService, times(2)).relayMessageToMatchMessageService(Mockito.any(), Mockito.any(), Mockito.any());
        verify(matchPlayerRepository, atLeastOnce()).save(any());
        verify(matchRepository, atLeastOnce()).save(any());

    }

    @Test
    public void testPassingAcceptCardsSkipPassing() {
        game.setPhase(GamePhase.SKIP_PASSING);

        given(matchRepository.findById(Mockito.anyLong())).willReturn(Optional.ofNullable(match));
        given(userService.requireUserByToken(Mockito.any())).willReturn(user);
        given(matchPlayerRepository.saveAndFlush(Mockito.any())).willReturn(matchPlayer);

        matchService.passingAcceptCards(1L, null, "1234", true);

        verify(matchRepository).findById(Mockito.anyLong());
        verify(userService).requireUserByToken(Mockito.any());
        verify(matchPlayerRepository).saveAndFlush(Mockito.any());
    }

    @Test
    public void testPlayCardAsHuman() {
        PlayedCardDTO playedCardDTO = new PlayedCardDTO();
        playedCardDTO.setCard("XX");

        given(matchRepository.findById(Mockito.anyLong())).willReturn(Optional.of(match));
        given(userRepository.findUserByToken("token0")).willReturn(user); // Fix
        given(matchPlayerRepository.findByUserAndMatch(user, match)).willReturn(matchPlayer);

        doNothing().when(gameService).playCardAsHuman(Mockito.any(), Mockito.any(), Mockito.any());

        matchService.playCardAsHuman("token0", 1L, playedCardDTO);

        verify(matchRepository).findById(Mockito.anyLong());
        verify(gameService).playCardAsHuman(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testWrapUpCompletedGame() {
        given(matchSummaryService.buildMatchResultHtml(match, game)).willReturn("{test_content}");
        given(matchRepository.save(Mockito.any())).willReturn(match);

        matchService.wrapUpCompletedGame(game);

        verify(matchSummaryService).buildMatchResultHtml(match, game);
        verify(matchRepository, times(2)).save(Mockito.any());
    }

    @Test
    public void testGetPlayerPolling() {
        matchPlayer.setIsHost(true);
        matchPlayer.setMatchScore(200);

        match.setPhase(MatchPhase.IN_PROGRESS);

        game.setPhase(GamePhase.NORMALTRICK);
        game.setCurrentMatchPlayerSlot(1);
        game.setTrickPhase(TrickPhase.RUNNINGTRICK);

        given(matchRepository.findMatchByMatchId(Mockito.anyLong())).willReturn(match);
        given(userRepository.findUserByToken("token0")).willReturn(user);
        given(matchPlayerRepository.findByUserAndMatch(user, match)).willReturn(matchPlayer);
        given(matchPlayerRepository.save(Mockito.any())).willReturn(matchPlayer);
        given(gameRepository.save(Mockito.any())).willReturn(game);

        doNothing().when(gameService).advanceTrickPhaseIfOwnerPolling(Mockito.any());
        given(matchSummaryService.buildMatchResultHtml(Mockito.any(), Mockito.any())).willReturn("{test_content}");
        given(matchRepository.saveAndFlush(Mockito.any())).willReturn(match);
        given(gameService.playSingleAiTurn(Mockito.any(), Mockito.any(), Mockito.any())).willReturn(true);
        given(gameService.finalizeGameIfComplete(Mockito.any())).willReturn(true);
        given(matchRepository.save(Mockito.any())).willReturn(match);
        given(pollingService.getPlayerPollingForPostMatchPhase(Mockito.any(), Mockito.any(), Mockito.anyBoolean()))
                .willReturn(new PollingDTO());

        matchService.getPlayerPolling("token0", 1L); // Fix token here

        verify(matchRepository).findMatchByMatchId(Mockito.anyLong());
        verify(userRepository).findUserByToken(Mockito.any());
        verify(matchPlayerRepository, times(5)).save(any());
        verify(gameService).advanceTrickPhaseIfOwnerPolling(Mockito.any());
        verify(gameRepository).save(Mockito.any());
        verify(matchSummaryService).buildMatchResultHtml(Mockito.any(), Mockito.any());
        verify(matchRepository).saveAndFlush(Mockito.any());
        verify(pollingService).getPlayerPollingForPostMatchPhase(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
    }

    @Test
    public void testHandleConfirmedGame() {
        matchPlayer.setReady(true);
        matchPlayer2.setReady(true);

        given(gameRepository.save(Mockito.any())).willReturn(game);
        given(matchRepository.save(Mockito.any())).willReturn(match);
        when(gameSetupService.createAndStartGameForMatch(any(), any(), any(), any())).thenReturn(game);

        matchService.handleConfirmedGame(match, game);

        verify(gameRepository).save(Mockito.any());
        verify(matchRepository).save(Mockito.any());
        verify(gameSetupService).createAndStartGameForMatch(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void handleConfirmedGame_allReady_triggersGameStart() {
        matchPlayer.setReady(true);
        matchPlayer2.setReady(true);

        when(gameSetupService.createAndStartGameForMatch(any(), any(), any(), any())).thenReturn(game);
        when(gameRepository.save(any())).thenReturn(game);
        when(matchRepository.save(any())).thenReturn(match);

        matchService.handleConfirmedGame(match, game);

        verify(gameSetupService).createAndStartGameForMatch(any(), any(), any(), any());
        verify(matchRepository, atLeastOnce()).save(any());
    }

    @Test
    void handleConfirmedGame_notAllReady_doesNotTriggerGameStart() {
        matchPlayer.setReady(true);
        matchPlayer2.setReady(false);

        matchService.handleConfirmedGame(match, game);

        verify(gameSetupService, never()).createAndStartGameForMatch(any(), any(), any(), any());
    }

    @Test
    void playCardAsHuman_invalidToken_throwsResponseStatusException() {
        // Arrange
        PlayedCardDTO dto = new PlayedCardDTO();
        dto.setCard("AS");

        Long matchId = 1L;

        // Use a real Match, but spy on it to override requireMatchPlayerByToken
        Match match = spy(new Match());
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        Game game = mock(Game.class);
        match.setGames(List.of(game));
        when(game.getPhase()).thenReturn(GamePhase.NORMALTRICK);
        when(game.getMatch()).thenReturn(match);
        when(game.getPhase()).thenReturn(GamePhase.NORMALTRICK);

        doReturn(List.of(game)).when(match).getGames();

        // This forces the exception inside match.requireMatchPlayerByToken
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"))
                .when(match).requireMatchPlayerByToken("invalidToken");

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> matchService.playCardAsHuman("invalidToken", matchId, dto));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Invalid token", exception.getReason());
    }

    @Test
    void wrapUpCompletedGame_summaryServiceFails_throws() {
        when(matchSummaryService.buildMatchResultHtml(match, game)).thenThrow(RuntimeException.class);

        assertThrows(RuntimeException.class, () -> matchService.wrapUpCompletedGame(game));
    }

    @Test
    void getMatchDTO_notFound_throws() {
        String token = "dummyToken";

        // Mock a valid user from token
        User user = new User();
        user.setId(1L);
        when(userService.getUserByToken(token)).thenReturn(user);

        // Simulate match not found
        when(matchRepository.findMatchByMatchId(42L)).thenReturn(null);

        // Assert 404 is thrown
        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> matchService.getMatchDTO(42L, token));

        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatus());
    }

    @Test
    public void testDeleteMatchByHostMatchNull() {
        lenient().when(matchRepository.findMatchByMatchId(Mockito.any())).thenReturn(null);
        lenient().when(userRepository.findUserByToken(Mockito.any())).thenReturn(user);

        assertThrows(
                ResponseStatusException.class,
                () -> matchService.deleteMatchByHost(1L, "1234"),
                "Expected deleteMatchByHost to throw an exception");
    }

    @Test
    public void testDeleteMatchByHostUserNull() {
        lenient().when(matchRepository.findMatchByMatchId(Mockito.any())).thenReturn(match);
        lenient().when(userRepository.findUserByToken(Mockito.any())).thenReturn(null);

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

        lenient().when(matchRepository.findMatchByMatchId(Mockito.any())).thenReturn(match);
        lenient().when(userRepository.findUserByToken(Mockito.any())).thenReturn(user);

        // Act & Assert
        assertThrows(
                ResponseStatusException.class,
                () -> matchService.deleteMatchByHost(1L, "1234"),
                "Expected deleteMatchByHost to throw an exception when user is not host");
    }

    @Test
    public void testDeleteMatchByHostSuccess() {
        // Arrange
        when(matchRepository.findMatchByMatchId(Mockito.any())).thenReturn(match);
        when(userRepository.findUserByToken(Mockito.any())).thenReturn(user);

        // Act
        matchService.deleteMatchByHost(1L, "1234");

        // Assert
        verify(matchRepository).saveAndFlush(match);
        verify(matchRepository).delete(match);
    }

}
