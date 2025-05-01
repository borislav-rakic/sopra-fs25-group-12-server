package ch.uzh.ifi.hase.soprafs24.model;

import java.util.*;

public class CardTrick {
    private final List<String> cards;

    public CardTrick(String commaSeparated) {
        if (commaSeparated == null || commaSeparated.isBlank()) {
            this.cards = new ArrayList<>();
        } else {
            this.cards = new ArrayList<>(Arrays.asList(commaSeparated.split(",")));
        }
    }

    public void addCardCode(String cardCode) {
        if (cards.size() >= 4) {
            throw new IllegalStateException("Trick already has 4 cards.");
        }
        cards.add(cardCode);
    }

    public List<String> asList() {
        return new ArrayList<>(cards);
    }

    public int size() {
        return cards.size();
    }

    public String getCard(int index) {
        return cards.get(index);
    }

    public void clear() {
        cards.clear();
    }

    public String asString() {
        return String.join(",", cards);
    }
}
