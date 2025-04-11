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
  private float scoreTotal = 0.0f;

  @Column(name = "games_played", nullable = false)
  private int gamesPlayed = 0;

  @Column(name = "avg_placement", nullable = false)
  private float avgPlacement = 0.0f;

  @Column(name = "moon_shots", nullable = false)
  private int moonShots = 0;

  @Column(name = "perfect_rounds", nullable = false)
  private int perfectRounds = 0;

  @Column(name = "perfect_matches", nullable = false)
  private int perfectMatches = 0;

  @Column(name = "current_streak", nullable = false)
  private int currentStreak = 0;

  @Column(name = "longest_streak", nullable = false)
  private int longestStreak = 0;

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

  public float getAvgPlacement() {
    return avgPlacement;
  }

  public void setAvgPlacement(float avgPlacement) {
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
