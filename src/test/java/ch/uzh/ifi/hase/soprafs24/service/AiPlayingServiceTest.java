package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.Strategy;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.repository.GameStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AiPlayingServiceTest {

    @Mock
    private CardRulesService cardRulesService;

    @Mock
    private GameStatsRepository gameStatsRepository;

    @InjectMocks
    private AiPlayingService aiPlayingService;

    private MatchPlayer matchPlayer;
    private Game game;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        matchPlayer = new MatchPlayer();
        matchPlayer.setMatchPlayerSlot(1);
        matchPlayer.setHand("2C,3C,QS,KH,AH");
        game = mock(Game.class);
    }

    @Test
    public void testSelectCardToPlay_LEFTMOST() {
        when(cardRulesService.getPlayableCardsForMatchPlayerPolling(any(), any())).thenReturn("2C,3C,QS");

        String card = aiPlayingService.selectCardToPlay(game, matchPlayer, Strategy.LEFTMOST);
        assertEquals("2C", card);
    }

    @Test
    public void testSelectCardToPlay_RANDOM() {
        when(cardRulesService.getPlayableCardsForMatchPlayerPolling(any(), any())).thenReturn("2C,3C,QS");

        String card = aiPlayingService.selectCardToPlay(game, matchPlayer, Strategy.RANDOM);
        assertTrue(List.of("2C", "3C", "QS").contains(card));
    }

    @Test
    public void testSelectCardToPlay_DUMPHIGHESTFACEFIRST() {
        when(cardRulesService.getPlayableCardsForMatchPlayerPolling(any(), any())).thenReturn("QS,KH,3C");

        String card = aiPlayingService.selectCardToPlay(game, matchPlayer, Strategy.DUMPHIGHESTFACEFIRST);
        assertEquals("QS", card); // QS is most dangerous
    }

    @Test
    public void testSelectCardToPlay_GETRIDOFCLUBSTHENHEARTS() {
        when(cardRulesService.getPlayableCardsForMatchPlayerPolling(any(), any())).thenReturn("2C,3C,7H,9H");

        String card = aiPlayingService.selectCardToPlay(game, matchPlayer, Strategy.GETRIDOFCLUBSTHENHEARTS);
        assertTrue(List.of("2C", "3C").contains(card));
    }

    @Test
    public void testSelectCardToPlay_PREFERBLACK() {
        when(cardRulesService.getPlayableCardsForMatchPlayerPolling(any(), any())).thenReturn("QS,2S,3H,4D");

        String card = aiPlayingService.selectCardToPlay(game, matchPlayer, Strategy.PREFERBLACK);
        assertEquals("2S", card); // QS avoided, 2S is safe
    }

    @Test
    public void testSelectCardToPlay_VOIDSUIT() {
        matchPlayer.setHand("2C,3C,3D,4D,5H,QS,KH");
        when(cardRulesService.getPlayableCardsForMatchPlayerPolling(any(), any())).thenReturn("3D,4D,QS");

        String card = aiPlayingService.selectCardToPlay(game, matchPlayer, Strategy.VOIDSUIT);
        assertTrue(List.of("3D", "4D", "QS").contains(card));
    }

    @Test
    public void testSelectCardToPlay_fallbackWhenNoPlayableCards() {
        when(cardRulesService.getPlayableCardsForMatchPlayerPolling(any(), any())).thenReturn("");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> aiPlayingService.selectCardToPlay(game, matchPlayer, Strategy.LEFTMOST));

        assertTrue(exception.getMessage().contains("AI player has no legal cards to play"));
    }

}
