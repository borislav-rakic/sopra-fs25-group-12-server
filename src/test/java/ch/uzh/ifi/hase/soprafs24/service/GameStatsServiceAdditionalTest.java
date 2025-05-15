
package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.Rank;
import ch.uzh.ifi.hase.soprafs24.constant.Suit;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.repository.GameStatsRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GameStatsServiceAdditionalTest {

    @Mock
    private GameStatsRepository gameStatsRepository;

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private GameStatsService gameStatsService;

    private Game game;
    private Match match;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        game = new Game();
        match = new Match();
        game.setMatch(match);
    }

    @Test
    void getPlayedCards_returnsFilteredResults() {
        List<GameStats> plays = new ArrayList<>();
        when(gameStatsRepository.findByGameAndPlayOrderGreaterThanOrderByPlayOrderAsc(eq(game), eq(0)))
                .thenReturn(plays);

        List<GameStats> result = gameStatsService.getPlayedCards(game);
        assertEquals(plays, result);
    }

    @Test
    void getGameStatsForMatch_validMatch_returnsStats() {
        when(matchRepository.findMatchByMatchId(1L)).thenReturn(match);
        List<GameStats> stats = new ArrayList<>();
        stats.add(new GameStats());
        when(gameStatsRepository.findByMatch(match)).thenReturn(stats);

        List<GameStats> result = gameStatsService.getGameStatsForMatch(1L);
        assertEquals(stats, result);
    }

    @Test
    void getCardsIPassedToPlayer_returnsCorrectCards() {
        GameStats stat = new GameStats();
        stat.setPassedBy(1);
        stat.setPassedTo(2);
        stat.setCardFromString("QS");
        stat.setCardHolder(2);

        when(gameStatsRepository.findAllByGame(game)).thenReturn(List.of(stat));

        List<String> result = gameStatsService.getCardsIPassedToPlayer(game, 1, 2);
        assertEquals(List.of("QS"), result);
    }

    @Test
    void recordCardPlay_updatesFieldsCorrectly() {
        // Arrange
        Match match = new Match();
        match.setMatchId(1L);

        MatchPlayer player = new MatchPlayer();
        player.setMatchPlayerSlot(2);
        player.setMatch(match);

        Game game = new Game();
        game.setMatch(match);
        game.setCurrentPlayOrder(2);
        game.setCurrentTrickNumber(3);
        game.setTrickLeaderMatchPlayerSlot(1);

        GameStats realStats = new GameStats();
        realStats.setRank(Rank.Q);
        realStats.setSuit(Suit.S);
        realStats.setCardHolder(2);

        // Ensure the repository returns the exact instance we're asserting later
        when(gameStatsRepository.findByGameAndRankSuit(game, "QS")).thenReturn(realStats);

        // Act
        gameStatsService.recordCardPlay(game, player, "QS");

        // Assert
        assertEquals(2, realStats.getPlayedBy());
        assertEquals(2, realStats.getPlayOrder());
        assertEquals(2, realStats.getCardHolder());
        assertEquals(3, realStats.getTrickNumber());
        assertEquals(1, realStats.getTrickLeadBySlot());
    }

    @Test
    void updateGameStatsAfterTrickChange_filtersAndSavesCorrectly() {
        GameStats stat = new GameStats();
        stat.setSuit(Suit.H);
        stat.setPlayOrder(0);
        stat.setPossibleHolders(0b1111);

        List<GameStats> stats = List.of(stat);

        when(gameStatsRepository.findAllByGame(game)).thenReturn(stats);
        game.setCurrentMatchPlayerSlot(2);
        game.setCurrentTrick(List.of("2H", "QS"));

        gameStatsService.updateGameStatsAfterTrickChange(game);

        verify(gameStatsRepository).saveAll(any());
        verify(gameStatsRepository).flush();
    }

    @Test
    void cardCodesInSlotFromSlot_filtersCorrectly() {
        Game game = new Game();
        Match match = new Match();
        game.setMatch(match);

        MatchPlayer me = new MatchPlayer();
        me.setMatchPlayerSlot(1);
        me.setHand("2C,3D"); // QS is not in hand

        MatchPlayer them = new MatchPlayer();
        them.setMatchPlayerSlot(2);

        match.setMatchPlayers(List.of(me, them));

        GameStats card = new GameStats(); // QS
        card.setRank(Rank.Q);
        card.setSuit(Suit.S);
        card.setPlayOrder(0); // hasn't been played
        card.setPossibleHolders(0b0100); // binary: 4 => only slot 2 is possible

        when(gameStatsRepository.findAllByGame(game)).thenReturn(List.of(card));

        List<String> result = gameStatsService.cardCodesInSlotFromSlot(game, 2, 1);

        assertTrue(result.contains("QS"), "QS should be returned as a possible card");
    }

    @Test
    void cardCodesInSlotFromSlotGivenLeadingSuit_filtersWithSuitRule() {
        Game game = new Game();
        Match match = new Match();
        game.setMatch(match);

        MatchPlayer me = new MatchPlayer();
        me.setMatchPlayerSlot(1);
        me.setHand("2C,3D");

        MatchPlayer them = new MatchPlayer();
        them.setMatchPlayerSlot(2);

        match.setMatchPlayers(List.of(me, them));

        GameStats spadeCard = new GameStats(); // Valid card
        spadeCard.setRank(Rank.J);
        spadeCard.setSuit(Suit.S);
        spadeCard.setPlayOrder(0);
        spadeCard.setPossibleHolders(0b1111);

        GameStats heartCard = new GameStats(); // Invalid due to suit rule
        heartCard.setRank(Rank.Q);
        heartCard.setSuit(Suit.H);
        heartCard.setPlayOrder(0);
        heartCard.setPossibleHolders(0b1111);

        when(gameStatsRepository.findAllByGame(game)).thenReturn(List.of(spadeCard, heartCard));

        List<String> result = gameStatsService.cardCodesInSlotFromSlotGivenLeadingSuit(game, 2, 1, "S");
        assertTrue(result.contains("JS"));
        assertFalse(result.contains("QH"));
    }

}
