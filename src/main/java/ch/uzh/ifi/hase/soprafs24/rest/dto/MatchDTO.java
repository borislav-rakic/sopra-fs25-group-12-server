package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MatchDTO {
    private Long matchId;
    private List<Long> matchPlayerIds;
    private Long hostId;
    private String hostUsername;
    private int matchGoal;
    private boolean started;
    private Map<Integer, Long> invites;
    private Map<Integer, Integer> aiPlayers;
    private Map<Long, String> joinRequests;
    private Long player1Id;
    private Long player2Id;
    private Long player3Id;
    private Long player4Id;
    private List<String> playerNames;

    private boolean slotAvailable;

    public boolean isSlotAvailable() {
        return slotAvailable;
    }

    public void setSlotAvailable(boolean slotAvailable) {
        this.slotAvailable = slotAvailable;
    }

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

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostUsername(String hostUsername) {
        this.hostUsername = hostUsername;
    }

    public String getHostUsername() {
        return hostUsername;
    }

    public void setMatchGoal(int matchGoal) {
        this.matchGoal = matchGoal;
    }

    public int getMatchGoal() {
        return matchGoal;
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

    public Map<Integer, Integer> getAiPlayers() {
        return aiPlayers;
    }

    public void setAiPlayers(Map<Integer, Integer> aiPlayers) {
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
        } catch (Exception e) {
            this.player1Id = null;
        }
    }

    public Long getPlayer2Id() {
        return player2Id;
    }

    public void setPlayer2Id(User player2) {
        try {
            this.player2Id = player2.getId();
        } catch (Exception e) {
            this.player2Id = null;
        }
    }

    public Long getPlayer3Id() {
        return player3Id;
    }

    public void setPlayer3Id(User player3) {
        try {
            this.player3Id = player3.getId();
        } catch (Exception e) {
            this.player3Id = null;
        }
    }

    public Long getPlayer4Id() {
        return player4Id;
    }

    public void setPlayer4Id(User player4) {
        try {
            this.player4Id = player4.getId();
        } catch (Exception e) {
            this.player4Id = null;
        }
    }

    // Overload functions (simplify testing)
    public void setPlayer1Id(Long player1Id) {
        this.player1Id = player1Id;
    }

    public void setPlayer2Id(Long player2Id) {
        this.player2Id = player2Id;
    }

    public void setPlayer3Id(Long player3Id) {
        this.player3Id = player3Id;
    }

    public void setPlayer4Id(Long player4Id) {
        this.player4Id = player4Id;
    }
    // End of overlad functions

    public List<String> getPlayerNames() {
        return playerNames;
    }

    public void setPlayerNames(List<String> playerNames) {
        this.playerNames = playerNames;
    }
}
