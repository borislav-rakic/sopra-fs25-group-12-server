package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.model.Card;
import ch.uzh.ifi.hase.soprafs24.util.CardUtils;

public class PlayerCardDTO {
    private Long gameId;
    private Long playerId;
    private Card card;
    private int gameNumber;

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

    public Card getCard() {
        return card;
    }

    public void setCard(String cardCode) {
        this.card = CardUtils.fromCode(cardCode); // Convert to full Card object
    }

    public int getCardOrder() {
        return card.getCardOrder(); // Delegate to the Card object
    }

    public int getGameNumber() {
        return gameNumber;
    }

    public void setGameNumber(int gameNumber) {
        this.gameNumber = gameNumber;
    }
}
