package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.Rank;
import ch.uzh.ifi.hase.soprafs24.constant.Suit;
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

class GameStatsServiceTest {

    @Mock
    private GameStatsRepository gameStatsRepository;

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private GameStatsService gameStatsService;

    private Match match;
    private Game game;

    private List<GameStats> capturedStats;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        match = new Match();
        game = new Game();

        capturedStats = new ArrayList<>();

        // Capture saved GameStats
        doAnswer(invocation -> {
            GameStats stat = invocation.getArgument(0);
            capturedStats.add(stat);
            return stat;
        }).when(gameStatsRepository).save(any(GameStats.class));
    }

    @Test
    void initializeGameStats_createsAllGameStats_correctly() {
        gameStatsService.initializeGameStats(match, game);

        // Check the total number of GameStats created
        assertEquals(Rank.values().length * Suit.values().length, capturedStats.size());

        // Verify some expected values
        GameStats qs = capturedStats.stream()
                .filter(gs -> gs.getRank() == Rank.Q && gs.getSuit() == Suit.S)
                .findFirst()
                .orElse(null);
        assertNotNull(qs);
        assertEquals(13, qs.getPointsWorth(), "Queen of Spades should be worth 13");

        long heartCount = capturedStats.stream().filter(gs -> gs.getSuit() == Suit.H).count();
        long heartPoints = capturedStats.stream().filter(gs -> gs.getSuit() == Suit.H)
                .mapToInt(GameStats::getPointsWorth).sum();

        assertEquals(Rank.values().length, heartCount);
        assertEquals(heartCount, heartPoints, "Each heart should be worth 1 point");

        verify(gameStatsRepository, times(capturedStats.size())).save(any(GameStats.class));
        verify(gameStatsRepository).flush();
    }
}
