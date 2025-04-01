package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class PlayedCardDTO {
    private Long gameId;
    private Long playerId;
    private String card;

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
    }

    public String getCard() {
        return card;
    }
}
