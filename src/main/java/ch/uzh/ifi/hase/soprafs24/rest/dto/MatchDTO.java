package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;

public class MatchDTO {
    private Long id;
    private List<PlayerDTO> players;
    private boolean started;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setPlayers(List<PlayerDTO> players) {
        this.players = players;
    }

    public List<PlayerDTO> getPlayers() {
        return players;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public boolean getStarted() {
        return started;
    }
}
