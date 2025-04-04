package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.Friendship;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.constant.FriendshipStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("friendshipRepository")
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

  // Use actual field names: user and friend
  Optional<Friendship> findByUserAndFriend(User user, User friend);

  List<Friendship> findAllByUserOrFriend(User user, User friend);

  List<Friendship> findAllByFriendAndStatus(User friend, FriendshipStatus status);

}
