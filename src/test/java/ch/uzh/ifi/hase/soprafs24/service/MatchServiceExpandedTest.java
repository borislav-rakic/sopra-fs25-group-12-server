package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.MatchSummary;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class MatchServiceExpandedTest {

    @Mock
    private GameRepository gameRepository;
    @Mock
    private GameService gameService;
    @Mock
    private GameSimulationService gameSimulationService;
    @Mock
    private GameSetupService gameSetupService;
    @Mock
    private MatchSummaryService matchSummaryService;
    @Mock
    private MatchPlayerRepository matchPlayerRepository;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private PollingService pollingService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MatchSetupService matchSetupService;

    @MockBean
    private UserService userService;

    @InjectMocks
    private MatchService matchService;

    private Match match;
    private Game game;
    private MatchPlayer mp;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        match = new Match();
        match.setMatchId(1L);
        match.setMatchGoal(50);
        match.setPhase(MatchPhase.BETWEEN_GAMES);
        match.setMatchSummary(new MatchSummary());

        // Create user and attach to MatchPlayer
        User user = new User();
        user.setId(42L);
        user.setUsername("testuser");

        mp = new MatchPlayer();
        mp.setMatchPlayerSlot(1);
        mp.setMatchScore(40);
        mp.setUser(user);

        List<MatchPlayer> players = new ArrayList<>();
        players.add(mp);
        match.setMatchPlayers(players);

        game = new Game();
        game.setGameId(1L);
        game.setPhase(GamePhase.NORMALTRICK);
        game.setMatch(match);

        match.setGames(new ArrayList<>(List.of(game)));

        // Clear mutable structures
        match.getMessages().clear();
        match.getInvites().clear();
        match.getJoinRequests().clear();
        match.getAiPlayers().clear();
    }

    @Test
    public void testAbortMatch_setsAbortedAndCallsCleanup() {
        matchService.abortMatch(match);
        assertEquals(MatchPhase.ABORTED, match.getPhase());
    }

    @Test
    public void testAutoPlayToMatchSummary_delegatesCorrectly() {
        when(matchRepository.findMatchByMatchId(1L)).thenReturn(match);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        matchService.autoPlayToMatchSummary(1L, 0);

        verify(gameSimulationService).autoPlayToMatchSummary(match, game);
    }

    @Test
    public void testAutoPlayToGameSummary_delegatesCorrectly() {
        when(matchRepository.findMatchByMatchId(1L)).thenReturn(match);

        matchService.autoPlayToGameSummary(1L, 0);

        verify(gameSimulationService).autoPlayToGameSummary(match, game);
    }

    @Test
    public void testAutoPlayToLastTrickOfMatch_delegatesAndSaves() {
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(matchRepository.findMatchByMatchId(1L)).thenReturn(match);

        matchService.autoPlayToLastTrickOfMatch(1L);

        verify(gameSimulationService).autoPlayToLastTrickOfMatch(match, game);
        verify(matchPlayerRepository).saveAll(match.getMatchPlayers());
    }

    @Test
    public void testCheckGameAndStartNextIfNeeded_createsGame() {
        match.setPhase(MatchPhase.BETWEEN_GAMES);

        Game finishedGame = new Game();
        finishedGame.setGameId(999L);
        finishedGame.setPhase(GamePhase.FINISHED);
        finishedGame.setMatch(match);

        match.setGames(new ArrayList<>(List.of(finishedGame)));

        when(gameSetupService.createAndStartGameForMatch(eq(match), any(), any(), isNull()))
                .thenReturn(game);

        matchService.checkGameAndStartNextIfNeeded(match);

        verify(gameSetupService).createAndStartGameForMatch(eq(match), any(), any(), isNull());
    }

    @Test
    public void testEstablishRankingOfMatchPlayersInMatch_correctRanksAssigned() {
        MatchPlayer mp1 = new MatchPlayer();
        mp1.setMatchScore(105); // Highest score, should get rank 4
        mp1.setMatchPlayerSlot(1);
        MatchPlayer mp2 = new MatchPlayer();
        mp2.setMatchScore(95); // Second highest score, should get rank 3
        mp2.setMatchPlayerSlot(2);
        MatchPlayer mp3 = new MatchPlayer();
        mp3.setMatchScore(85); // Second lowest score, should get rank 2
        mp3.setMatchPlayerSlot(3);
        MatchPlayer mp4 = new MatchPlayer();
        mp4.setMatchScore(75); // Lowest score, should get rank 1
        mp4.setMatchPlayerSlot(4);

        // Ensure all MatchPlayers are correctly initialized with a User
        mp1.setUser(new User()); // Mock users as needed
        mp2.setUser(new User());
        mp3.setUser(new User());
        mp4.setUser(new User());

        List<MatchPlayer> players = new ArrayList<>();
        players.add(mp1);
        players.add(mp2);
        players.add(mp3);
        players.add(mp4);

        match.setMatchPlayers(players);

        // Run the ranking logic
        matchService.establishRankingOfMatchPlayersInMatch(match);

        // Verify the save call - each player should be saved after ranking
        verify(matchPlayerRepository, times(4)).save(any());

        // Assert the ranks based on the sorted scores (lowest score = lowest rank)
        assertEquals(1, mp4.getRankingInMatch()); // Lowest score should get Rank 1
        assertEquals(2, mp3.getRankingInMatch()); // Second lowest score gets Rank 2
        assertEquals(3, mp2.getRankingInMatch()); // Second highest score gets Rank 3
        assertEquals(4, mp1.getRankingInMatch()); // Highest score should get Rank 4
    }

    @Test
    public void testShouldEndMatch_returnsTrueIfOnePlayerHasEnoughScore() {
        mp.setMatchScore(100);
        assertEquals(true, matchService.shouldEndMatch(match));
    }

    @Test
    public void testShouldEndMatch_returnsFalseIfNoPlayerHasEnoughScore() {
        mp.setMatchScore(10);
        assertEquals(false, matchService.shouldEndMatch(match));
    }
    // === Additional Unit Tests ===

    @Test
    public void testSetExistingMatchSummaryOrCreateIt_createsSummary() {
        Match matchWithoutSummary = new Match();
        matchWithoutSummary.setMatchId(2L);
        when(matchRepository.save(any())).thenReturn(matchWithoutSummary);

        matchService.setExistingMatchSummaryOrCreateIt(matchWithoutSummary, "<html>Summary</html>");

        assertEquals("<html>Summary</html>", matchWithoutSummary.getMatchSummary().getMatchSummaryHtml());
        verify(matchRepository).save(matchWithoutSummary);
    }

    @Test
    public void testAwardScoresToUsersOfFinishedMatch_addsScoreCorrectly() {
        User user = new User();
        user.setId(42L);
        user.setMatchesPlayed(0);
        user.setAvgMatchRanking(0f);
        user.setScoreTotal(0);
        user.setCurrentMatchStreak(0);
        user.setLongestMatchStreak(0);

        mp.setUser(user);
        mp.setGameScore(0); // Best score
        mp.setMatchScore(0);
        mp.setIsAiPlayer(false);
        match.setMatchPlayers(new ArrayList<>(List.of(mp)));

        matchService.awardScoresToUsersOfFinishedMatch(match);

        verify(userRepository).save(user);
        assertEquals(1, user.getMatchesPlayed());
        assertEquals(10 + 20, user.getScoreTotal());
        assertEquals(1, user.getCurrentMatchStreak());
        assertEquals(1, user.getLongestMatchStreak());
    }

    @Test
    public void testAutoPlayFastForwardPoints_setsScoreForAllPlayers() {
        MatchPlayer mp2 = new MatchPlayer();
        mp2.setMatchPlayerSlot(2);
        mp2.setMatchScore(5);
        User dummy = new User();
        dummy.setId(99L);
        mp2.setUser(dummy);

        match.setMatchPlayers(new ArrayList<>(List.of(mp, mp2)));
        when(matchRepository.findMatchByMatchId(1L)).thenReturn(match);

        matchService.autoPlayFastForwardPoints(1L, 50);

        assertEquals(50, mp.getMatchScore());
        assertEquals(50, mp2.getMatchScore());
        verify(matchPlayerRepository, times(2)).save(any());
    }

    @Test
    public void testHighestLevelOfAiPlayers_detectsCorrectly() {
        MatchPlayer aiEasy = new MatchPlayer();
        User aiEasyUser = new User();
        aiEasyUser.setId(2L);
        aiEasy.setUser(aiEasyUser);
        aiEasy.setIsAiPlayer(true);

        MatchPlayer aiHard = new MatchPlayer();
        User aiHardUser = new User();
        aiHardUser.setId(8L);
        aiHard.setUser(aiHardUser);
        aiHard.setIsAiPlayer(true);

        match.setMatchPlayers(new ArrayList<>(List.of(aiEasy, aiHard)));
        int level = matchService.highestLevelOfAiPlayers(match);
        assertEquals(2, level);
    }

    @Test
    public void testGetMatchPlayerSlotForUser_returnsCorrectSlot() {
        User user = mp.getUser();
        match.setPlayer1(user);
        int slot = matchService.getMatchPlayerSlotForUser(match, user);
        assertEquals(1, slot);
    }

    @Test
    public void checkGameAndStartNextIfNeeded_shouldStartNewGameWhenNoActiveGame() {
        match.setPhase(MatchPhase.BETWEEN_GAMES);
        match.setGames(new ArrayList<>()); // No active game

        when(gameSetupService.createAndStartGameForMatch(eq(match), any(), any(), isNull())).thenReturn(game);

        matchService.checkGameAndStartNextIfNeeded(match);

        verify(gameSetupService, times(1)).createAndStartGameForMatch(eq(match), any(), any(), isNull());
    }

    @Test
    public void checkGameAndStartNextIfNeeded_shouldDoNothingIfActiveGameExists() {
        match.setPhase(MatchPhase.BETWEEN_GAMES);
        game.setPhase(GamePhase.NORMALTRICK); // Still ongoing
        match.setGames(new ArrayList<>(List.of(game)));

        matchService.checkGameAndStartNextIfNeeded(match);

        verify(gameSetupService, never()).createAndStartGameForMatch(any(), any(), any(), any());
    }

    @Test
    public void confirmGameResult_resultPhase_shouldFinishMatch() {
        match.setMatchId(1L);
        match.setPhase(MatchPhase.RESULT);
        match.setSlotDidConfirmLastGame(new ArrayList<>());

        User user = new User();
        user.setId(42L);
        user.setToken("token123");
        user.setUsername("testuser");

        mp.setUser(user);
        match.setMatchPlayers(new ArrayList<>(List.of(mp)));
        match.setPlayer1(user); // Required for slot lookup

        when(userRepository.findUserByToken("token123")).thenReturn(user);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(matchRepository.findMatchByMatchId(1L)).thenReturn(match);

        matchService.confirmGameResult("token123", 1L);

        assertEquals(MatchPhase.FINISHED, match.getPhase());
        verify(matchRepository).saveAndFlush(any(Match.class));
    }

    @Test
    public void confirmGameResult_normalPhase_shouldMarkPlayerReady() {
        match.setMatchId(1L);
        match.setPhase(MatchPhase.BEFORE_GAMES);

        User user = mp.getUser();
        user.setToken("token123");
        mp.setUser(user);
        mp.setIsAiPlayer(false);
        mp.setReady(false);
        match.setPlayer1(user); // Required for getMatchPlayerSlotForUser()

        when(userRepository.findUserByToken("token123")).thenReturn(user);
        when(matchRepository.findMatchByMatchId(1L)).thenReturn(match);
        when(matchPlayerRepository.findByUserAndMatch(user, match)).thenReturn(mp);

        // Mock GameEnforcer static behavior if needed (or ensure match.getGames() has
        // one valid game)
        Game activeGame = new Game();
        activeGame.setPhase(GamePhase.NORMALTRICK);
        activeGame.setMatch(match);
        match.setGames(new ArrayList<>(List.of(game)));
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        matchService.confirmGameResult("token123", 1L);

        assertEquals(true, mp.getIsReady());
        verify(matchPlayerRepository).save(mp);
    }

    @Test
    public void getMatchPlayerSlotForUser_shouldReturnCorrectSlot() {
        User user = mp.getUser();
        match.setPlayer1(user);

        Integer slot = matchService.getMatchPlayerSlotForUser(match, user);
        assertEquals(1, slot);
    }

    @Test
    void testMatchPlayerSlotToPlayerSlot() {
        assertEquals(0, matchService.matchPlayerSlotToPlayerSlot(1));
        assertEquals(1, matchService.matchPlayerSlotToPlayerSlot(2));
        assertEquals(2, matchService.matchPlayerSlotToPlayerSlot(3));
        assertEquals(3, matchService.matchPlayerSlotToPlayerSlot(4));
    }

    @Test
    void testPlayerSlotToMatchPlayerSlot() {
        assertEquals(1, matchService.playerSlotToMatchPlayerSlot(0));
        assertEquals(2, matchService.playerSlotToMatchPlayerSlot(1));
        assertEquals(3, matchService.playerSlotToMatchPlayerSlot(2));
        assertEquals(4, matchService.playerSlotToMatchPlayerSlot(3));
    }

    @Test
    void testReplaceMatchPlayerSlotWithAiPlayer_noAvailableAi_throws() {
        Match match = mock(Match.class);
        when(match.getMatchPlayers()).thenReturn(List.of()); // all IDs taken
        when(userRepository.findUserById(any())).thenReturn(null);

        assertThrows(ResponseStatusException.class, () -> matchService.replaceMatchPlayerSlotWithAiPlayer(match, 1));
    }

    @Test
    public void testReplaceMatchPlayerSlotWithAiPlayer_invalidSlot_throws() {
        Match match = new Match();
        match.setMatchPlayers(new ArrayList<>()); // no players

        assertThrows(ResponseStatusException.class, () -> {
            matchService.replaceMatchPlayerSlotWithAiPlayer(match, 5); // invalid slot
        });
    }

    @Test
    void testLeaveMatch_userNotInMatch_throws() {
        // Arrange
        Match match = new Match();
        match.setMatchId(1L);
        match.setJoinRequests(new HashMap<>()); // avoid NPE
        match.setMatchPlayers(List.of()); // empty match players list

        User user = new User();
        user.setId(5L);
        when(userRepository.findUserByToken("some-token")).thenReturn(user);
        when(matchRepository.findMatchByMatchId(1L)).thenReturn(match);

        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            matchService.leaveMatch(1L, "some-token", null);
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void testHandleMatchInResultPhaseOrAborted_callsCleanup() {
        Match matchSpy = spy(match);
        matchService.handleMatchInResultPhaseOrAborted(matchSpy);
        verify(matchRepository).saveAndFlush(matchSpy);
    }

}
