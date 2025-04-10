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
  private int rating;
  private boolean isGuest;
  private boolean isAiPlayer;
  private int scoreTotal;
  private int gamesPlayed;
  private int avgPlacement;
  private int moonShots;
  private int perfectRounds;
  private int perfectMatches;
  private int currentStreak;
  private int longestStreak;

  public UserGetDTO() {
    // default constructor required by mapping libraries
  }

  public UserGetDTO(Long id, String username, UserStatus status, int avatar, LocalDate birthday, int rating,
      int scoreTotal, int gamesPlayed, int avgPlacement, int moonShots,
      int perfectRounds, int perfectMatches, int currentStreak, int longestStreak,
      boolean isAiPlayer, boolean isGuest) {
    this.id = id;
    this.username = username;
    this.status = status;
    this.avatar = avatar;
    this.birthday = birthday;
    this.rating = rating;
    this.scoreTotal = scoreTotal;
    this.gamesPlayed = gamesPlayed;
    this.avgPlacement = avgPlacement;
    this.moonShots = moonShots;
    this.perfectRounds = perfectRounds;
    this.perfectMatches = perfectMatches;
    this.currentStreak = currentStreak;
    this.longestStreak = longestStreak;
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

  public int getRating() {
    return rating;
  }

  public void setRating(int rating) {
    this.rating = rating;
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

  public int getAvgPlacement() {
    return avgPlacement;
  }

  public void setAvgPlacement(int avgPlacement) {
    this.avgPlacement = avgPlacement;
  }

  public int getMoonShots() {
    return moonShots;
  }

  public void setMoonShots(int moonShots) {
    this.moonShots = moonShots;
  }

  public int getPerfectRounds() {
    return perfectRounds;
  }

  public void setPerfectRounds(int perfectRounds) {
    this.perfectRounds = perfectRounds;
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
