package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

// only include public inforamtion
public class UserAuthDTO {

  private Long id;
  private String username;
  private String token;
  private UserStatus status;
  private int avatar;
  private boolean isGuest;

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

  public int getAvatar() {
    return avatar;
  }

  public void setAvatar(int avatar) {
    this.avatar = avatar;
  }

  public UserStatus getStatus() {
    return status;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

  public void setIsGuest(Boolean isGuest) {
    this.isGuest = isGuest;
  }

  public Boolean getIsGuest() {
    return isGuest;
  }
}
