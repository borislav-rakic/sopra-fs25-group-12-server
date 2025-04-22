package ch.uzh.ifi.hase.soprafs24.model;

import java.util.List;

public class DrawCardResponse {
    private boolean success;
    private String deck_id;
    private List<CardResponse> cards;
    private int remaining;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getDeck_id() {
        return deck_id;
    }

    public void setDeck_id(String deck_id) {
        this.deck_id = deck_id;
    }

    public List<CardResponse> getCards() {
        return cards;
    }

    public void setCards(List<CardResponse> cards) {
        this.cards = cards;
    }

    public int getRemaining() {
        return remaining;
    }

    public void setRemaining(int remaining) {
        this.remaining = remaining;
    }
}
