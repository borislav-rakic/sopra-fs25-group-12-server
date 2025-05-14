package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserAuthDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserCreateDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPrivateDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPutDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserLoginDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.InviteGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

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

  /**
   * Retrieves the list of all users.
   */
  @GetMapping("/users")
  @ResponseStatus(HttpStatus.OK)
  public List<UserGetDTO> getAllUsers() {
    List<User> users = userService.getUsers();
    List<UserGetDTO> userGetDTOs = new ArrayList<>();

    for (User user : users) {
      userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
    }
    return userGetDTOs;
  }

  /**
   * Searches users by username.
   */
  @GetMapping("/users/search")
  public List<UserGetDTO> searchUsers(@RequestParam String username) {
    List<User> users = userService.searchUsersByUsername(username);
    return users.stream()
        .map(DTOMapper.INSTANCE::convertEntityToUserGetDTO)
        .collect(Collectors.toList());
  }

  /**
   * Registers a new user.
   */
  @PostMapping("/users")
  @ResponseStatus(HttpStatus.CREATED)
  public UserAuthDTO createUser(@RequestBody UserCreateDTO userCreateDTO) {
    User userInput = DTOMapper.INSTANCE.convertUserCreateDTOtoEntity(userCreateDTO);

    String hashedPassword = hashPassword(userInput.getPassword());
    userInput.setPassword(hashedPassword);

    userInput.setToken(UUID.randomUUID().toString());

    User createdUser = userService.createUser(userInput);

    return DTOMapper.INSTANCE.convertEntityToUserAuthDTO(createdUser);
  }

  /**
   * Creates and logs in a guest user.
   */
  @PostMapping("/users/guest")
  @ResponseStatus(HttpStatus.CREATED)
  public UserAuthDTO createGuestUser() {
    User guest = new User();
    guest.setUsername("guest-" + UUID.randomUUID().toString().substring(0, 10));
    guest.setIsGuest(true);
    guest.setToken(UUID.randomUUID().toString());
    guest.setStatus(UserStatus.ONLINE);
    guest.setAvatar(UserService.randomAvatarGeneratorForGuests());
    String hashedRandomPassword = hashPassword(UUID.randomUUID().toString());
    guest.setPassword(hashedRandomPassword);

    User createdGuest = userService.createGuestUser(guest);
    return DTOMapper.INSTANCE.convertEntityToUserAuthDTO(createdGuest);
  }

  /**
   * Gets public user data for a specific user by ID.
   */
  @GetMapping("/users/{userId}")
  @ResponseStatus(HttpStatus.OK)
  public UserGetDTO getUserById(@PathVariable Long userId) {
    User user = userService.getUserById(userId);
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
  }

  /**
   * Gets private data of the authenticated user.
   */
  @GetMapping("/users/me")
  public UserPrivateDTO getUserInformation(@RequestHeader("Authorization") String authHeader) {
    String token = extractToken(authHeader);
    Long userId = userService.getUserIdFromToken(token);

    return userService.getUserInformation(userId);
  }

  /**
   * Logs in a user using username and password.
   */
  @PostMapping("/login")
  @ResponseStatus(HttpStatus.OK)
  public UserAuthDTO loginUser(@RequestBody UserLoginDTO loginDTO) {
    User user = userService.authenticateUserAtLogin(loginDTO.getUsername(), loginDTO.getPassword());
    return DTOMapper.INSTANCE.convertEntityToUserAuthDTO(user);
  }

  /**
   * Logs out the currently authenticated user.
   */
  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT) // 204
  public void logoutUser(@RequestHeader("Authorization") String authHeader) {
    String token = extractToken(authHeader);
    userService.logoutUserByToken(token);
  }

  /**
   * Updates the currently authenticated user’s profile data.
   */
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

  /**
   * Returns a list of all pending invites for the authenticated user.
   */
  @GetMapping("/users/me/invites")
  @ResponseStatus(HttpStatus.OK)
  public List<InviteGetDTO> getPendingInvites(@RequestHeader("Authorization") String authHeader) {
    return userService.getPendingInvites(authHeader);
  }

}