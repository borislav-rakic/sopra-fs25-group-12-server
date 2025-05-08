package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;

public class TrickDTO {

    public static class TrickCard {
        private String code;
        private int position; // 0 = me, 1 = left, 2 = across, 3 = right
        private int order; // 0 to 3

        public TrickCard() {
        }

        public TrickCard(String code, int position, int order) {
            this.code = code;
            this.position = position;
            this.order = order;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }
    }

    private List<TrickCard> cards;
    private int trickLeaderPosition; // relative to this player
    private Integer winningPosition; // 0–3 (relative), or null if ongoing

    public TrickDTO() {
    }

    public TrickDTO(List<TrickCard> cards, int trickLeaderPosition, Integer winningPosition) {
        this.cards = cards;
        this.trickLeaderPosition = trickLeaderPosition;
        this.winningPosition = winningPosition;
    }

    public List<TrickCard> getCards() {
        return cards;
    }

    public void setCards(List<TrickCard> cards) {
        this.cards = cards;
    }

    public int getTrickLeaderPosition() {
        return trickLeaderPosition;
    }

    public void setTrickLeaderPosition(int trickLeaderPosition) {
        this.trickLeaderPosition = trickLeaderPosition;
    }

    public Integer getWinningPosition() {
        return winningPosition;
    }

    public void setWinningPosition(Integer winningPosition) {
        this.winningPosition = winningPosition;
    }
}
