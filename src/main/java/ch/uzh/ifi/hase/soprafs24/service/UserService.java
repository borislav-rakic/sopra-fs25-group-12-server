package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import org.springframework.security.crypto.bcrypt.BCrypt;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.io.IOException;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

  private final Logger log = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  public boolean isUserTableEmpty() {
    return userRepository.count() == 0;
  }

  public void populateUsersFromSQL() {
    try {
      var resource = new ClassPathResource("sql/insert_test_users.sql");
      String sql = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      jdbcTemplate.execute(sql);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read SQL file", e);
    }
  }

  private String extractToken(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing or malformed Authorization header");
    }
    return authHeader.substring(7).trim(); // remove "Bearer " prefix
  }

  public User authenticateUserAtLogin(String username, String password) {
    User user = userRepository.findUserByUsername(username);
    if (user == null || !BCrypt.checkpw(password, user.getPassword())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
    // set new token.
    user.setToken(UUID.randomUUID().toString());
    user.setStatus(UserStatus.ONLINE);
    userRepository.save(user);
    return user;
  }

  @Autowired
  public UserService(@Qualifier("userRepository") UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public List<User> getUsers() {
    return this.userRepository.findAll();
  }

  public void logoutUserByToken(String token) {
    User user = userRepository.findUserByToken(token);
    if (user == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
    }
    user.setToken(null); // Invalidate token
    user.setStatus(UserStatus.OFFLINE); // Set status to OFFLINE.
    userRepository.save(user);
  }

  public User createUser(User newUser) {
    newUser.setToken(UUID.randomUUID().toString());
    newUser.setStatus(UserStatus.ONLINE);
    newUser.setAvatar(0); // default avatar is 0! (may change later)
    newUser.setIsGuest(false); // default
    newUser.setBirthday(null); // default date is null!
    newUser.setUserSettings("{}"); // empty settings
    newUser.setRating(0); // default rating

    checkIfUserExists(newUser);
    // saves the given entity but data is only persisted in the database once
    // flush() is called
    newUser = userRepository.save(newUser);
    userRepository.flush();

    log.debug("Created Information for User: {}", newUser);
    return newUser;
  }

  /**
   * This is a helper method that will check the uniqueness criteria of the
   * username and the name
   * defined in the User entity. The method will do nothing if the input is unique
   * and throw an error otherwise.
   *
   * @param userToBeCreated
   * @throws org.springframework.web.server.ResponseStatusException
   * @see User
   */
  private void checkIfUserExists(User userToBeCreated) {
    User userByUsername = userRepository.findUserByUsername(userToBeCreated.getUsername());

    String baseErrorMessage = "The %s provided %s not unique. Therefore, the user could not be created!";
    if (userByUsername != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          String.format(baseErrorMessage, "username", "is"));
    }
  }

  public User getUserById(Long userId) {
    User user = userRepository.findUserById(userId);
    if (user == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
    }
    return user;
  }

  public User getUserByToken(String token) {
    User user = userRepository.findUserByToken(token);
    if (user == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
    }
    return user;
  }

  public void updateUser(Long userId, User updates) {
    User user = userRepository.findUserById(userId);
    if (user == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
    }

    // Only update fields that are non-null in the DTO
    if (updates.getPassword() != null && updates.getPassword().length() > 0) {
      user.setPassword(updates.getPassword());
    }
    if (updates.getUsername() != null) {
      user.setUsername(updates.getUsername());
    }
    if (updates.getBirthday() != null) {
      user.setBirthday(updates.getBirthday());
    }
    if (updates.getAvatar() != null) {
      user.setAvatar(updates.getAvatar());
    }
    if (updates.getUserSettings() != null) {
      user.setUserSettings(updates.getUserSettings());
    }

    userRepository.save(user);
  }

  public void logoutUser(Long id, String token) {
    User user = userRepository.findUserById(id);
    if (user == null || !user.getToken().equals(token)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials for logout");
    }

    user.setStatus(UserStatus.OFFLINE);
    user.setToken(null);
    userRepository.save(user);
  }

  public Page<User> findUsersForLeaderboard(String filter, Pageable pageable) {
    Specification<User> spec = (root, query, cb) -> {
      if (filter != null && !filter.isBlank()) {
        // Filter by username (case-insensitive)
        return cb.like(cb.lower(root.get("username")), "%" + filter.toLowerCase() + "%");
      }
      return cb.conjunction(); // No filtering
    };

    return userRepository.findAll(spec, pageable);
  }

}
