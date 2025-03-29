package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
//import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;

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
  @Mapping(target = "birthday", ignore = true)
  @Mapping(target = "userSettings", ignore = true)
  @Mapping(target = "rating", ignore = true)
  User convertUserCreateDTOtoEntity(UserCreateDTO userCreateDTO);

  @Mapping(source = "id", target = "id")
  @Mapping(source = "username", target = "username")
  @Mapping(source = "status", target = "status")
  @Mapping(source = "avatar", target = "avatar")
  @Mapping(source = "birthday", target = "birthday")
  @Mapping(source = "rating", target = "rating")
  UserGetDTO convertEntityToUserGetDTO(User user);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "isGuest", ignore = true)
  @Mapping(target = "rating", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "token", ignore = true)
  @Mapping(target = "userSettings", ignore = true)
  User convertUserPutDTOtoEntity(UserPutDTO userPutDTO);

  @Mapping(source = "id", target = "id")
  @Mapping(source = "username", target = "username")
  @Mapping(source = "token", target = "token")
  @Mapping(source = "avatar", target = "avatar")
  @Mapping(source = "status", target = "status")
  UserAuthDTO convertEntityToUserAuthDTO(User user);

  @Mapping(source = "id", target = "id")
  @Mapping(source = "username", target = "username")
  @Mapping(source = "avatar", target = "avatar")
  @Mapping(source = "status", target = "status")
  @Mapping(source = "birthday", target = "birthday")
  @Mapping(source = "userSettings", target = "userSettings")
  @Mapping(source = "rating", target = "rating")
  UserPrivateDTO convertEntityToUserPrivateDTO(User user);

  @Mapping(source = "id", target = "id")
  @Mapping(source = "username", target = "username")
  @Mapping(source = "avatar", target = "avatar")
  @Mapping(source = "status", target = "status")
  @Mapping(source = "token", target = "token")
  @Mapping(target = "password", ignore = true)
  @Mapping(target = "isGuest", ignore = true)
  @Mapping(target = "birthday", ignore = true)
  @Mapping(target = "userSettings", ignore = true)
  @Mapping(target = "rating", ignore = true)
  User convertUserAuthDTOtoEntity(UserAuthDTO userAuthDTO);

  @Mapping(source = "id", target = "id")
  @Mapping(source = "username", target = "username")
  @Mapping(source = "rating", target = "rating")
  LeaderboardDTO convertToLeaderboardDTO(User user);

  @Mapping(source = "playerIds", target = "playerIds")
  Match convertMatchCreateDTOtoEntity(MatchCreateDTO matchCreateDTO);

  @Mapping(source = "matchId", target = "id")
  @Mapping(source = "playerIds", target = "players")
  MatchDTO convertEntityToMatchDTO(Match match);
}
