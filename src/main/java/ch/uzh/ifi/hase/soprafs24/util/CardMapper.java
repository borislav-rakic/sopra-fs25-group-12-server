package ch.uzh.ifi.hase.soprafs24.util;

import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import ch.uzh.ifi.hase.soprafs24.model.Card;
import ch.uzh.ifi.hase.soprafs24.constant.Rank;
import ch.uzh.ifi.hase.soprafs24.constant.Suit;

public class CardMapper {

    public static Card fromGameStats(GameStats stats) {
        Card card = new Card();
        card.setCode(toCardCode(stats));
        return card;
    }

    public static String toCardCode(GameStats stats) {
        return toCardCode(stats.getRank(), stats.getSuit());
    }

    public static String toCardCode(Rank rank, Suit suit) {
        return rank.getSymbol() + suit.getSymbol(); // assuming getSymbol() returns "0", "A", etc.
    }
}
