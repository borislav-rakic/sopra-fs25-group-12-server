package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;

public class GameDTO {
    private int gameNumber;
    private List<PlayerCardDTO> playerCards;
    private Long currentTurnUserId;

    public void setGameNumber(int gameNumber) {
        this.gameNumber = gameNumber;
    }

    public int getGameNumber() {
        return gameNumber;
    }

    public void setPlayerCards(List<PlayerCardDTO> playerCards) {
        this.playerCards = playerCards;
    }

    public List<PlayerCardDTO> getPlayerCards() {
        return playerCards;
    }

    public void setCurrentTurnUserId(Long currentTurnUserId) {
        this.currentTurnUserId = currentTurnUserId;
    }

    public Long getCurrentTurnUserId() {
        return currentTurnUserId;
    }
}
