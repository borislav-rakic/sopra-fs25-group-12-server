package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.time.LocalDate;

import ch.uzh.ifi.hase.soprafs24.constant.PriorEngagement;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

// only include public inforamtion
public class UserPrivateDTO {

  private Long id;
  private String username;
  private UserStatus status;
  private int avatar;
  private LocalDate birthday;
  private String userSettings;
  private Boolean isGuest;
  private Long participantOfActiveMatchId;
  private PriorEngagement participantOfActiveMatchPhase = PriorEngagement.NULL;

  public void setId(Long id) {
    this.id = id;
  }

  public Long getId() {
    return id;
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

  public String getUserSettings() {
    return userSettings;
  }

  public void setUserSettings(String userSettings) {
    this.userSettings = userSettings;
  }

  public boolean getIsGuest() {
    return isGuest;
  }

  public void setIsGuest(boolean isGuest) {
    this.isGuest = isGuest;
  }

  public PriorEngagement getParticipantOfActiveMatchPhase() {
    return participantOfActiveMatchPhase;
  }

  public void setParticipantOfActiveMatchPhase(PriorEngagement participantOfActiveMatchPhase) {
    this.participantOfActiveMatchPhase = participantOfActiveMatchPhase;
  }

  public Long getParticipantOfActiveMatchId() {
    return participantOfActiveMatchId;
  }

  public void setParticipantOfActiveMatchId(Long participantOfActiveMatchId) {
    this.participantOfActiveMatchId = participantOfActiveMatchId;
  }

}
