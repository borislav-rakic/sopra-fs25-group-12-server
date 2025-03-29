package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;

public class MatchDTO {
    private Long matchId;
    private List<Long> playerIds;
    private boolean started;

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public Long getMatchId() {
        return matchId;
    }

    public void setPlayerIds(List<Long> playerIds) {
        this.playerIds = playerIds;
    }

    public List<Long> getPlayerIds() {
        return playerIds;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public boolean getStarted() {
        return started;
    }
}
