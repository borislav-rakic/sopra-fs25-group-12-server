package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class AIPlayerDTO {

    // gets sent from frontend to backend

    private int difficulty; // 0 = Easy, 1 = Medium, 2 = Difficult
    private int playerSlot; // available slots are 1, 2 and 3.

    public AIPlayerDTO() {
    }

    public AIPlayerDTO(int difficulty) {
        this.difficulty = difficulty;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public int getPlayerSlot() {
        return playerSlot;
    }

    public void setPlayerSlot(int playerSlot) {
        this.playerSlot = playerSlot;
    }
}
