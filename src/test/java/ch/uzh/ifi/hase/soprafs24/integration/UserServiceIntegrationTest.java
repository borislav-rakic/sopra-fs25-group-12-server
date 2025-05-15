package ch.uzh.ifi.hase.soprafs24.integration;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void createUser_validUser_userIsSavedWithDefaults() {
        User newUser = new User();
        newUser.setUsername("testuser");
        newUser.setPassword("securepass");

        User created = userService.createUser(newUser);

        assertNotNull(created.getId());
        assertNotNull(created.getToken());
        assertEquals(UserStatus.ONLINE, created.getStatus());
        assertFalse(created.getIsGuest());
        assertFalse(created.getIsAiPlayer());
        assertNotNull(created.getAvatar());
        assertEquals("testuser", created.getUsername());
        assertEquals("securepass", created.getPassword()); // No hashing in this method
    }

    @Test
    void createUser_missingUsername_throwsException() {
        User newUser = new User();
        newUser.setPassword("pw");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.createUser(newUser));
        assertTrue(ex.getMessage().contains("Username is required"));
    }

    @Test
    void createUser_missingPassword_throwsException() {
        User newUser = new User();
        newUser.setUsername("name");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.createUser(newUser));
        assertTrue(ex.getMessage().contains("Password is required"));

    }

    @Test
    void createUser_duplicateUsername_throwsException() {
        User user1 = new User();
        user1.setUsername("dupe");
        user1.setPassword("pw");
        userService.createUser(user1);

        User user2 = new User();
        user2.setUsername("dupe");
        user2.setPassword("pw2");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.createUser(user2));
        assertTrue(ex.getMessage().contains("username"));
    }
}
