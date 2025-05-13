package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class LeaderboardDTO {
    private Long id;
    private String username;
    private boolean isGuest;
    private boolean isAiPlayer;
    private float avgGameRanking;
    private float avgMatchRanking;
    private float scoreTotal;
    private int gamesPlayed;
    private int matchesPlayed;
    private int moonShots;
    private int perfectGames;
    private int perfectMatches;
    private int currentMatchStreak;
    private int longestMatchStreak;
    private int currentGameStreak;
    private int longestGameStreak;

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

    public boolean isGuest() {
        return isGuest;
    }

    public void setIsGuest(boolean isGuest) {
        this.isGuest = isGuest;
    }

    public boolean isAiPlayer() {
        return isAiPlayer;
    }

    public void setIsAiPlayer(boolean isAiPlayer) {
        this.isAiPlayer = isAiPlayer;
    }

    public float getAvgGameRanking() {
        return avgGameRanking;
    }

    public void setAvgGameRanking(float avgGameRanking) {
        this.avgGameRanking = avgGameRanking;
    }

    public float getAvgMatchRanking() {
        return avgMatchRanking;
    }

    public void setAvgMatchRanking(float avgMatchRanking) {
        this.avgMatchRanking = avgMatchRanking;
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

    public int getMatchesPlayed() {
        return matchesPlayed;
    }

    public void setMatchesPlayed(int matchesPlayed) {
        this.matchesPlayed = matchesPlayed;
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

    public int getCurrentGameStreak() {
        return currentGameStreak;
    }

    public void setCurrentGameStreak(int currentGameStreak) {
        this.currentGameStreak = currentGameStreak;
    }

    public int getCurrentMatchStreak() {
        return currentMatchStreak;
    }

    public void setCurrentMatchStreak(int currentMatchStreak) {
        this.currentMatchStreak = currentMatchStreak;
    }

    public int getLongestGameStreak() {
        return longestGameStreak;
    }

    public void setLongestGameStreak(int longestGameStreak) {
        this.longestGameStreak = longestGameStreak;
    }

    public int getLongestMatchStreak() {
        return longestMatchStreak;
    }

    public void setLongestMatchStreak(int longestMatchStreak) {
        this.longestMatchStreak = longestMatchStreak;
    }
}
