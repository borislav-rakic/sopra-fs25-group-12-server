package ch.uzh.ifi.hase.soprafs24.util;

import ch.uzh.ifi.hase.soprafs24.constant.GameConstants;
import ch.uzh.ifi.hase.soprafs24.constant.Rank;
import ch.uzh.ifi.hase.soprafs24.constant.Suit;
import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import ch.uzh.ifi.hase.soprafs24.model.Card;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CardUtilsTest {

    @Test
    void fromCode_validCode_returnsCard() {
        Card card = CardUtils.fromCode("QS");
        assertNotNull(card);
        assertEquals("QS", card.getCode());
    }

    @Test
    void fromCode_invalidCode_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> CardUtils.fromCode("X"));
    }

    @Test
    void calculateCardOrder_validCard_returnsCorrectOrder() {
        int order = CardUtils.calculateCardOrder("AH");
        assertEquals(84, order); // 14 + 70
    }

    @Test
    void calculateCardOrder_invalidCard_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> CardUtils.calculateCardOrder("ZZ"));
    }

    @Test
    void compareCards_ordersCorrectly() {
        assertTrue(CardUtils.compareCards("2C", "3C") < 0);
    }

    @Test
    void fromGameStats_validStats_returnsCard() {
        GameStats gs = new GameStats();
        gs.setRank(Rank.Q);
        gs.setSuit(Suit.S);

        Card card = CardUtils.fromGameStats(gs);
        assertEquals("QS", card.getCode());
    }

    @Test
    void fromCodes_validList_returnsCards() {
        List<Card> cards = CardUtils.fromCodes(List.of("QS", "0H"));
        assertEquals(2, cards.size());
        assertEquals("QS", cards.get(0).getCode());
    }

    @Test
    void toCode_card_returnsString() {
        Card card = new Card();
        card.setCode("QS");
        assertEquals("QS", CardUtils.toCode(card));
    }

    @Test
    void isValidCardFormat_validAndInvalidInputs() {
        assertTrue(CardUtils.isValidCardFormat("QS"));
        assertFalse(CardUtils.isValidCardFormat("Z9"));
    }

    @Test
    void requireValidCardFormat_invalidCard_throws() {
        assertThrows(IllegalArgumentException.class, () -> CardUtils.requireValidCardFormat("Z1"));
    }

    @Test
    void normalizeCardCodeString_deduplicatesAndSorts() {
        String result = CardUtils.normalizeCardCodeString("KH, 2C, KH");
        assertEquals("2C,KH", result);
    }

    @Test
    void sizeOfCardCodeString_validInput_returnsSize() {
        assertEquals(3, CardUtils.sizeOfCardCodeString("2C,KH,QD"));
    }

    @Test
    void calculateHighestScoreOrder_queenOfSpades_returnsMax() {
        int score = CardUtils.calculateHighestScoreOrder(GameConstants.QUEEN_OF_SPADES);
        assertEquals(999, score);
    }

    @Test
    void rankStrToInt_returnsCorrectValues() {
        assertEquals(14, CardUtils.rankStrToInt("A"));
        assertEquals(10, CardUtils.rankStrToInt("0"));
        assertEquals(0, CardUtils.rankStrToInt("X")); // fallback
    }

    @Test
    void splitCardCodesAsListOfStrings_filtersInvalid() {
        List<String> result = CardUtils.splitCardCodesAsListOfStrings("QS,INVALID,0H");
        assertEquals(2, result.size());
    }

    @Test
    void joinCardCodesSkipSilently_skipsInvalid() {
        String result = CardUtils.joinCardCodesSkipSilently(List.of("QS", "INVALID", "0H"));
        assertEquals("QS,0H", result);
    }

    @Test
    void isCardCodeInHandAsString_detectsCard() {
        assertTrue(CardUtils.isCardCodeInHandAsString("QS", "2C,QS,KH"));
        assertFalse(CardUtils.isCardCodeInHandAsString("KD", "2C,QS,KH"));
    }

    @Test
    void numberOfHeartsCardInCardCodeString_countsCorrectly() {
        assertEquals(2, CardUtils.numberOfHeartsCardInCardCodeString("KH,2H,2C"));
    }

    @Test
    void cardCodeStringMinusCardCode_removesCorrectly() {
        String result = CardUtils.cardCodeStringMinusCardCode("KH,2H,2C", "2H");
        assertEquals("2C,KH", result);
    }
}
