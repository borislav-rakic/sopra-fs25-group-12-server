package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.constant.PriorEngagement;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.rest.dto.InviteGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPrivateDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;

import java.util.concurrent.ThreadLocalRandom;

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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.Predicate;
import java.util.Map;

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

  @Autowired
  private MatchRepository matchRepository;

  public boolean isUserTableEmpty() {
    return userRepository.count() == 0;
  }

  public long getUserCount() {
    return userRepository.count();
  }

  public User createGuestUser(User guest) {
    return userRepository.save(guest);
  }

  public void populateUsersFromSQL() {
    if (userRepository.count() > 10) {
      return;
    }
    try {
      var resource = new ClassPathResource("sql/insert_test_users.sql");
      String sql = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      jdbcTemplate.execute(sql);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read SQL file", e);
    }
  }

  public List<User> searchUsersByUsername(String username) {
    return userRepository.findByUsernameContainingIgnoreCase(username);
  }

  public Long getUserIdFromToken(String token) {
    User user = userRepository.findUserByToken(token);
    if (user == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
    }
    return user.getId();
  }

  public User requireUserByToken(String token) {
    User user = userRepository.findUserByToken(token);
    if (user == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
    }
    return user;
  }

  public User authenticateUserAtLogin(String username, String password) {
    User user = userRepository.findUserByUsername(username);
    if (user == null || !BCrypt.checkpw(password, user.getPassword())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
    // Set new token. Makes developing more difficult, should not be enforced, yet.
    // /dsj
    // user.setToken(UUID.randomUUID().toString());
    user.setStatus(UserStatus.ONLINE);
    user.setIsGuest(false);
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
    if (newUser.getUsername() == null || newUser.getUsername().trim().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
    }

    if (newUser.getPassword() == null || newUser.getPassword().trim().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
    }

    newUser.setToken(UUID.randomUUID().toString());
    newUser.setStatus(UserStatus.ONLINE);

    newUser.setAvatar(randomAvatarGeneratorForUsers()); // default avatar is 0! (may change later)
    newUser.setIsGuest(false); // default
    newUser.setIsAiPlayer(false); // default
    newUser.setBirthday(null); // default date is null!
    newUser.setUserSettings("{}"); // empty settings

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
      // Base predicate: only real, non-guest users
      Predicate isNotAi = cb.equal(root.get("isAiPlayer"), false);
      Predicate isNotGuest = cb.equal(root.get("isGuest"), false);
      Predicate basePredicate = cb.and(isNotAi, isNotGuest);

      // Optional username filter
      if (filter != null && !filter.isBlank()) {
        Predicate usernamePredicate = cb.like(cb.lower(root.get("username")), "%" + filter.toLowerCase() + "%");
        return cb.and(basePredicate, usernamePredicate);
      }

      return basePredicate;
    };

    return userRepository.findAll(spec, pageable);
  }

  public List<InviteGetDTO> getPendingInvites(String authHeader) {
    String token = authHeader.replace("Bearer ", "");
    User user = userRepository.findUserByToken(token);

    List<Match> matches = matchRepository.findAll();

    List<InviteGetDTO> invites = new ArrayList<>();

    for (Match match : matches) {
      Map<Integer, Long> matchInvites = match.getInvites();
      if (matchInvites != null && matchInvites.containsValue(user.getId())) {
        Integer matchPlayerSlot = null;
        for (Map.Entry<Integer, Long> entry : matchInvites.entrySet()) {
          if (entry.getValue().equals(user.getId())) {
            matchPlayerSlot = entry.getKey();
            break;
          }
        }

        if (matchPlayerSlot != null) {
          InviteGetDTO dto = new InviteGetDTO();
          dto.setMatchId(match.getMatchId());
          dto.setMatchPlayerSlot(matchPlayerSlot);
          dto.setHostId(match.getHostId());
          dto.setUserId(user.getId());
          User hostUser = userRepository.findById(match.getHostId())
              .orElseThrow(() -> new EntityNotFoundException("Host not found"));
          dto.setFromUsername(hostUser.getUsername());
          invites.add(dto);
        }
      }
    }

    return invites;
  }

  public UserPrivateDTO getUserInformation(Long userId) {
    // Fetch the user by ID
    User user = userRepository.findUserById(userId);

    if (user == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
    }

    // Fetch the match the user is participating in
    Match activeMatch = matchRepository.findByMatchPlayersUserId(userId);

    // Default to 0 if no active match is found, or match is finished/aborted
    Long participantOfActiveMatchId = 0L;

    // Check if the active match exists and it's not in FINISHED or ABORTED phase
    if (activeMatch != null && isActiveMatch(activeMatch)) {
      participantOfActiveMatchId = activeMatch.getMatchId(); // Set the match ID if it's active
    } else {
      participantOfActiveMatchId = null; // Return null if no active match
    }

    // Convert User to UserPrivateDTO and set participantOfActiveMatchId
    UserPrivateDTO userDTO = DTOMapper.INSTANCE.convertEntityToUserPrivateDTO(user);
    userDTO.setParticipantOfActiveMatchId(participantOfActiveMatchId);

    // "wäre es möglich beim userPrivateDTO noch mitzugeben, ob der player in einem
    // aktiven match oder in einem matchcreation (also start/{matched}) ist?"

    List<Long> matchIdList1 = matchRepository.findActiveMatchIdsWithUserId(userId);
    List<Long> matchIdList2 = matchRepository.findMatchIdsInSetupWithUserId(userId);
    if (matchIdList1.size() > 0) {
      userDTO.setParticipantOfActiveMatchPhase(PriorEngagement.MATCH);
    } else if (matchIdList2.size() > 0) {
      userDTO.setParticipantOfActiveMatchPhase(PriorEngagement.START);
    } else {
      userDTO.setParticipantOfActiveMatchPhase(PriorEngagement.NULL);
    }

    return userDTO;
  }

  private boolean isActiveMatch(Match match) {
    // Check if the match phase is neither FINISHED nor ABORTED
    return match.getPhase() != MatchPhase.FINISHED && match.getPhase() != MatchPhase.ABORTED;
  }

  public static int randomAvatarGenerator(int offset) {
    int randomNum = offset + ThreadLocalRandom.current().nextInt(1, 51); // upper bound exclusive
    return randomNum;
  }

  public static int randomAvatarGeneratorForUsers() {
    return randomAvatarGenerator(100);
  }

  public static int randomAvatarGeneratorForGuests() {
    return randomAvatarGenerator(200);
  }

  public static int randomAvatarGeneratorForAis() {
    return randomAvatarGenerator(300);
  }

}
