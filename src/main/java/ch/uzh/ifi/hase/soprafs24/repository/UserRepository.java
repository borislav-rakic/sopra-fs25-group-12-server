package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.User;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository("userRepository")
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
  User findUserByUsername(String username);

  User findUserById(Long id);

  User findUserByToken(String token);

  List<User> findByUsernameContainingIgnoreCase(String username);
}
