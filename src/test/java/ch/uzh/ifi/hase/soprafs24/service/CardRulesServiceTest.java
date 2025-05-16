package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.exceptions.GameplayException;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
    public void testGetPlayableCardsForMatchPlayerPolling() {
        Game game = mock(Game.class);
        MatchPlayer player = mock(MatchPlayer.class);

        when(game.getPhase()).thenReturn(GamePhase.NORMALTRICK);
        when(game.getCurrentMatchPlayerSlot()).thenReturn(1);
        when(player.getMatchPlayerSlot()).thenReturn(1);
        when(player.getHand()).thenReturn("2C,3C,4C,5C,6C");

        String playable = cardRulesService.getPlayableCardsForMatchPlayerPolling(game, player);
        assertEquals("2C,3C,4C,5C,6C", playable);
    }

    @Test
    public void testGetPlayableCardsForMatchPlayerPlaying() {
        Game game = mock(Game.class);
        MatchPlayer player = mock(MatchPlayer.class);

        when(game.getPhase()).thenReturn(GamePhase.NORMALTRICK);
        when(game.getCurrentMatchPlayerSlot()).thenReturn(1);
        when(player.getMatchPlayerSlot()).thenReturn(1);
        when(player.getHand()).thenReturn("2C,3C,4C,5C,6C");

        String playable = cardRulesService.getPlayableCardsForMatchPlayerPlaying(game, player);
        assertEquals("2C,3C,4C,5C,6C", playable);
    }

    @Test
    public void testGetPlayableCards_FirstTrickWith2C() {
        Game game = mock(Game.class);
        MatchPlayer player = mock(MatchPlayer.class);
        GameStatsService statsService = mock(GameStatsService.class);
        CardRulesService cardRulesService = new CardRulesService(statsService);

        when(game.getPhase()).thenReturn(GamePhase.FIRSTTRICK);
        when(game.getCurrentMatchPlayerSlot()).thenReturn(1);
        when(player.getMatchPlayerSlot()).thenReturn(1);
        when(player.getHand()).thenReturn("2C,3C,4C,5C,6C,7C,8C,9C,5H,9D,0D,JD,QD");
        when(game.getCurrentPlayOrder()).thenReturn(0);
        when(game.getCurrentTrickAsString()).thenReturn("");
        when(game.getCurrentTrickNumber()).thenReturn(1);
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

    // @Test
    // public void testValidateMatchPlayerCardCode() {
    // Game game = mock(Game.class);
    // MatchPlayer player = mock(MatchPlayer.class);

    // when(game.getCurrentTrickNumber()).thenReturn(1);
    // when(game.getPhase()).thenReturn(GamePhase.NORMALTRICK);
    // when(player.getMatchPlayerSlot()).thenReturn(1);
    // when(player.getHand()).thenReturn("2C,3C,4C");

    // assertDoesNotThrow(() -> {
    // cardRulesService.validateMatchPlayerCardCode(game, player, "2C"); // Valid
    // card in hand
    // });
    // }

    // @Test
    // public void testValidateMatchPlayerCardCode_IllegalCardPlayed() {
    // Game game = mock(Game.class);
    // MatchPlayer player = mock(MatchPlayer.class);

    // when(game.getCurrentTrickNumber()).thenReturn(1);
    // when(game.getPhase()).thenReturn(GamePhase.NORMALTRICK);
    // when(player.getMatchPlayerSlot()).thenReturn(1);
    // when(player.getHand()).thenReturn("2C,3C,4C");
    // when(game.getCurrentTrickAsString()).thenReturn("5H");

    // assertThrows(ResponseStatusException.class, () -> {
    // cardRulesService.validateMatchPlayerCardCode(game, player, "3H"); // 3H is
    // not playable in the current trick
    // });
    // }

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
    public void testCalculateTrickPoints() {
        Game game = mock(Game.class);

        when(game.getCurrentTrick()).thenReturn(List.of("5H", "QH", "2H", "KH"));
        when(game.getCurrentTrickNumber()).thenReturn(1);

        int points = cardRulesService.calculateTrickPoints(game, 1);
        assertEquals(4, points);
    }

    @Test
    public void testEnsureHeartBreak_HeartCardPlayed() {
        Game game = mock(Game.class);

        when(game.getHeartsBroken()).thenReturn(false);
        when(game.getCurrentTrick()).thenReturn(List.of("2C", "3C", "4C", "5H"));
        boolean result = cardRulesService.ensureHeartBreak(game);
        assertTrue(result);
    }

    @Test
    public void testDeterminePassingDirection_left() {
        Map<Integer, Integer> passMapLeft = cardRulesService.determinePassingDirection(1);
        assertEquals(2, passMapLeft.get(1));
        assertEquals(3, passMapLeft.get(2));
        assertEquals(4, passMapLeft.get(3));
        assertEquals(1, passMapLeft.get(4));
    }

    @Test
    public void testDeterminePassingDirection_across() {
        Map<Integer, Integer> passMapAcross = cardRulesService.determinePassingDirection(2);
        assertEquals(3, passMapAcross.get(1));
        assertEquals(4, passMapAcross.get(2));
        assertEquals(1, passMapAcross.get(3));
        assertEquals(2, passMapAcross.get(4));
    }

    @Test
    public void testDeterminePassingDirection_right() {
        Map<Integer, Integer> passMapRight = cardRulesService.determinePassingDirection(3);
        assertEquals(4, passMapRight.get(1));
        assertEquals(1, passMapRight.get(2));
        assertEquals(2, passMapRight.get(3));
        assertEquals(3, passMapRight.get(4));
    }

    @Test
    public void testDeterminePassingDirection_nopass() {
        Map<Integer, Integer> passMapNoPass = cardRulesService.determinePassingDirection(0);
        assertTrue(passMapNoPass.isEmpty());
    }

    @Test
    public void testEnsureHeartBreak_HeartsAlreadyBroken() {
        Game game = mock(Game.class);
        when(game.getHeartsBroken()).thenReturn(true);
        boolean result = cardRulesService.ensureHeartBreak(game);
        assertFalse(result);
    }

    @Test
    public void testIsGameReadyForResults_CardsRemaining() {
        Game game = mock(Game.class);
        Match match = mock(Match.class);
        MatchPlayer player1 = mock(MatchPlayer.class);
        MatchPlayer player2 = mock(MatchPlayer.class);

        when(game.getMatch()).thenReturn(match);
        when(match.getMatchPlayers()).thenReturn(List.of(player1, player2));

        when(player1.getHand()).thenReturn("2C,3C");
        when(player2.getHand()).thenReturn("4C,5C");

        assertFalse(cardRulesService.isGameReadyForResults(game));
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

    @Test
    public void testTrickConsistsOnlyOfHearts_AllHearts() {
        List<String> heartsTrick = List.of("2H", "3H", "4H", "KH");

        assertTrue(cardRulesService.trickConsistsOnlyOfHearts(heartsTrick));
    }

    @Test
    public void testTrickConsistsOnlyOfHearts_MixedTrick() {
        List<String> mixedTrick = List.of("2H", "3C", "4H", "KH");

        assertFalse(cardRulesService.trickConsistsOnlyOfHearts(mixedTrick));
    }

    @Test
    public void testGetPlayableCards_PlayerHasNoCards() {
        Game game = mock(Game.class);
        MatchPlayer player = mock(MatchPlayer.class);

        when(game.getPhase()).thenReturn(GamePhase.NORMALTRICK);
        when(game.getCurrentMatchPlayerSlot()).thenReturn(1);
        when(player.getMatchPlayerSlot()).thenReturn(1);
        when(player.getHand()).thenReturn(""); // Player has no cards

        assertThrows(GameplayException.class, () -> {
            cardRulesService.getPlayableCardsForMatchPlayer(game, player, false); // Expect exception due to no cards
        });
    }

    @Test
    public void testGetPassingToMatchPlayerSlot_validLeft() {
        int toSlot = cardRulesService.getPassingToMatchPlayerSlot(1, 1);
        assertEquals(2, toSlot);
    }

    @Test
    public void testGetPassingToMatchPlayerSlot_noPass() {
        int sameSlot = cardRulesService.getPassingToMatchPlayerSlot(0, 2); // No pass round
        assertEquals(2, sameSlot);
    }

    @Test
    public void testNamePassingRecipient_valid() {
        Match match = mock(Match.class);
        when(match.getNameForMatchPlayerSlot(2)).thenReturn("Alice");

        String message = cardRulesService.namePassingRecipient(match, 1, 1);
        assertTrue(message.contains("Your cards will be passed to Alice"));
    }

    @Test
    public void testNamePassingRecipient_noPass() {
        Match match = mock(Match.class);
        String message = cardRulesService.namePassingRecipient(match, 0, 1);

        assertTrue(message.contains("No passing this round"));
    }

    @Test
    public void testValidateMatchPlayerCardCode_validCard() {
        Game game = mock(Game.class);
        MatchPlayer player = mock(MatchPlayer.class);

        when(game.getPhase()).thenReturn(GamePhase.NORMALTRICK);
        when(game.getCurrentMatchPlayerSlot()).thenReturn(1);
        when(player.getMatchPlayerSlot()).thenReturn(1);
        when(player.getHand()).thenReturn("2C,3C,4C");
        when(game.getCurrentTrickAsString()).thenReturn("");
        when(game.getCurrentTrickNumber()).thenReturn(1);
        when(game.getHeartsBroken()).thenReturn(false);
        when(game.getCurrentPlayOrder()).thenReturn(0);

        when(player.hasCardCodeInHand("2C")).thenReturn(true);
        when(player.getInfo()).thenReturn("Tester");

        assertDoesNotThrow(() -> cardRulesService.validateMatchPlayerCardCode(game, player, "2C"));
    }

    @Test
    public void testValidateMatchPlayerCardCode_illegalCard() {
        Game game = mock(Game.class);
        MatchPlayer player = mock(MatchPlayer.class);

        when(game.getPhase()).thenReturn(GamePhase.FIRSTTRICK);
        when(game.getCurrentMatchPlayerSlot()).thenReturn(1);
        when(player.getMatchPlayerSlot()).thenReturn(1);
        when(player.getHand()).thenReturn("2C,3C");
        when(game.getCurrentPlayOrder()).thenReturn(0);
        when(game.getCurrentTrickAsString()).thenReturn("3H");
        when(game.getCurrentTrickNumber()).thenReturn(1);
        when(game.getHeartsBroken()).thenReturn(false);

        when(player.hasCardCodeInHand("3H")).thenReturn(true);
        when(player.getInfo()).thenReturn("Tester");

        assertThrows(Exception.class, () -> cardRulesService.validateMatchPlayerCardCode(game, player, "3H"));
    }

    @Test
    public void testValidateMatchPlayerCardCode_cardNotInHand() {
        Game game = mock(Game.class);
        MatchPlayer player = mock(MatchPlayer.class);

        when(game.getPhase()).thenReturn(GamePhase.NORMALTRICK);
        when(game.getCurrentMatchPlayerSlot()).thenReturn(1);
        when(player.getMatchPlayerSlot()).thenReturn(1);
        when(player.getHand()).thenReturn("2C,3C");
        when(game.getCurrentTrickAsString()).thenReturn("");
        when(game.getCurrentTrickNumber()).thenReturn(1);
        when(game.getHeartsBroken()).thenReturn(false);
        when(game.getCurrentPlayOrder()).thenReturn(0);

        when(player.hasCardCodeInHand("5H")).thenReturn(false);
        when(player.getInfo()).thenReturn("Tester");

        assertThrows(Exception.class, () -> cardRulesService.validateMatchPlayerCardCode(game, player, "5H"));
    }

}
