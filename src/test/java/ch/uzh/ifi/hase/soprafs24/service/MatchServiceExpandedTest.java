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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        mp1.setGameScore(10);
        MatchPlayer mp2 = new MatchPlayer();
        mp2.setGameScore(5);

        List<MatchPlayer> players = new ArrayList<>();
        players.add(mp1);
        players.add(mp2);

        match.setMatchPlayers(players);

        matchService.establishRankingOfMatchPlayersInMatch(match);

        verify(matchPlayerRepository, times(2)).save(any());
        assertEquals(1, mp2.getRankingInMatch());
        assertEquals(2, mp1.getRankingInMatch());
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
        verify(matchRepository).save(match);
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

}
