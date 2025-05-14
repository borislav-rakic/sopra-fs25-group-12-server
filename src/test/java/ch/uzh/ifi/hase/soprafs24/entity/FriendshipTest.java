package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.FriendshipStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FriendshipTest {

    @Test
    void testNoArgsConstructorAndSetters() {
        Friendship friendship = new Friendship();

        User user = new User();
        user.setId(1L);
        User friend = new User();
        friend.setId(2L);

        friendship.setId(42L);
        friendship.setUser(user);
        friendship.setFriend(friend);
        friendship.setStatus(FriendshipStatus.PENDING);

        assertEquals(42L, friendship.getId());
        assertEquals(user, friendship.getUser());
        assertEquals(friend, friendship.getFriend());
        assertEquals(FriendshipStatus.PENDING, friendship.getStatus());
    }

    @Test
    void testAllArgsConstructor() {
        User user = new User();
        User friend = new User();

        Friendship friendship = new Friendship(user, friend, FriendshipStatus.ACCEPTED);

        assertEquals(user, friendship.getUser());
        assertEquals(friend, friendship.getFriend());
        assertEquals(FriendshipStatus.ACCEPTED, friendship.getStatus());
    }
}
