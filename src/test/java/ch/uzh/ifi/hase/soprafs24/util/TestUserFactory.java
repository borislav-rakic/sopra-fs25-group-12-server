package ch.uzh.ifi.hase.soprafs24.util;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;

import org.springframework.security.crypto.bcrypt.BCrypt;

public class TestUserFactory {

    public static User createValidUser(String username) {
        return createValidUser(username, false);
    }

    public static User createValidUser(String username, boolean encryptedPassword) {
        User user = new User();
        user.setUsername(username);
        String password = "testPassword";
        if (encryptedPassword) {
            password = BCrypt.hashpw(password, BCrypt.gensalt());
        }
        user.setPassword(password);
        user.setStatus(UserStatus.OFFLINE);
        user.setAvatar(0);
        user.setUserSettings("{}");
        user.setRating(0);
        user.setIsGuest(false);
        user.setIsAiPlayer(false);
        return user;
    }
}
