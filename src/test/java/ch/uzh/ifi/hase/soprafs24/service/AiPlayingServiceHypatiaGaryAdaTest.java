package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.Strategy;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class AiPlayingServiceHypatiaGaryAdaTest {

    @Mock
    private CardRulesService cardRulesService;

    @Mock
    private GameStatsRepository gameStatsRepository;

    @InjectMocks
    private AiPlayingService aiPlayingService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // Helper to build a test player
    private MatchPlayer createMatchPlayer(long userId, int slot, List<String> takenCards) {
        MatchPlayer player = new MatchPlayer();
        player.setMatchPlayerSlot(slot);
        player.setTakenCards(takenCards); // fixed line
        User user = new User();
        user.setId(userId);
        player.setUser(user);
        return player;
    }

    private void mockCommonGameSetup(Game game, List<String> trick, List<Integer> trickOrder, String leadSuit) {
        when(game.getCurrentTrick()).thenReturn(trick);
        when(game.getTrickMatchPlayerSlotOrder()).thenReturn(trickOrder);
        when(game.getSuitOfFirstCardInCurrentTrick()).thenReturn(leadSuit);
    }

    private void mockGameStatsForAllPlayableCards(List<String> cards, Game game, int holderBitmask) {
        for (String card : cards) {
            GameStats stats = new GameStats();
            stats.setPossibleHolders(holderBitmask);
            stats.setPassedBy(0);
            stats.setPassedTo(0);
            when(gameStatsRepository.findByRankSuitAndGame(eq(card), eq(game)))
                    .thenReturn(stats);
        }
    }

    @Test
    public void testAdaStrategy_winsSafely() {
        Game game = mock(Game.class);
        MatchPlayer player = createMatchPlayer(9, 2, List.of("QS", "KH", "JH", "QH", "4H")); // moonshot

        mockCommonGameSetup(game, List.of("2H", "3H"), List.of(1, 2, 3, 4), "H");
        List<String> playable = List.of("5H", "6H", "7H");

        when(cardRulesService.getPlayableCardsForMatchPlayerPolling(eq(game), eq(player)))
                .thenReturn(String.join(",", playable));

        mockGameStatsForAllPlayableCards(playable, game, 0b0010); // Only slot 2 has them

        String selected = aiPlayingService.selectCardToPlay(game, player, Strategy.ADA);
        assertEquals("5H", selected);
    }

    @Test
    public void testGaryStrategy_dumpsSafely() {
        Game game = mock(Game.class);
        MatchPlayer player = createMatchPlayer(8, 3, List.of());

        mockCommonGameSetup(game, List.of("2H", "5H"), List.of(1, 2, 3, 4), "H");
        List<String> playable = List.of("QS", "4C", "6H");

        when(cardRulesService.getPlayableCardsForMatchPlayerPolling(eq(game), eq(player)))
                .thenReturn(String.join(",", playable));

        mockGameStatsForAllPlayableCards(playable, game, 0b1111);

        String selected = aiPlayingService.selectCardToPlay(game, player, Strategy.GARY);
        assertTrue(List.of("6H", "QS").contains(selected)); // dumps safely
    }

    @Test
    public void testHypatiaStrategy_picksSafestWin() {
        Game game = mock(Game.class);
        MatchPlayer player = createMatchPlayer(7, 2, List.of("QS", "KH", "9H", "8H", "7H")); // moonshot attempt

        mockCommonGameSetup(game, List.of("2H", "3H"), List.of(1, 2, 3, 4), "H");
        List<String> playable = List.of("6H", "7H", "8H");

        when(cardRulesService.getPlayableCardsForMatchPlayerPolling(eq(game), eq(player)))
                .thenReturn(String.join(",", playable));

        mockGameStatsForAllPlayableCards(playable, game, 0b0010);

        String selected = aiPlayingService.selectCardToPlay(game, player, Strategy.HYPATIA);
        assertEquals("6H", selected); // safe win
    }
}
