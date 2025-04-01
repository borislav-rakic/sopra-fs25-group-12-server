package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class PlayerDTO {
    private Long userId;
    private String username;
    private boolean isAIPlayer;

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setIsAIPlayer(boolean isAIPlayer) {
        this.isAIPlayer = isAIPlayer;
    }

    public boolean getIsAIPlayer() {
        return isAIPlayer;
    }
}
