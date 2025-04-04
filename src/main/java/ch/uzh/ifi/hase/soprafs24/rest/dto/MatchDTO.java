package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;
import java.util.Map;

public class MatchDTO {
    private Long matchId;
    private List<Long> playerIds;
    private String host;
    private int length;
    private boolean started;
    private Map<Integer, Long> invites;
    private List<Integer> aiPlayers;

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

    public void setHost(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getLength() {
        return length;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public boolean getStarted() {
        return started;
    }

    public Map<Integer, Long> getInvites() {
        return invites;
    }

    public void setInvites(Map<Integer, Long> invites) {
        this.invites = invites;
    }

    public List<Integer> getAiPlayers() {
        return aiPlayers;
    }
    
    public void setAiPlayers(List<Integer> aiPlayers) {
        this.aiPlayers = aiPlayers;
    }
    
}
