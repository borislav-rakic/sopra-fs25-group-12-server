package ch.uzh.ifi.hase.soprafs24.rest.dto;

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
        this.cardOrder = calculateCardOrder(card);
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

    // Logic for calculating card order
    private int calculateCardOrder(String card) {
        if (card == null || card.length() != 2) {
            throw new IllegalArgumentException("Invalid card format: " + card);
        }

        String rankChar = card.substring(0, 1);
        char suitChar = card.charAt(1);

        int rankValue;
        switch (rankChar) {
            case "J":
                rankValue = 11;
                break;
            case "Q":
                rankValue = 12;
                break;
            case "K":
                rankValue = 13;
                break;
            case "A":
                rankValue = 14;
                break;
            default:
                // parse numeric cards ("2" to "9" and "0" for 10)
                if (rankChar.matches("[2-9]")) {
                    rankValue = Integer.parseInt(rankChar);
                } else if (rankChar.equals("0")) {
                    rankValue = 10;
                } else {
                    throw new IllegalArgumentException("Invalid rank: " + rankChar);
                }
        }

        int suitValue;
        switch (suitChar) {
            case 'C':
                suitValue = 10;
                break;
            case 'D':
                suitValue = 30;
                break;
            case 'S':
                suitValue = 50;
                break;
            case 'H':
                suitValue = 70;
                break;
            default:
                throw new IllegalArgumentException("Invalid suit: " + suitChar);
        }

        return rankValue + suitValue;
    }
}
