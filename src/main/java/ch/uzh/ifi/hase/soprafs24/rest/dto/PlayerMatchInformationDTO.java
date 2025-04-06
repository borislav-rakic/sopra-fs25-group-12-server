package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;
import java.util.Map;

public class PlayerMatchInformationDTO {
    private Long matchId;
    private List<String> matchPlayers;
    private String host;
    private int length;
    private boolean started;
    private List<Integer> aiPlayers;

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public Long getMatchId() {
        return matchId;
    }

    public List<String> getMatchPlayers() {
        return matchPlayers;
    }

    public void setMatchPlayers(List<String> matchPlayers) {
        this.matchPlayers = matchPlayers;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public List<Integer> getAiPlayers() {
        return aiPlayers;
    }

    public void setAiPlayers(List<Integer> aiPlayers) {
        this.aiPlayers = aiPlayers;
    }
}
