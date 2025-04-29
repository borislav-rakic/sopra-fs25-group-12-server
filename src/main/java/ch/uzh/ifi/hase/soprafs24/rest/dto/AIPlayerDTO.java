package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class AIPlayerDTO {

    private int difficulty; // 0 = Easy, 1 = Medium, 2 = Difficult
    private int slot; // available slots are 1, 2 and 3.

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

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }
}
