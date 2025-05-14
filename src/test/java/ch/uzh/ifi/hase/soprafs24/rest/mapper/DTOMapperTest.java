package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DTOMapperTest {

  @Test
  void testCreateUser_fromUserCreateDTO_toUser_success() {
    UserCreateDTO dto = new UserCreateDTO();
    dto.setUsername("testuser");
    dto.setPassword("secret");

    User user = DTOMapper.INSTANCE.convertUserCreateDTOtoEntity(dto);

    assertEquals("testuser", user.getUsername());
    assertEquals("secret", user.getPassword());
  }

  @Test
  void testGetUser_fromUser_toUserGetDTO_success() {
    User user = new User();
    user.setId(1L);
    user.setUsername("user1");
    user.setStatus(UserStatus.ONLINE);
    user.setAvatar(123);
    user.setBirthday(LocalDate.of(1990, 1, 1));
    user.setGamesPlayed(10);
    user.setScoreTotal(100);
    user.setMoonShots(1);

    UserGetDTO dto = DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);

    assertEquals(user.getId(), dto.getId());
    assertEquals(user.getUsername(), dto.getUsername());
    assertEquals(user.getStatus(), dto.getStatus());
    assertEquals(user.getAvatar(), dto.getAvatar());
    assertEquals(user.getBirthday(), dto.getBirthday());
    assertEquals(user.getGamesPlayed(), dto.getGamesPlayed());
    assertEquals(user.getScoreTotal(), dto.getScoreTotal());
    assertEquals(user.getMoonShots(), dto.getMoonShots());
  }

  @Test
  void testConvertUserPutDTOtoEntity_success() {
    UserPutDTO dto = new UserPutDTO();
    dto.setUsername("updated");
    dto.setAvatar(123);

    User user = DTOMapper.INSTANCE.convertUserPutDTOtoEntity(dto);

    assertEquals("updated", user.getUsername());
    assertEquals(123, user.getAvatar());
  }

  @Test
  void testConvertEntityToUserAuthDTO_success() {
    User user = new User();
    user.setId(5L);
    user.setUsername("authuser");
    user.setToken("abc123");
    user.setStatus(UserStatus.OFFLINE);
    user.setAvatar(123);
    user.setIsGuest(true);

    UserAuthDTO dto = DTOMapper.INSTANCE.convertEntityToUserAuthDTO(user);

    assertEquals(5L, dto.getId());
    assertEquals("authuser", dto.getUsername());
    assertEquals("abc123", dto.getToken());
    assertEquals(UserStatus.OFFLINE, dto.getStatus());
    assertEquals(123, dto.getAvatar());
    assertTrue(dto.getIsGuest());
  }

  @Test
  void testConvertEntityToUserPrivateDTO_success() {
    User user = new User();
    user.setId(9L);
    user.setUsername("private");
    user.setAvatar(123);

    UserPrivateDTO dto = DTOMapper.INSTANCE.convertEntityToUserPrivateDTO(user);

    assertEquals(user.getId(), dto.getId());
    assertEquals(user.getUsername(), dto.getUsername());
    assertEquals(user.getAvatar(), dto.getAvatar());
    assertNull(dto.getParticipantOfActiveMatchId());
  }

  @Test
  void testMapToUserPrivateDTOWithActiveMatch_success() {
    User user = new User();
    user.setId(10L);
    user.setUsername("inmatch");

    UserPrivateDTO dto = DTOMapper.INSTANCE.mapToUserPrivateDTOWithActiveMatch(user, 42L);

    assertEquals(42L, dto.getParticipantOfActiveMatchId());
    assertEquals("inmatch", dto.getUsername());
  }

  @Test
  void testConvertUserAuthDTOtoEntity_success() {
    UserAuthDTO dto = new UserAuthDTO();
    dto.setId(7L);
    dto.setUsername("backuser");
    dto.setToken("xyz987");
    dto.setStatus(UserStatus.ONLINE);

    User user = DTOMapper.INSTANCE.convertUserAuthDTOtoEntity(dto);

    assertEquals(7L, user.getId());
    assertEquals("backuser", user.getUsername());
    assertEquals("xyz987", user.getToken());
    assertEquals(UserStatus.ONLINE, user.getStatus());
  }

  @Test
  void testConvertToLeaderboardDTO_success() {
    User user = new User();
    user.setId(12L);
    user.setUsername("leader");
    user.setScoreTotal(300);
    user.setGamesPlayed(30);
    user.setAvgGameRanking(2.0f);
    user.setPerfectGames(3);

    LeaderboardDTO dto = DTOMapper.INSTANCE.convertToLeaderboardDTO(user);

    assertEquals("leader", dto.getUsername());
    assertEquals(300, dto.getScoreTotal());
    assertEquals(30, dto.getGamesPlayed());
    assertEquals(2.0, dto.getAvgGameRanking());
    assertEquals(3, dto.getPerfectGames());
  }

  @Test
  void testConvertAiPlayersToFrontendFormat_success() {
    Map<Integer, Integer> backendMap = Map.of(1, 101, 2, 102, 4, 104);

    Map<Integer, Integer> frontendMap = DTOMapper.INSTANCE.convertAiPlayersToFrontendFormat(backendMap);

    assertEquals(3, frontendMap.size());
    assertEquals(101, frontendMap.get(0));
    assertEquals(102, frontendMap.get(1));
    assertEquals(104, frontendMap.get(3));
  }

  @Test
  void testConvertEntityToMatchDTO_minimal_success() {
    Match match = new Match();
    match.setMatchId(99L);
    match.setHostId(5L);
    match.setHostUsername("hoster");
    match.setMatchGoal(50);
    match.setStarted(false);
    match.setAiPlayers(Map.of(1, 123, 2, 456));

    MatchDTO dto = DTOMapper.INSTANCE.convertEntityToMatchDTO(match);

    assertEquals(99L, dto.getMatchId());
    assertEquals(5L, dto.getHostId());
    assertEquals("hoster", dto.getHostUsername());
    assertEquals(50, dto.getMatchGoal());
    assertEquals(false, dto.getStarted());
    assertEquals(123, dto.getAiPlayers().get(0));
    assertEquals(456, dto.getAiPlayers().get(1));
  }
}
