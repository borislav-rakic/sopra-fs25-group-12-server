package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.util.CardUtils;

public class PlayerCardDTO {
    private Long gameId;
    private Long playerId;
    private String card;
    private int gameNumber;
    private int cardOrder;

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setCard(String card) {
        this.card = card;
        this.cardOrder = CardUtils.calculateCardOrder(card); // ✅ use shared utility
    }

    public String getCard() {
        return card;
    }

    public int getCardOrder() {
        return cardOrder;
    }

    public void setGameNumber(int gameNumber) {
        this.gameNumber = gameNumber;
    }

    public int getGameNumber() {
        return gameNumber;
    }
}
