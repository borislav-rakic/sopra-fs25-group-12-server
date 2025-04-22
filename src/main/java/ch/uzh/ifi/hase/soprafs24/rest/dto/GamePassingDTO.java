package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;

public class GamePassingDTO {
    private Long gameId;
    private Long playerId;
    private List<String> cards; // exactly three cards

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

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
}
