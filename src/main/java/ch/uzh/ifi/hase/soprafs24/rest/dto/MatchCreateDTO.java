package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.ArrayList;
import java.util.List;

public class MatchCreateDTO {
    private List<Long> playerIds;

    public void setPlayerIds(List<Long> playerIds) {
        System.out.println("LIST: " + playerIds.getClass()); this.playerIds = playerIds;
    }

    public List<Long> getPlayerIds() {
        return playerIds;
    }
}
