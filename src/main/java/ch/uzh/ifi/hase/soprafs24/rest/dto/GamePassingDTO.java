package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;

public class GamePassingDTO {
    private Long playerId;
    private List<String> cards; // exactly three cards

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public List<String> getCards() {
        return cards;
    }

    public void setCards(List<String> cards) {
        this.cards = cards;
    }

    public String getCardsAsString() {
        return cards != null ? "[" + String.join(",", cards) + "]" : "[]";
    }
}
