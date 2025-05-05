package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CardRulesServiceTest {

    private CardRulesService cardRulesService;
    private GameStatsService gameStatsService;

    @BeforeEach
    public void setup() {
        gameStatsService = mock(GameStatsService.class);
        cardRulesService = new CardRulesService(gameStatsService);
    }

    @Test
    public void testGetPlayableCards_FirstTrickWith2C() {
        Game game = mock(Game.class);
        MatchPlayer player = mock(MatchPlayer.class);

        when(game.getPhase()).thenReturn(GamePhase.FIRSTTRICK);
        when(game.getCurrentMatchPlayerSlot()).thenReturn(1);
        when(player.getMatchPlayerSlot()).thenReturn(1);
        when(player.getHand()).thenReturn("2C,5H,9D");
        when(game.getCurrentPlayOrder()).thenReturn(0);
        when(game.getCurrentTrickAsString()).thenReturn("");
        when(game.getHeartsBroken()).thenReturn(false);

        String playable = cardRulesService.getPlayableCardsForMatchPlayer(game, player, false);
        assertEquals("2C", playable);
    }

    @Test
    public void testGetPlayableCards_HeartsNotBrokenOnlyHeartsInHand() {
        Game game = mock(Game.class);
        MatchPlayer player = mock(MatchPlayer.class);

        when(game.getPhase()).thenReturn(GamePhase.NORMALTRICK);
        when(game.getCurrentMatchPlayerSlot()).thenReturn(1);
        when(player.getMatchPlayerSlot()).thenReturn(1);
        when(player.getHand()).thenReturn("2H,3H,KH");
        when(game.getCurrentPlayOrder()).thenReturn(40);
        when(game.getCurrentTrickAsString()).thenReturn("");
        when(game.getHeartsBroken()).thenReturn(false);
        when(player.getInfo()).thenReturn("TestPlayer");

        String playable = cardRulesService.getPlayableCardsForMatchPlayer(game, player, false);
        assertEquals("2H,3H,KH", playable);
    }

    @Test
    public void testDetermineTrickWinner() {
        Game game = mock(Game.class);

        when(game.getCurrentTrick()).thenReturn(List.of("5H", "QH", "2H", "KH"));
        when(game.getTrickMatchPlayerSlotOrder()).thenReturn(List.of(1, 2, 3, 4));
        when(game.getTrickLeaderMatchPlayerSlot()).thenReturn(1);
        when(game.getCurrentTrickNumber()).thenReturn(1);
        when(game.getCurrentTrickAsString()).thenReturn("5H,QH,2H,KH");
        when(game.getTrickMatchPlayerSlotOrderAsString()).thenReturn("1,2,3,4");

        int winner = cardRulesService.determineTrickWinner(game);
        assertEquals(4, winner);
    }

    @Test
    public void testIsCardCodePlayable() {
        String playableCards = "2H,5H,QH";
        assertTrue(CardRulesService.isCardCodePlayable("QH", playableCards));
        assertFalse(CardRulesService.isCardCodePlayable("3D", playableCards));
    }

    @Test
    public void testDescribePassingDirection_Left() {
        String description = cardRulesService.describePassingDirection(1, 1);
        assertTrue(description.contains("to matchPlayerSlot 2"));
    }

    @Test
    public void testDescribePassingDirection_NoPass() {
        String description = cardRulesService.describePassingDirection(0, 1);
        assertTrue(description.contains("No passing"));
    }
}
