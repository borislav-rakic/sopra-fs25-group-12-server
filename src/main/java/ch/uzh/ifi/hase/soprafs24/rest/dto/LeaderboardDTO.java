package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class LeaderboardDTO {
    private Long id;
    private String username;
    private boolean isGuest;
    private boolean isAiPlayer;
    private float avgPlacement;
    private float scoreTotal;
    private int gamesPlayed;
    private int moonShots;
    private int perfectGames;
    private int perfectMatches;
    private int currentStreak;
    private int longestStreak;

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean getIsGuest() {
        return isGuest;
    }

    public void setIsGuest(boolean isGuest) {
        this.isGuest = isGuest;
    }

    public boolean getIsAiPlayer() {
        return isAiPlayer;
    }

    public void setIsAiPlayer(boolean isAiPlayer) {
        this.isAiPlayer = isAiPlayer;
    }

    public float getAvgPlacement() {
        return avgPlacement;
    }

    public void setAvgPlacement(float avgPlacement) {
        this.avgPlacement = avgPlacement;
    }

    public float getScoreTotal() {
        return scoreTotal;
    }

    public void setScoreTotal(float scoreTotal) {
        this.scoreTotal = scoreTotal;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public int getMoonShots() {
        return moonShots;
    }

    public void setMoonShots(int moonShots) {
        this.moonShots = moonShots;
    }

    public int getPerfectGames() {
        return perfectGames;
    }

    public void setPerfectGames(int perfectGames) {
        this.perfectGames = perfectGames;
    }

    public int getPerfectMatches() {
        return perfectMatches;
    }

    public void setPerfectMatches(int perfectMatches) {
        this.perfectMatches = perfectMatches;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public int getLongestStreak() {
        return longestStreak;
    }

    public void setLongestStreak(int longestStreak) {
        this.longestStreak = longestStreak;
    }
}
