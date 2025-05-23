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

        when(player.getHand()).thenReturn("2C,3C");
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
        when(player.getHand()).thenReturn("2C,3C"); // no 3H in hand
        when(game.getCurrentPlayOrder()).thenReturn(0);
        when(game.getCurrentTrickAsString()).thenReturn("3H");
        when(game.getCurrentTrickNumber()).thenReturn(1);
        when(game.getHeartsBroken()).thenReturn(false);
        when(player.getInfo()).thenReturn("Tester");

        // Don't stub CardUtils — real logic will check and return false

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
        when(player.getInfo()).thenReturn("Tester");

        // Card "5H" is not in hand -> should throw
        assertThrows(Exception.class, () -> cardRulesService.validateMatchPlayerCardCode(game, player, "5H"));
    }

    @Test
    public void testValidateUniqueDeckAcrossPlayers_validDeck() {
        Match match = mock(Match.class);

        // Four players with 13 unique cards each, total 52 unique cards
        MatchPlayer player1 = mock(MatchPlayer.class);
        MatchPlayer player2 = mock(MatchPlayer.class);
        MatchPlayer player3 = mock(MatchPlayer.class);
        MatchPlayer player4 = mock(MatchPlayer.class);

        when(player1.getMatchPlayerSlot()).thenReturn(1);
        when(player2.getMatchPlayerSlot()).thenReturn(2);
        when(player3.getMatchPlayerSlot()).thenReturn(3);
        when(player4.getMatchPlayerSlot()).thenReturn(4);

        when(player1.getHand()).thenReturn("2H,3H,4H,5H,6H,7H,8H,9H,0H,JH,QH,KH,AH");
        when(player2.getHand()).thenReturn("2D,3D,4D,5D,6D,7D,8D,9D,0D,JD,QD,KD,AD");
        when(player3.getHand()).thenReturn("2C,3C,4C,5C,6C,7C,8C,9C,0C,JC,QC,KC,AC");
        when(player4.getHand()).thenReturn("2S,3S,4S,5S,6S,7S,8S,9S,0S,JS,QS,KS,AS");

        when(match.getMatchPlayers()).thenReturn(List.of(player1, player2, player3, player4));

        // We use system output capture to verify no errors (optional improvement)
        cardRulesService.validateUniqueDeckAcrossPlayers(match);
    }

    @Test
    public void testValidateUniqueDeckAcrossPlayers_duplicateCard() {
        Match match = mock(Match.class);

        MatchPlayer player1 = mock(MatchPlayer.class);
        MatchPlayer player2 = mock(MatchPlayer.class);
        MatchPlayer player3 = mock(MatchPlayer.class);
        MatchPlayer player4 = mock(MatchPlayer.class);

        when(player1.getMatchPlayerSlot()).thenReturn(1);
        when(player2.getMatchPlayerSlot()).thenReturn(2);
        when(player3.getMatchPlayerSlot()).thenReturn(3);
        when(player4.getMatchPlayerSlot()).thenReturn(4);

        // Intentional duplicate: AH is in player1 and player2
        when(player1.getHand()).thenReturn("2H,3H,4H,5H,6H,7H,8H,9H,0H,JH,QH,KH,AH");
        when(player2.getHand()).thenReturn("2D,3D,4D,5D,6D,7D,8D,9D,0D,JD,QD,KD,AH");
        when(player3.getHand()).thenReturn("2C,3C,4C,5C,6C,7C,8C,9C,0C,JC,QC,KC,AC");
        when(player4.getHand()).thenReturn("2S,3S,4S,5S,6S,7S,8S,9S,0S,JS,QS,KS,AS");

        when(match.getMatchPlayers()).thenReturn(List.of(player1, player2, player3, player4));

        cardRulesService.validateUniqueDeckAcrossPlayers(match);
    }

}
