package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.time.LocalDate;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

// only include public inforamtion
public class UserGetDTO {

  private Long id;
  private String username;
  private UserStatus status;
  private int avatar;
  private LocalDate birthday;
  private int rating;

  public UserGetDTO(Long id, String username, UserStatus status, int avatar, LocalDate birthday, int rating) {
    this.id = id;
    this.username = username;
    this.status = status;
    this.avatar = avatar;
    this.birthday = birthday;
    this.rating = rating;
  }

  public UserGetDTO() {
    // default constructor required by mapping libraries
  }

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
}
