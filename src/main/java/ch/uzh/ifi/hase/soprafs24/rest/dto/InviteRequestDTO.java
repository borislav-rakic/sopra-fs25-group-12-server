package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class InviteRequestDTO {

    private Long userId;
    private Integer playerSlot;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getPlayerSlot() {
        return playerSlot;
    }

    public void setPlayerSlot(Integer playerSlot) {
        this.playerSlot = playerSlot;
    }
}
