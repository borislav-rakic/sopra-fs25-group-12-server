package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;

public class MatchCreateDTO {
    private Long matchId;
    private String name;
    private List<PlayerDTO> players;

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public Long getMatchId() {
        return matchId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setPlayers(List<PlayerDTO> players) {
        this.players = players;
    }

    public List<PlayerDTO> getPlayers() {
        return players;
    }
}
