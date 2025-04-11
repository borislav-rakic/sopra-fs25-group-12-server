package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the UserService integration test.
 */
@WebAppConfiguration
@SpringBootTest
public class UserServiceIntegrationTest {

  @Qualifier("userRepository")
  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserService userService;

  @BeforeEach
  public void setup() {
    // Clean the repository before each test
    userRepository.deleteAll();
  }

  @Test
  public void createUser_validInputs_success() {
    // given
    assertNull(userRepository.findUserByUsername("testUsername"));

    User testUser = new User();
    testUser.setUsername("testUsername");
    testUser.setPassword("testPassword");

    // when
    User createdUser = userService.createUser(testUser);

    // then
    assertNotNull(createdUser);
    assertEquals(testUser.getUsername(), createdUser.getUsername());
    assertNotNull(createdUser.getToken());
    assertEquals(UserStatus.ONLINE, createdUser.getStatus());

    // Verify the user is saved to the database
    assertNotNull(userRepository.findUserByUsername("testUsername"));
  }

  @Test
  public void createUser_duplicateUsername_throwsException() {
    // given
    assertNull(userRepository.findUserByUsername("testUsername"));

    User testUser = new User();
    testUser.setUsername("testUsername");
    testUser.setPassword("testPassword");
    testUser.setIsGuest(false);
    testUser.setIsAiPlayer(false);
    testUser.setStatus(UserStatus.OFFLINE);
    testUser.setUserSettings("{}");

    // Create the first user
    userService.createUser(testUser);

    // Check that the user was created
    assertNotNull(userRepository.findUserByUsername("testUsername"));

    // Try to create another user with the same username
    User testUser2 = new User();
    testUser2.setUsername("testUsername"); // same username as testUser
    testUser2.setPassword("newPassword");
    testUser2.setIsGuest(false);
    testUser2.setIsAiPlayer(false);
    testUser2.setStatus(UserStatus.OFFLINE);
    testUser2.setUserSettings("{}");

    // when / then
    assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser2));
  }

}
