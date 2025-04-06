package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;

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
    private Long player1Id;
    private Long player2Id;
    private Long player3Id;
    private Long player4Id;

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

    public Long getPlayer1Id() {
        return player1Id;
    }

    public void setPlayer1Id(User player1) {
        try {
            this.player1Id = player1.getId();
        }
        catch (Exception e) {
            this.player1Id = null;
        }
    }

    public Long getPlayer2Id() {
        return player2Id;
    }

    public void setPlayer2Id(User player2) {
        try {
            this.player2Id = player2.getId();
        }
        catch (Exception e) {
            this.player2Id = null;
        }
    }

    public Long getPlayer3Id() {
        return player3Id;
    }

    public void setPlayer3Id(User player3) {
        try {
            this.player3Id = player3.getId();
        }
        catch (Exception e) {
            this.player3Id = null;
        }
    }

    public Long getPlayer4Id() {
        return player4Id;
    }

    public void setPlayer4Id(User player4) {
        try {
            this.player4Id = player4.getId();
        }
        catch (Exception e) {
            this.player4Id = null;
        }
    }
}
