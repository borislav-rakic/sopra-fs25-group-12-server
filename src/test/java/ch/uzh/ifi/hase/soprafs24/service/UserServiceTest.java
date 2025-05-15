package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.constant.PriorEngagement;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.InviteGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPrivateDTO;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private MatchRepository matchRepository;

  @Mock
  private JdbcTemplate jdbcTemplate;

  private UserService userService;

  private User testUser;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);

    userService = new UserService(userRepository);

    // Manually inject the matchRepository mock using reflection
    try {
      java.lang.reflect.Field field = UserService.class.getDeclaredField("matchRepository");
      field.setAccessible(true);
      field.set(userService, matchRepository);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    testUser = new User();
    testUser.setId(1L);
    testUser.setUsername("testUsername");
    testUser.setPassword(BCrypt.hashpw("testPassword", BCrypt.gensalt()));
    testUser.setToken("testToken");
    testUser.setStatus(UserStatus.ONLINE);
  }

  @Test
  void authenticateUserAtLogin_validCredentials_success() {
    Mockito.when(userRepository.findUserByUsername("testUsername")).thenReturn(testUser);
    Mockito.when(userRepository.save(any())).thenReturn(testUser);

    User result = userService.authenticateUserAtLogin("testUsername", "testPassword");

    assertNotNull(result);
    assertEquals(UserStatus.ONLINE, result.getStatus());
    assertNotNull(result.getToken());
  }

  @Test
  void createUser_validInput_success() {
    User newUser = new User();
    newUser.setUsername("newUser");
    newUser.setPassword("newPassword");

    Mockito.when(userRepository.findUserByUsername("newUser")).thenReturn(null);
    Mockito.when(userRepository.save(any())).thenReturn(newUser);

    // Mock MatchRepository behavior - assume no active match for the new user
    Mockito.when(matchRepository.findByMatchPlayersUserId(anyLong()))
        .thenReturn(null); // Mocking no active match (return null instead of long)

    User createdUser = userService.createUser(newUser);

    assertNotNull(createdUser.getToken());
    assertEquals(UserStatus.ONLINE, createdUser.getStatus());
    Mockito.verify(userRepository, Mockito.times(1)).flush();
  }

  @Test
  void createUser_duplicateUsername_throwsException() {
    Mockito.when(userRepository.findUserByUsername("testUsername")).thenReturn(testUser);

    User duplicateUser = new User();
    duplicateUser.setUsername("testUsername");
    duplicateUser.setPassword("password123");

    // Mock MatchRepository behavior - no active match for the duplicate user
    Mockito.when(matchRepository.findByMatchPlayersUserId(anyLong()))
        .thenReturn(null); // Mocking no active match (return null instead of long)

    assertThrows(ResponseStatusException.class, () -> userService.createUser(duplicateUser));
  }

  @Test
  void updateUser_validUpdates_success() {
    User updates = new User();
    updates.setUsername("updatedUsername");
    updates.setPassword("updatedPassword");
    updates.setAvatar(2);

    Mockito.when(userRepository.findUserById(1L)).thenReturn(testUser);
    Mockito.when(userRepository.save(any())).thenReturn(testUser);

    userService.updateUser(1L, updates);

    assertEquals("updatedUsername", testUser.getUsername());
    assertEquals("updatedPassword", testUser.getPassword());
    assertEquals(2, testUser.getAvatar());
  }

  @Test
  void searchUsersByUsername_partialMatch_success() {
    Mockito.when(userRepository.findByUsernameContainingIgnoreCase("test"))
        .thenReturn(List.of(testUser));

    List<User> results = userService.searchUsersByUsername("test");

    assertFalse(results.isEmpty());
    assertEquals(1, results.size());
    assertEquals("testUsername", results.get(0).getUsername());
  }

  @Test
  void getUserIdFromToken_validToken_success() {
    Mockito.when(userRepository.findUserByToken("testToken")).thenReturn(testUser);

    Long userId = userService.getUserIdFromToken("testToken");

    assertEquals(1L, userId);
  }

  @Test
  void getUserIdFromToken_invalidToken_throwsException() {
    Mockito.when(matchRepository.findByMatchPlayersUserId(anyLong()))
        .thenReturn(null); // Mocking no active match (return null instead of long)

    assertThrows(ResponseStatusException.class, () -> userService.getUserIdFromToken("invalidToken"));
  }

  @Test
  void isUserTableEmpty_true() {
    Mockito.when(userRepository.count()).thenReturn(0L);

    assertTrue(userService.isUserTableEmpty());
  }

  @Test
  void isUserTableEmpty_false() {
    Mockito.when(userRepository.count()).thenReturn(1L);

    assertFalse(userService.isUserTableEmpty());
  }

  @Test
  void authenticateUserAtLogin_invalidPassword_throwsException() {
    Mockito.when(userRepository.findUserByUsername("testUsername")).thenReturn(testUser);

    assertThrows(ResponseStatusException.class,
        () -> userService.authenticateUserAtLogin("testUsername", "wrongPassword"));
  }

  @Test
  void updateUser_nonExistingUser_throwsException() {
    Mockito.when(userRepository.findUserById(1L)).thenReturn(null);

    User updates = new User();
    updates.setUsername("any");

    assertThrows(ResponseStatusException.class, () -> userService.updateUser(1L, updates));
  }

  @Test
  void logoutUser_setsStatusOffline() {
    Mockito.when(userRepository.findUserByToken("testToken")).thenReturn(testUser);
    userService.logoutUserByToken("testToken");
    assertEquals(UserStatus.OFFLINE, testUser.getStatus());
  }

  @Test
  void logoutUser_invalidToken_throwsException() {
    Mockito.when(userRepository.findUserByToken("invalidToken")).thenReturn(null);
    assertThrows(ResponseStatusException.class, () -> userService.logoutUserByToken("invalidToken"));
  }

  @Test
  void getUserByToken_validToken_returnsUser() {
    Mockito.when(userRepository.findUserByToken("testToken")).thenReturn(testUser);

    User result = userService.getUserByToken("testToken");
    assertEquals(testUser, result);
  }

  @Test
  void getUserByToken_invalidToken_returnsNull() {
    Mockito.when(userRepository.findUserByToken("invalidToken")).thenReturn(null);

    ResponseStatusException thrown = assertThrows(ResponseStatusException.class, () -> {
      userService.getUserByToken("invalidToken");
    });

    assertEquals(HttpStatus.UNAUTHORIZED, thrown.getStatus());
    assertEquals("Invalid token", thrown.getReason());
  }

  @Test
  void getUserById_validId_returnsUser() {
    Mockito.when(userRepository.findUserById(1L)).thenReturn(testUser);

    User result = userService.getUserById(1L);
    assertEquals(testUser, result);
  }

  @Test
  void getUserById_invalidId_returnsNull() {
    Mockito.when(userRepository.findUserById(999L)).thenReturn(null);

    ResponseStatusException thrown = assertThrows(ResponseStatusException.class, () -> {
      userService.getUserById(999L);
    });

    assertEquals(HttpStatus.NOT_FOUND, thrown.getStatus());
    assertEquals("User not found", thrown.getReason());
  }

  @Test
  void requireUserByToken_validToken_returnsUser() {
    Mockito.when(userRepository.findUserByToken("testToken")).thenReturn(testUser);

    User result = userService.requireUserByToken("testToken");
    assertEquals(testUser, result);
  }

  @Test
  void requireUserByToken_invalidToken_throwsException() {
    Mockito.when(userRepository.findUserByToken("invalid")).thenReturn(null);

    assertThrows(ResponseStatusException.class, () -> userService.requireUserByToken("invalid"));
  }

  @Test
  void logout_validUser_success() {
    Mockito.when(userRepository.findUserById(1L)).thenReturn(testUser);
    userService.logoutUser(1L, "testToken");
    assertEquals(UserStatus.OFFLINE, testUser.getStatus());
  }

  @Test
  void logout_invalidToken_throwsException() {
    testUser.setToken("someOtherToken");
    Mockito.when(userRepository.findUserById(1L)).thenReturn(testUser);

    assertThrows(ResponseStatusException.class, () -> userService.logoutUser(1L, "invalidToken"));
  }

  @Test
  void getUserInformation_validUserId_returnsUserPrivateDTO() {
    Match mockMatch = new Match();
    mockMatch.setMatchId(10L);
    mockMatch.setPhase(MatchPhase.SETUP);

    Mockito.when(userRepository.findUserById(1L)).thenReturn(testUser);
    Mockito.when(matchRepository.findByMatchPlayersUserId(1L)).thenReturn(mockMatch);
    Mockito.when(matchRepository.findActiveMatchIdsWithUserId(1L)).thenReturn(List.of());
    Mockito.when(matchRepository.findMatchIdsInSetupWithUserId(1L)).thenReturn(List.of(10L));

    UserPrivateDTO result = userService.getUserInformation(1L);

    assertNotNull(result);
    assertEquals(testUser.getId(), result.getId());
    assertEquals(PriorEngagement.START, result.getParticipantOfActiveMatchPhase());
    assertEquals(10L, result.getParticipantOfActiveMatchId());
  }

  @Test
  void getUserInformation_invalidUserId_throwsException() {
    Mockito.when(userRepository.findUserById(999L)).thenReturn(null);

    assertThrows(ResponseStatusException.class, () -> userService.getUserInformation(999L));
  }

  @Test
  void getPendingInvites_userHasInvites_returnsList() {
    Match match = new Match();
    match.setMatchId(1L);
    match.setHostId(10L);

    Map<Integer, Long> invites = Map.of(0, 1L); // Assume user ID is 1L
    match.setInvites(invites);

    User hostUser = new User();
    hostUser.setId(10L);
    hostUser.setUsername("hostUser");

    Mockito.when(userRepository.findUserByToken("testToken")).thenReturn(testUser);
    Mockito.when(matchRepository.findAll()).thenReturn(List.of(match));
    Mockito.when(userRepository.findById(10L)).thenReturn(Optional.of(hostUser));

    List<InviteGetDTO> result = userService.getPendingInvites("Bearer testToken");

    assertEquals(1, result.size());
    assertEquals(1L, result.get(0).getMatchId());
    assertEquals("hostUser", result.get(0).getFromUsername());
  }

}
