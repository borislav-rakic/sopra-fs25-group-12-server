package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayerDTO;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


@Entity
@Table(name = "MATCH")
public class Match implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long matchId;

    @ElementCollection
    @Column(name = "player_id")
    private List<Long> playerIds;

    @Column(name = "host")
    private String host;

    @Column(name = "length")
    private int length;

    @Column
    private boolean started;

    @ElementCollection
    @CollectionTable(name = "match_invites", joinColumns = @JoinColumn(name = "match_id"))
    @MapKeyColumn(name = "slot_index")
    @Column(name = "user_id")
    private Map<Integer, Long> invites = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "match_ai_players", joinColumns = @JoinColumn(name = "match_id"))
    @Column(name = "difficulty")
    private List<Integer> aiPlayers = new ArrayList<>();

    public List<Integer> getAiPlayers() {
        return aiPlayers;
    }
    
    public void setAiPlayers(List<Integer> aiPlayers) {
        this.aiPlayers = aiPlayers;
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

    public void setHost(String host) { this.host = host; }

    public String getHost() { return host; }

    public void setLength(int length) { this.length = length; }

    public int getLength() { return length; }

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

}
