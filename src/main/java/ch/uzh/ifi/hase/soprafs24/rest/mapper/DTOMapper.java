package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically
 * transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g.,
 * UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for
 * creating information (POST).
 */
@Mapper
public interface DTOMapper {

  DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

  @Mapping(source = "password", target = "password")
  @Mapping(source = "username", target = "username")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "token", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "avatar", ignore = true)
  @Mapping(target = "isGuest", ignore = true)
  @Mapping(target = "isAiPlayer", ignore = true)
  @Mapping(target = "birthday", ignore = true)
  @Mapping(target = "userSettings", ignore = true)
  @Mapping(target = "scoreTotal", ignore = true)
  @Mapping(target = "gamesPlayed", ignore = true)
  @Mapping(target = "avgPlacement", ignore = true)
  @Mapping(target = "moonShots", ignore = true)
  @Mapping(target = "perfectGames", ignore = true)
  @Mapping(target = "perfectMatches", ignore = true)
  @Mapping(target = "currentStreak", ignore = true)
  @Mapping(target = "longestStreak", ignore = true)
  User convertUserCreateDTOtoEntity(UserCreateDTO userCreateDTO);

  @Mapping(source = "id", target = "id")
  @Mapping(source = "username", target = "username")
  @Mapping(source = "status", target = "status")
  @Mapping(source = "avatar", target = "avatar")
  @Mapping(source = "birthday", target = "birthday")
  @Mapping(source = "scoreTotal", target = "scoreTotal")
  @Mapping(source = "gamesPlayed", target = "gamesPlayed")
  @Mapping(source = "avgPlacement", target = "avgPlacement")
  @Mapping(source = "moonShots", target = "moonShots")
  @Mapping(source = "perfectGames", target = "perfectGames")
  @Mapping(source = "perfectMatches", target = "perfectMatches")
  @Mapping(source = "currentStreak", target = "currentStreak")
  @Mapping(source = "longestStreak", target = "longestStreak")
  UserGetDTO convertEntityToUserGetDTO(User user);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "isGuest", ignore = true)
  @Mapping(target = "isAiPlayer", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "token", ignore = true)
  @Mapping(target = "userSettings", ignore = true)
  @Mapping(target = "scoreTotal", ignore = true)
  @Mapping(target = "gamesPlayed", ignore = true)
  @Mapping(target = "avgPlacement", ignore = true)
  @Mapping(target = "moonShots", ignore = true)
  @Mapping(target = "perfectGames", ignore = true)
  @Mapping(target = "perfectMatches", ignore = true)
  @Mapping(target = "currentStreak", ignore = true)
  @Mapping(target = "longestStreak", ignore = true)
  User convertUserPutDTOtoEntity(UserPutDTO userPutDTO);

  @Mapping(source = "id", target = "id")
  @Mapping(source = "username", target = "username")
  @Mapping(source = "token", target = "token")
  @Mapping(source = "avatar", target = "avatar")
  @Mapping(source = "status", target = "status")
  @Mapping(source = "isGuest", target = "isGuest")
  UserAuthDTO convertEntityToUserAuthDTO(User user);

  @Mapping(source = "id", target = "id")
  @Mapping(source = "username", target = "username")
  @Mapping(source = "avatar", target = "avatar")
  @Mapping(source = "status", target = "status")
  @Mapping(source = "birthday", target = "birthday")
  @Mapping(source = "userSettings", target = "userSettings")
  @Mapping(source = "isGuest", target = "isGuest")
  UserPrivateDTO convertEntityToUserPrivateDTO(User user);

  @Mapping(source = "id", target = "id")
  @Mapping(source = "username", target = "username")
  @Mapping(source = "avatar", target = "avatar")
  @Mapping(source = "status", target = "status")
  @Mapping(source = "token", target = "token")
  @Mapping(target = "password", ignore = true)
  @Mapping(target = "isGuest", ignore = true)
  @Mapping(target = "isAiPlayer", ignore = true)
  @Mapping(target = "birthday", ignore = true)
  @Mapping(target = "userSettings", ignore = true)
  @Mapping(target = "scoreTotal", ignore = true)
  @Mapping(target = "gamesPlayed", ignore = true)
  @Mapping(target = "avgPlacement", ignore = true)
  @Mapping(target = "moonShots", ignore = true)
  @Mapping(target = "perfectGames", ignore = true)
  @Mapping(target = "perfectMatches", ignore = true)
  @Mapping(target = "currentStreak", ignore = true)
  @Mapping(target = "longestStreak", ignore = true)
  User convertUserAuthDTOtoEntity(UserAuthDTO userAuthDTO);

  @Mapping(source = "id", target = "id")
  @Mapping(source = "username", target = "username")
  @Mapping(source = "avgPlacement", target = "avgPlacement")
  @Mapping(source = "scoreTotal", target = "scoreTotal")
  @Mapping(source = "gamesPlayed", target = "gamesPlayed")
  @Mapping(source = "moonShots", target = "moonShots")
  @Mapping(source = "perfectGames", target = "perfectGames")
  @Mapping(source = "perfectMatches", target = "perfectMatches")
  @Mapping(source = "currentStreak", target = "currentStreak")
  @Mapping(source = "longestStreak", target = "longestStreak")
  LeaderboardDTO convertToLeaderboardDTO(User user);

  @Mapping(source = "matchId", target = "matchId")
  @Mapping(source = "hostId", target = "hostId")
  @Mapping(source = "matchGoal", target = "matchGoal")
  @Mapping(source = "matchPlayers", target = "matchPlayerIds")
  @Mapping(source = "started", target = "started")
  @Mapping(source = "aiPlayers", target = "aiPlayers")
  @Mapping(source = "joinRequests", target = "joinRequests")
  @Mapping(source = "player1", target = "player1Id")
  @Mapping(source = "player2", target = "player2Id")
  @Mapping(source = "player3", target = "player3Id")
  @Mapping(source = "player4", target = "player4Id")
  MatchDTO convertEntityToMatchDTO(Match match);
}
