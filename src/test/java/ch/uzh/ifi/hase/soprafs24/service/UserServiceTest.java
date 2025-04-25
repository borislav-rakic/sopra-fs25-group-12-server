package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private MatchRepository matchRepository; // Add MatchRepository mock

  @InjectMocks
  private UserService userService;

  private User testUser;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);

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
}
