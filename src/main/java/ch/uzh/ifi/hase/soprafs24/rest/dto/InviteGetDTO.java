package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class InviteGetDTO {
    private Long matchId;
    private int playerSlot;
    private int matchPlayerSlot;
    private String fromUsername;
    private Long hostId;
    private Long userId;

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public int getPlayerSlot() {
        return playerSlot;
    }

    public int getMatchPlayerSlot() {
        return matchPlayerSlot;
    }

    public void setMatchPlayerSlot(int matchPlayerSlot) {
        this.matchPlayerSlot = matchPlayerSlot;
        this.playerSlot = this.matchPlayerSlot - 1;
    }

    public String getFromUsername() {
        return fromUsername;
    }

    public void setFromUsername(String fromUsername) {
        this.fromUsername = fromUsername;
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
