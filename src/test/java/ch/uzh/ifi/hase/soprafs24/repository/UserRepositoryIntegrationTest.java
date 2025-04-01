package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
public class UserRepositoryIntegrationTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private UserRepository userRepository;

  @Test
  public void findByName_success() {
    // given
    User user = new User();
    user.setUsername("testUsername");
    user.setPassword("testPassword");
    user.setStatus(UserStatus.OFFLINE);
    user.setToken("1");
    user.setAvatar(0);
    user.setIsGuest(false);
    user.setBirthday(java.time.LocalDate.of(2000, 1, 1));
    user.setUserSettings("{}");
    user.setRating(0);

    entityManager.persist(user);
    entityManager.flush();

    // when
    User found = userRepository.findUserByUsername(user.getUsername());

    // then
    assertNotNull(found.getId());
    assertEquals(found.getUsername(), user.getUsername());
    assertEquals(found.getToken(), user.getToken());
    assertEquals(found.getStatus(), user.getStatus());
  }
}
