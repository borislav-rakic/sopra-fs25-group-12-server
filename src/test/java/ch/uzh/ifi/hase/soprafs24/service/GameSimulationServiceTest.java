package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameStatsRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GameSimulationServiceTest {

    @Mock
    private CardRulesService cardRulesService;

    @Mock
    private GameService gameService;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameStatsRepository gameStatsRepository;

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @InjectMocks
    private GameSimulationService gameSimulationService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    private Match createMockMatchWithPlayers() {
        Match match = new Match();
        List<MatchPlayer> players = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            MatchPlayer mp = new MatchPlayer();
            mp.setMatchPlayerSlot(i + 1);
            mp.setMatchScore(0);
            players.add(mp);
        }
        match.setMatchPlayers(players);
        match.setMatchGoal(100);
        return match;
    }

    @Test
    public void testAutoPlayToLastTrickOfGame_setsFinalTrickState() {
        Game game = new Game();
        Match match = createMockMatchWithPlayers();

        when(gameRepository.saveAndFlush(any())).thenReturn(game);
        when(gameRepository.save(any())).thenReturn(game);

        gameSimulationService.autoPlayToLastTrickOfGame(match, game, 0);

        assertEquals(GamePhase.FINALTRICK, game.getPhase());
        assertEquals(13, game.getCurrentTrickNumber());
        assertEquals(1, game.getCurrentMatchPlayerSlot());
        assertEquals(1, game.getTrickLeaderMatchPlayerSlot());
        assertEquals(48, game.getCurrentPlayOrder());
        assertTrue(game.getHeartsBroken());
        assertNotNull(game.getCurrentTrick());
        assertEquals(0, game.getCurrentTrick().size());

        // Verify players were saved
        verify(matchPlayerRepository, times(4)).save(any(MatchPlayer.class));
        verify(gameRepository, atLeastOnce()).saveAndFlush(game);
    }

    @Test
    public void generateRandomScores_returnsFourIntegersSummingTo26_oneIsZero() {
        List<Integer> scores = GameSimulationService.generateRandomScores();

        assertNotNull(scores);
        assertEquals(4, scores.size());
        assertEquals(26, scores.stream().mapToInt(Integer::intValue).sum());
        long zeros = scores.stream().filter(i -> i == 0).count();
        assertEquals(1, zeros, "Exactly one score should be zero");
    }

    @Test
    public void testAutoPlayToMatchSummary_runsSimulationAndSavesEntities() {
        Match match = createMockMatchWithPlayers();
        Game game = new Game();
        game.setGameNumber(1);
        match.setMatchGoal(50);

        when(gameRepository.save(any())).thenReturn(game);

        gameSimulationService.autoPlayToMatchSummary(match, game);

        verify(matchPlayerRepository, atLeast(4)).save(any(MatchPlayer.class));
        verify(gameRepository, atLeast(1)).save(any(Game.class));
    }

    @Test
    public void testAutoPlayToGameSummary_runsSimulationAndTransfersStats() {
        Match match = createMockMatchWithPlayers();
        Game game = new Game();
        game.setGameNumber(1);
        match.setMatchGoal(60); // Higher goal → force simulation loop

        GameStats stat = new GameStats();
        when(gameRepository.save(any())).thenReturn(game);
        when(gameStatsRepository.findAllByGame(any())).thenReturn(Collections.singletonList(stat));

        gameSimulationService.autoPlayToGameSummary(match, game);

        verify(matchPlayerRepository, atLeast(4)).save(any(MatchPlayer.class));
        verify(gameRepository, atLeast(1)).save(any(Game.class));
        verify(gameStatsRepository).save(any(GameStats.class));
    }

    @Test
    public void testAutoPlayToLastTrickOfMatchThree_stopsAtGameThreeAndMovesStats() {
        Match match = createMockMatchWithPlayers();
        Game game = new Game();
        game.setGameNumber(1);
        match.setMatchGoal(100); // Loop should hit game number >= 3

        GameStats stat = new GameStats();
        when(gameRepository.save(any())).thenReturn(game);
        when(gameStatsRepository.findAllByGame(any())).thenReturn(Collections.singletonList(stat));

        gameSimulationService.autoPlayToLastTrickOfMatchThree(match, game);

        verify(gameRepository, atLeast(1)).save(any(Game.class));
        verify(matchPlayerRepository, atLeast(4)).save(any(MatchPlayer.class));
        verify(gameStatsRepository).save(any(GameStats.class));
    }

    @Test
    public void autoPlayToLastTrickOfGame_setsFinalTrickState() {
        // Arrange
        Game game = new Game();
        game.setGameId(1L);
        Match match = mock(Match.class);
        List<MatchPlayer> mockPlayers = Arrays.asList(new MatchPlayer(), new MatchPlayer(), new MatchPlayer(),
                new MatchPlayer());

        when(match.getMatchPlayersSortedBySlot()).thenReturn(mockPlayers);

        GameRepository gameRepository = mock(GameRepository.class);
        MatchPlayerRepository matchPlayerRepository = mock(MatchPlayerRepository.class);

        GameSimulationService simService = new GameSimulationService(
                gameRepository,
                mock(GameStatsRepository.class),
                matchPlayerRepository);

        // Act
        simService.autoPlayToLastTrickOfGame(match, game, 0);

        // Assert
        assertEquals(GamePhase.FINALTRICK, game.getPhase());
        assertEquals(13, game.getCurrentTrickNumber());
        assertTrue(game.getHeartsBroken());
    }

    @Test
    public void isTrickComplete_returnsTrueWhenTrickHas4Cards() {
        Game game = new Game();
        game.setCurrentTrick(Arrays.asList("2H", "3D", "QC", "AS"));
        GameSimulationService simService = new GameSimulationService(null, null, null);

        assertTrue(simService.isTrickComplete(game));
    }

    @Test
    public void getMaxScore_returnsHighestScoreAmongPlayers() {
        Match match = new Match();
        MatchPlayer p1 = new MatchPlayer();
        p1.setMatchScore(12);
        MatchPlayer p2 = new MatchPlayer();
        p2.setMatchScore(28);
        MatchPlayer p3 = new MatchPlayer();
        p3.setMatchScore(14);
        match.setMatchPlayers(Arrays.asList(p1, p2, p3));

        GameSimulationService simService = new GameSimulationService(null, null, null);
        int max = simService.getMaxScore(match, new Game());

        assertEquals(28, max);
    }

    @Test
    public void testAutoPlayToLastTrickOfMatch_updatesStatsAndScores() {
        Game game = new Game();
        game.setGameNumber(1);
        Match match = createMockMatchWithPlayers();

        GameStats stat = new GameStats();
        List<GameStats> statsList = List.of(stat);

        when(gameRepository.save(any())).thenReturn(game);
        when(gameStatsRepository.findAllByGame(any())).thenReturn(statsList);

        gameSimulationService.autoPlayToLastTrickOfMatch(match, game);

        // Confirm stats are moved to new game
        verify(gameStatsRepository, atLeastOnce()).save(stat);

        // Confirm scores updated
        verify(matchPlayerRepository, atLeast(4)).save(any(MatchPlayer.class));
        verify(gameRepository, atLeastOnce()).save(game);
    }

    @Test
    public void testGenerateRandomScores_validOutput() {
        List<Integer> scores = GameSimulationService.generateRandomScores();

        assertNotNull(scores);
        assertEquals(4, scores.size());
        assertEquals(26, scores.stream().mapToInt(Integer::intValue).sum());
        assertEquals(1, scores.stream().filter(s -> s == 0).count());
    }

    @Test
    public void testIsTrickComplete_returnsTrueIfTrickHasFourCards() {
        Game game = new Game();
        game.setCurrentTrick(Arrays.asList("AC", "2D", "3S", "4H"));

        boolean result = gameSimulationService.isTrickComplete(game);
        assertTrue(result);
    }

    @Test
    public void testAutoPlayToLastTrickOfMatch_loopExitAfterLimit() {
        Game game = new Game();
        game.setGameNumber(1);
        Match match = createMockMatchWithPlayers();
        match.setMatchGoal(1000); // High goal to force loop

        when(gameRepository.save(any())).thenReturn(game);
        when(gameStatsRepository.findAllByGame(any())).thenReturn(Collections.emptyList());

        gameSimulationService.autoPlayToLastTrickOfMatch(match, game);

        // Confirm it did not go infinite or fail
        // Each save is run twice per cycle, so it should not be more than about 2*16
        // invocations of save.
        verify(gameRepository, atMost(32)).save(any());
    }

}
