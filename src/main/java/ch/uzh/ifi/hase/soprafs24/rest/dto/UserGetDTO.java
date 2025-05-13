package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.time.LocalDate;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

// only include public information
public class UserGetDTO {

  private Long id;
  private String username;
  private UserStatus status;
  private int avatar;
  private LocalDate birthday;
  private boolean isGuest;
  private boolean isAiPlayer;
  private int scoreTotal;
  private int gamesPlayed;
  private int matchesPlayed;
  private float avgGameRanking;
  private float avgMatchRanking;
  private int moonShots;
  private int perfectGames;
  private int perfectMatches;
  private int currentGameStreak;
  private int longestGameStreak;
  private int currentMatchStreak;
  private int longestMatchStreak;

  public UserGetDTO() {
    // default constructor required by mapping libraries
  }

  public UserGetDTO(Long id, String username, UserStatus status, int avatar, LocalDate birthday,
      int scoreTotal, int gamesPlayed, int avgGameRanking, int avgMatchRanking, int moonShots,
      int perfectGames, int perfectMatches, int currentGameStreak, int longestGameStreak, int currentMatchStreak,
      int longestMatchStreak,
      boolean isAiPlayer, boolean isGuest) {
    this.id = id;
    this.username = username;
    this.status = status;
    this.avatar = avatar;
    this.birthday = birthday;
    this.scoreTotal = scoreTotal;
    this.gamesPlayed = gamesPlayed;
    this.avgGameRanking = avgGameRanking;
    this.avgMatchRanking = avgMatchRanking;
    this.moonShots = moonShots;
    this.perfectGames = perfectGames;
    this.perfectMatches = perfectMatches;
    this.currentMatchStreak = currentMatchStreak;
    this.longestMatchStreak = longestMatchStreak;
    this.currentGameStreak = currentGameStreak;
    this.longestGameStreak = longestGameStreak;
    this.isAiPlayer = isAiPlayer;
    this.isGuest = isGuest;
  }

  // Getters and Setters
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

  public UserStatus getStatus() {
    return status;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
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

  public LocalDate getBirthday() {
    return birthday;
  }

  public void setBirthday(LocalDate birthday) {
    this.birthday = birthday;
  }

  public int getAvatar() {
    return avatar;
  }

  public void setAvatar(int avatar) {
    this.avatar = avatar;
  }

  public int getScoreTotal() {
    return scoreTotal;
  }

  public void setScoreTotal(int scoreTotal) {
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

  public int getLongestGameStreak() {
    return longestGameStreak;
  }

  public void setLongestGameStreak(int longestGameStreak) {
    this.longestGameStreak = longestGameStreak;
  }

  public int getCurrentMatchStreak() {
    return currentMatchStreak;
  }

  public void setCurrentMatchStreak(int currentMatchStreak) {
    this.currentMatchStreak = currentMatchStreak;
  }

  public int getLongestMatchStreak() {
    return longestMatchStreak;
  }

  public void setLongestMatchStreak(int longestMatchStreak) {
    this.longestMatchStreak = longestMatchStreak;
  }
}
