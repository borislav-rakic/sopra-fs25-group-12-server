package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserAuthDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserCreateDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPrivateDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPutDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserLoginDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */
@RestController
public class UserController {

  private final UserService userService;

  UserController(UserService userService) {
    this.userService = userService;
  }

  public String hashPassword(String plainPassword) {
    return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
  }

  private String extractToken(String authHeader) {
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7); // remove "Bearer "
    }
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
  }

  @GetMapping("/users")
  @ResponseStatus(HttpStatus.OK)
  public List<UserGetDTO> getAllUsers() {
    // fetch all users in the internal representation
    List<User> users = userService.getUsers();
    List<UserGetDTO> userGetDTOs = new ArrayList<>();

    // convert each user to the API representation
    for (User user : users) {
      userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
    }
    return userGetDTOs;
  }

  @GetMapping("/users/search")
  public List<UserGetDTO> searchUsers(@RequestParam String username) {
    List<User> users = userService.searchUsersByUsername(username);
    return users.stream()
        .map(DTOMapper.INSTANCE::convertEntityToUserGetDTO)
        .collect(Collectors.toList());
  }

  @PostMapping("/users")
  @ResponseStatus(HttpStatus.CREATED)
  public UserAuthDTO createUser(@RequestBody UserCreateDTO userCreateDTO) {
    // convert API user to internal representation
    User userInput = DTOMapper.INSTANCE.convertUserCreateDTOtoEntity(userCreateDTO);

    // hash password
    String hashedPassword = hashPassword(userInput.getPassword());
    userInput.setPassword(hashedPassword);

    // generate token
    userInput.setToken(UUID.randomUUID().toString());

    // create user
    User createdUser = userService.createUser(userInput);

    // convert internal representation of user back to auth DTO
    return DTOMapper.INSTANCE.convertEntityToUserAuthDTO(createdUser);
  }

  @GetMapping("/users/{userId}")
  @ResponseStatus(HttpStatus.OK)
  public UserGetDTO getUserById(@PathVariable Long userId) {
    User user = userService.getUserById(userId);
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
  }

  @GetMapping("/users/me")
  @ResponseStatus(HttpStatus.OK)
  public UserPrivateDTO getOwnUser(@RequestHeader("Authorization") String authHeader) {
    String token = extractToken(authHeader);
    User user = userService.getUserByToken(token);
    return DTOMapper.INSTANCE.convertEntityToUserPrivateDTO(user);
  }

  @PostMapping("/login")
  @ResponseStatus(HttpStatus.OK)
  public UserAuthDTO loginUser(@RequestBody UserLoginDTO loginDTO) {
    User user = userService.authenticateUserAtLogin(loginDTO.getUsername(), loginDTO.getPassword());
    return DTOMapper.INSTANCE.convertEntityToUserAuthDTO(user);
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT) // 204
  public void logoutUser(@RequestHeader("Authorization") String authHeader) {
    String token = extractToken(authHeader);
    userService.logoutUserByToken(token);
  }

  @PutMapping("/users/me")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateCurrentUser(@RequestHeader("Authorization") String authHeader,
      @RequestBody UserPutDTO userPutDTO) {
    String token = extractToken(authHeader);

    User currentUser = userService.getUserByToken(token);
    if (currentUser == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing token");
    }

    if (userPutDTO.getPassword() != null &&
        !userPutDTO.getPassword().isEmpty() &&
        userPutDTO.getPasswordConfirmed() != null &&
        !userPutDTO.getPasswordConfirmed().isEmpty()) {

      if (!userPutDTO.getPassword().equals(userPutDTO.getPasswordConfirmed())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
      }
    }

    User userUpdates = DTOMapper.INSTANCE.convertUserPutDTOtoEntity(userPutDTO);

    if (userPutDTO.getPassword() != null && !userPutDTO.getPassword().isEmpty()) {
      String hashedPassword = hashPassword(userPutDTO.getPassword());
      userUpdates.setPassword(hashedPassword);
    }

    userService.updateUser(currentUser.getId(), userUpdates);
  }

}