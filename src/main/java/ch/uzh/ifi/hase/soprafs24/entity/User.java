package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * Internal User Representation
 * This class composes the internal representation of the user and defines how
 * the user is stored in the database.
 * Every variable will be mapped into a database field with the @Column
 * annotation
 * - nullable = false -> this cannot be left empty
 * - unique = true -> this value must be unqiue across the database -> composes
 * the primary key
 */
@Entity
@Table(name = "USER")
public class User implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = true, unique = true)
  private String token;

  @Column(nullable = false)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserStatus status = UserStatus.OFFLINE;

  @Column(nullable = true)
  private Integer avatar;

  @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
  private boolean isAiPlayer = false;

  @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
  private boolean isGuest = false;

  @Column(nullable = true)
  private LocalDate birthday;

  @Column(name = "user_settings", nullable = false)
  private String userSettings = "{}";

  // NEW STATS FIELDS
  @Column(name = "score_total", nullable = false)
  private int scoreTotal = 0;

  @Column(name = "games_played", nullable = false)
  private int gamesPlayed = 0;

  @Column(name = "matches_played", nullable = false)
  private int matchesPlayed = 0;

  @Column(name = "avg_game_ranking", nullable = false)
  private float avgGameRanking = 0.0f;

  @Column(name = "avg_match_ranking", nullable = false)
  private float avgMatchRanking = 0.0f;

  @Column(name = "moon_shots", nullable = false)
  private int moonShots = 0;

  @Column(name = "perfect_games", nullable = false)
  private int perfectGames = 0;

  @Column(name = "perfect_matches", nullable = false)
  private int perfectMatches = 0;

  @Column(name = "current_game_streak", nullable = false)
  private int currentGameStreak = 0;

  @Column(name = "longest_game_streak", nullable = false)
  private int longestGameStreak = 0;

  @Column(name = "current_match_streak", nullable = false)
  private int currentMatchStreak = 0;

  @Column(name = "longest_match_streak", nullable = false)
  private int longestMatchStreak = 0;

  // GETTERS AND SETTERS

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

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public UserStatus getStatus() {
    return status;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

  public Integer getAvatar() {
    return avatar;
  }

  public void setAvatar(Integer avatar) {
    this.avatar = avatar;
  }

  public Boolean getIsAiPlayer() {
    return isAiPlayer;
  }

  public void setIsAiPlayer(Boolean isAiPlayer) {
    this.isAiPlayer = isAiPlayer;
  }

  public Boolean getIsGuest() {
    return isGuest;
  }

  public void setIsGuest(Boolean isGuest) {
    this.isGuest = isGuest;
  }

  public LocalDate getBirthday() {
    return birthday;
  }

  public void setBirthday(LocalDate birthday) {
    this.birthday = birthday;
  }

  public String getUserSettings() {
    return userSettings;
  }

  public void setUserSettings(String userSettings) {
    this.userSettings = userSettings;
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
