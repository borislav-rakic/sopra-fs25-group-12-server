package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testSettersAndGetters() {
        User user = new User();

        user.setId(1L);
        user.setUsername("testuser");
        user.setToken("abc123");
        user.setPassword("secure");
        user.setStatus(UserStatus.ONLINE);
        user.setAvatar(5);
        user.setIsAiPlayer(true);
        user.setIsGuest(true);
        user.setBirthday(LocalDate.of(2000, 1, 1));
        user.setUserSettings("{\"theme\":\"dark\"}");

        user.setScoreTotal(100);
        user.setGamesPlayed(10);
        user.setMatchesPlayed(4);
        user.setAvgGameRanking(2.5f);
        user.setAvgMatchRanking(3.1f);
        user.setMoonShots(2);
        user.setPerfectGames(1);
        user.setPerfectMatches(1);
        user.setCurrentGameStreak(3);
        user.setLongestGameStreak(5);
        user.setCurrentMatchStreak(2);
        user.setLongestMatchStreak(4);

        assertEquals(1L, user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals("abc123", user.getToken());
        assertEquals("secure", user.getPassword());
        assertEquals(UserStatus.ONLINE, user.getStatus());
        assertEquals(5, user.getAvatar());
        assertTrue(user.getIsAiPlayer());
        assertTrue(user.getIsGuest());
        assertEquals(LocalDate.of(2000, 1, 1), user.getBirthday());
        assertEquals("{\"theme\":\"dark\"}", user.getUserSettings());

        assertEquals(100, user.getScoreTotal());
        assertEquals(10, user.getGamesPlayed());
        assertEquals(4, user.getMatchesPlayed());
        assertEquals(2.5f, user.getAvgGameRanking());
        assertEquals(3.1f, user.getAvgMatchRanking());
        assertEquals(2, user.getMoonShots());
        assertEquals(1, user.getPerfectGames());
        assertEquals(1, user.getPerfectMatches());
        assertEquals(3, user.getCurrentGameStreak());
        assertEquals(5, user.getLongestGameStreak());
        assertEquals(2, user.getCurrentMatchStreak());
        assertEquals(4, user.getLongestMatchStreak());
    }

    @Test
    void testDefaultValues() {
        User user = new User();
        assertEquals(UserStatus.OFFLINE, user.getStatus());
        assertEquals("{}", user.getUserSettings());
        assertFalse(user.getIsAiPlayer());
        assertFalse(user.getIsGuest());
        assertEquals(0, user.getScoreTotal());
        assertEquals(0, user.getGamesPlayed());
        assertEquals(0, user.getMatchesPlayed());
        assertEquals(0.0f, user.getAvgGameRanking());
        assertEquals(0.0f, user.getAvgMatchRanking());
        assertEquals(0, user.getMoonShots());
        assertEquals(0, user.getPerfectGames());
        assertEquals(0, user.getPerfectMatches());
        assertEquals(0, user.getCurrentGameStreak());
        assertEquals(0, user.getLongestGameStreak());
        assertEquals(0, user.getCurrentMatchStreak());
        assertEquals(0, user.getLongestMatchStreak());
    }
}
