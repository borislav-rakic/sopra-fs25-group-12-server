package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;

public class TrickDTO {

    public static class TrickCard {
        private String code;
        private int playerSlot; // 0 = me, 1 = left, 2 = across, 3 = right
        private int order; // 0 to 3

        public TrickCard() {
        }

        public TrickCard(String code, int playerSlot, int order) {
            this.code = code;
            this.playerSlot = playerSlot;
            this.order = order;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public int getPlayerSlot() {
            return playerSlot;
        }

        public void setPlayerSlot(int playerSlot) {
            this.playerSlot = playerSlot;
        }

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }
    }

    private List<TrickCard> cards;
    private int trickLeaderSlot; // relative to this player
    private Integer winningSlot; // 0–3 (relative), or null if ongoing

    public TrickDTO() {
    }

    public TrickDTO(List<TrickCard> cards, int trickLeaderSlot, Integer winningSlot) {
        this.cards = cards;
        this.trickLeaderSlot = trickLeaderSlot;
        this.winningSlot = winningSlot;
    }

    public List<TrickCard> getCards() {
        return cards;
    }

    public void setCards(List<TrickCard> cards) {
        this.cards = cards;
    }

    public int getTrickLeaderSlot() {
        return trickLeaderSlot;
    }

    public void setTrickLeaderSlot(int trickLeaderSlot) {
        this.trickLeaderSlot = trickLeaderSlot;
    }

    public Integer getWinningSlot() {
        return winningSlot;
    }

    public void setWinningSlot(Integer winningSlot) {
        this.winningSlot = winningSlot;
    }
}
