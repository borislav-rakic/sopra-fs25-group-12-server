package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;

public class RoundDTO {
    private int roundNumber;
    private List<PlayerCardDTO> playerCards;
    private Long currentTurnUserId;

    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public int getRoundNumber() {
        return roundNumber;
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
