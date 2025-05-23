package ch.uzh.ifi.hase.soprafs24.util;

import ch.uzh.ifi.hase.soprafs24.constant.GameConstants;
import ch.uzh.ifi.hase.soprafs24.constant.Rank;
import ch.uzh.ifi.hase.soprafs24.constant.Suit;
import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import ch.uzh.ifi.hase.soprafs24.model.Card;
import ch.uzh.ifi.hase.soprafs24.model.CardResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
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
        String invalidCode = "Z1";

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            CardUtils.requireValidCardFormat(invalidCode);
        });

        assertTrue(exception.getMessage().contains("invalid cardCode"));
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
        String result = CardUtils.getHandWithCardCodeRemoved("KH,2H,2C", "2H");
        assertEquals("2C,KH", result);
    }

    @Test
    void testValidateDrawnCards_validCards() {
        List<CardResponse> cards = new ArrayList<>();

        for (Rank rank : Rank.values()) {
            for (Suit suit : Suit.values()) {
                CardResponse cardResponse = new CardResponse();
                cardResponse.setCode(rank.toString() + suit.toString());

                cards.add(cardResponse);
            }
        }

        boolean expected = true;

        boolean actual = CardUtils.validateDrawnCards(cards);

        assertEquals(expected, actual);
    }

    @Test
    void testValidateDrawnCards_duplicateCards() {
        List<CardResponse> cards = new ArrayList<>();

        for (Rank rank : Rank.values()) {
            for (Suit suit : Suit.values()) {
                CardResponse cardResponse = new CardResponse();
                cardResponse.setCode(rank.toString() + suit.toString());

                cards.add(cardResponse);
            }
        }

        cards.get(0).setCode("QS");

        boolean expected = false;

        boolean actual = CardUtils.validateDrawnCards(cards);

        assertEquals(expected, actual);
    }

    @Test
    void getHandWithCardCodeRemoved_removesAndSortsCorrectly() {
        String result = CardUtils.getHandWithCardCodeRemoved("KH,2C,2H", "2H");
        assertEquals("2C,KH", result);
    }

    @Test
    void isCardCodeInHand_correctlyDetectsPresence() {
        assertTrue(CardUtils.isCardCodeInHand("2C,KH,0D", "KH"));
        assertFalse(CardUtils.isCardCodeInHand("2C,KH,0D", "QS"));
    }

    @Test
    void getHandWithCardCodesAdded_mergesDeduplicatesAndSorts() {
        String result = CardUtils.getHandWithCardCodesAdded("2C,KH", List.of("0D", "2C"));
        assertEquals("2C,0D,KH", result);
    }

    @Test
    void countUniqueCardsInHand_countsDistinctCards() {
        int count = CardUtils.countUniqueCardsInHand("2C,2C,KH");
        assertEquals(2, count);
    }

    @Test
    void countValidUniqueCardsInString_countsOnlyValidDistinctCards() {
        int count = CardUtils.countValidUniqueCardsInString("2C,KH,INVALID,2C");
        assertEquals(2, count);
    }

    @Test
    void reduceHandToCardsInSuit_filtersAndSortsSuit() {
        String result = CardUtils.reduceHandToCardsInSuit("2C,3H,KH,4H", 'H');
        assertEquals("3H,4H,KH", result);
    }

    @Test
    void reduceHandToCardsInSuit_withStringSuit_filtersAndSortsSuit() {
        String result = CardUtils.reduceHandToCardsInSuit("2C,3H,KH,4H", "H");
        assertEquals("3H,4H,KH", result);
    }

    @Test
    void reduceToCardsNotInSuit_filtersAndSorts() {
        String result = CardUtils.reduceToCardsNotInSuit("2C,3H,KH,4H", 'H');
        assertEquals("2C", result);
    }

    @Test
    void reduceToCardsNotInSuit_withStringSuit_filtersAndSorts() {
        String result = CardUtils.reduceToCardsNotInSuit("2C,3H,KH,4H", "H");
        assertEquals("2C", result);
    }

}
