package ch.uzh.ifi.hase.soprafs24.constant;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.Pattern;

public class ConstantsTest {

    @Test
    void testAiMatchPlayerStateValues() {
        assertNotNull(AiMatchPlayerState.READY);
        assertNotNull(AiMatchPlayerState.THINKING);
    }

    @Test
    void testFriendshipStatusValues() {
        assertNotNull(FriendshipStatus.PENDING);
        assertEquals(FriendshipStatus.ACCEPTED.name(), "ACCEPTED");
    }

    @Test
    void testGamePhaseLogic() {
        assertTrue(GamePhase.ABORTED.isNotActive());
        assertTrue(GamePhase.NORMALTRICK.onGoing());
        assertTrue(GamePhase.FIRSTTRICK.inTrick());
        assertTrue(GamePhase.PASSING.inPassing());
        assertFalse(GamePhase.RESULT.inTrick());
    }

    @Test
    void testMatchMessageTypeEnum() {
        assertNotNull(MatchMessageType.PLAYER_LEFT);
        assertEquals("HOST_CHANGED", MatchMessageType.HOST_CHANGED.name());
    }

    @Test
    void testCardCodeRegex_validExamples() {
        Pattern pattern = Pattern.compile(GameConstants.CARD_CODE_REGEX);

        assertTrue(pattern.matcher("2C").matches());
        assertTrue(pattern.matcher("JH").matches());
        assertTrue(pattern.matcher("AD").matches());
        assertTrue(pattern.matcher("QS").matches());
        assertTrue(pattern.matcher("0H").matches()); // 10 is represented as "0"
    }

    @Test
    void testCardCodeRegex_invalidExamples() {
        Pattern pattern = Pattern.compile(GameConstants.CARD_CODE_REGEX);

        assertFalse(pattern.matcher("10H").matches());
        assertFalse(pattern.matcher("1S").matches());
        assertFalse(pattern.matcher("AC,KH").matches()); // comma not allowed
        assertFalse(pattern.matcher("ZZ").matches());
    }

    @Test
    void testCardCodeHandRegex_validExamples() {
        Pattern pattern = Pattern.compile(GameConstants.CARD_CODE_HAND_REGEX);

        assertTrue(pattern.matcher("2C,5H").matches());
        assertTrue(pattern.matcher("JH,KH").matches());
        assertTrue(pattern.matcher("0C,4D,5S,KH").matches()); // 10 is represented as "0"
    }

    @Test
    void testCardCodeHandRegex_invalidExamples() {
        Pattern pattern = Pattern.compile(GameConstants.CARD_CODE_HAND_REGEX);

        assertFalse(pattern.matcher("10H,JH").matches());
        assertFalse(pattern.matcher("1S,").matches());
        assertFalse(pattern.matcher(",AC,KH").matches());
        assertFalse(pattern.matcher("AC ,KH").matches());
        assertFalse(pattern.matcher("ZZ").matches());
    }

    @Test
    void testGameConstantsIntegrity() {
        assertEquals(52, GameConstants.FULL_DECK_CARD_COUNT);
        assertEquals("seed--", GameConstants.SEED_PREFIX);

        // Skip mutable fields or document them clearly
        assertDoesNotThrow(() -> GameConstants.PREVENT_OVERPOLLING);
        assertDoesNotThrow(() -> GameConstants.HOSTS_ARE_ALLOWED_TO_LEAVE_THE_MATCH);
    }

    @Test
    void testMatchPhaseLogic() {
        assertTrue(MatchPhase.IN_PROGRESS.inGame());
        assertTrue(MatchPhase.FINISHED.over());
        assertFalse(MatchPhase.FINISHED.notover());
    }

    @Test
    void testPriorEngagementEnum() {
        assertEquals("MATCH", PriorEngagement.MATCH.name());
    }

    @Test
    void testRankEnumConversion() {
        assertEquals("Q", Rank.Q.getSymbol());
        assertEquals(Rank.Q, Rank.fromSymbol("Q"));
        assertEquals(Rank._0, Rank.fromSymbol("10"));
        assertThrows(IllegalArgumentException.class, () -> Rank.fromSymbol("Z"));
    }

    @Test
    void testStrategyCode() {
        assertEquals(1, Strategy.LEFTMOST.getCode());
        assertEquals(7, Strategy.HYPATIA.getCode());
    }

    @Test
    void testSuitSymbolConversion() {
        assertEquals("C", Suit.C.getSymbol());
        assertEquals(Suit.H, Suit.fromSymbol("H"));
        assertThrows(IllegalArgumentException.class, () -> Suit.fromSymbol("X"));
    }

    @Test
    void testTrickPhaseLogic() {
        assertTrue(TrickPhase.TRICKJUSTCOMPLETED.inTransition());
        assertFalse(TrickPhase.READYFORFIRSTCARD.inTransition());
        assertTrue(TrickPhase.RUNNINGTRICK.inPlay());
        assertFalse(TrickPhase.PROCESSINGTRICK.inPlay());
    }
}
