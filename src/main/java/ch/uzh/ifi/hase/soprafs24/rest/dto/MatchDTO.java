package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MatchDTO {
    private Long matchId;
    private List<Long> matchPlayerIds;
    private String host;
    private int length;
    private boolean started;
    private Map<Integer, Long> invites;
    private List<Integer> aiPlayers;
    private Map<Long, String> joinRequests;

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchPlayerIds(List<MatchPlayer> matchPlayers) {
        List<Long> matchPlayerIds = new ArrayList<>();

        for (MatchPlayer matchPlayer : matchPlayers) {
            matchPlayerIds.add(matchPlayer.getMatchPlayerId());
        }

        this.matchPlayerIds = matchPlayerIds;
    }

    public List<Long> getMatchPlayerIds() {
        return matchPlayerIds;
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
    
    public Map<Long, String> getJoinRequests() {
        return joinRequests;
    }

    public void setJoinRequests(Map<Long, String> joinRequests) {
        this.joinRequests = joinRequests;
    }
}
