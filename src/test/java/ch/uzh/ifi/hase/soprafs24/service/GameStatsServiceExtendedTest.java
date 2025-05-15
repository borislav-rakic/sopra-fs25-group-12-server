package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.repository.GameStatsRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameStatsServiceExtendedTest {

    @Mock
    private GameStatsRepository gameStatsRepository;

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private GameStatsService gameStatsService;

    private Match match;
    private Game game;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        match = new Match();
        game = new Game();
        game.setMatch(match);
    }

    @Test
    void deleteGameStatsForMatch_executes() {
        match.setMatchId(42L);
        gameStatsService.deleteGameStatsForMatch(match);
        verify(gameStatsRepository).deleteByMatch(match);
    }

    @Test
    void getPlayerScoreInGame_returnsCorrectScore() {
        List<GameStats> stats = new ArrayList<>();
        GameStats stat1 = new GameStats();
        stat1.setPointsWorth(1);
        GameStats stat2 = new GameStats();
        stat2.setPointsWorth(2);
        stats.add(stat1);
        stats.add(stat2);
        when(gameStatsRepository.findByGameAndPointsBilledTo(game, 1)).thenReturn(stats);
        int score = gameStatsService.getPlayerScoreInGame(1, game);
        assertEquals(3, score);
    }

    @Test
    void getTrickByIndex_returnsCorrectSubset() {
        List<GameStats> plays = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            GameStats stat = new GameStats();
            stat.setPlayOrder(i + 1);
            plays.add(stat);
        }
        when(gameStatsRepository.findByGameAndPlayOrderGreaterThanOrderByPlayOrderAsc(game, 0)).thenReturn(plays);
        List<GameStats> trick = gameStatsService.getTrickByIndex(game, 2);
        assertEquals(4, trick.size());
        assertEquals(9, trick.get(0).getPlayOrder());
    }

    @Test
    void updateGameStatsPointsBilledTo_setsField() {
        GameStats stat = new GameStats();
        when(gameStatsRepository.findByRankSuitAndGame("QS", game)).thenReturn(stat);
        gameStatsService.updateGameStatsPointsBilledTo(game, "QS", 2);
        verify(gameStatsRepository).save(stat);
        assertEquals(2, stat.getPointsBilledTo());
    }

    @Test
    void matchPlayerSlotsLeftAfterMyCard_logicIsCorrect() {
        game.setCurrentMatchPlayerSlot(1);
        game.setCurrentTrick(List.of("QS"));
        List<Integer> remaining = gameStatsService.matchPlayerSlotsLeftAfterMyCard(game);
        assertEquals(List.of(2, 3), remaining);
    }
}
