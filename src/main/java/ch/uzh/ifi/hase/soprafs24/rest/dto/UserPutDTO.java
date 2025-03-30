package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.time.LocalDate;

public class UserPutDTO {

  private String username;
  private Integer avatar;
  private LocalDate birthday;
  private String password;
  private String passwordConfirmed;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public LocalDate getBirthday() {
    return birthday;
  }

  public void setBirthday(LocalDate birthday) {
    this.birthday = birthday;
  }

  public Integer getAvatar() {
    return avatar;
  }

  public void setAvatar(int avatar) {
    this.avatar = avatar;
  }

  public String getPasswordConfirmed() {
    return passwordConfirmed;
  }

  public void setPasswordConfirmed(String passwordConfirmed) {
    this.passwordConfirmed = passwordConfirmed;
  }
}
