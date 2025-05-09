package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;

import java.util.HashMap;
import java.util.Map;

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

  @Named("convertAiPlayersToFrontendFormat")
  default Map<Integer, Integer> convertAiPlayersToFrontendFormat(Map<Integer, Integer> aiPlayers) {
    if (aiPlayers == null) {
      return null;
    }

    Map<Integer, Integer> translated = new HashMap<>();
    for (Map.Entry<Integer, Integer> entry : aiPlayers.entrySet()) {
      int backendSlot = entry.getKey(); // e.g., 1–4
      int frontendSlot = backendSlot - 1; // convert to 0-based
      translated.put(frontendSlot, entry.getValue());
    }
    return translated;
  }

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

  @Mapping(target = "participantOfActiveMatchId", ignore = true) // Ignore this field during normal mapping
  @Mapping(target = "participantOfActiveMatchPhase", ignore = true) // Ignore this field during normal mapping
  UserPrivateDTO convertEntityToUserPrivateDTO(User user);

  // Add a custom method to set participantOfActiveMatchId
  default UserPrivateDTO mapToUserPrivateDTOWithActiveMatch(User user, Long activeMatchId) {
    UserPrivateDTO dto = convertEntityToUserPrivateDTO(user); // Convert User to DTO
    dto.setParticipantOfActiveMatchId(activeMatchId); // Set the custom field
    return dto;
  }

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
  @Mapping(source = "hostUsername", target = "hostUsername")
  @Mapping(source = "matchGoal", target = "matchGoal")
  @Mapping(source = "matchPlayers", target = "matchPlayerIds")
  @Mapping(source = "started", target = "started")
  @Mapping(source = "aiPlayers", target = "aiPlayers", qualifiedByName = "convertAiPlayersToFrontendFormat")
  @Mapping(source = "joinRequests", target = "joinRequests")
  @Mapping(source = "player1", target = "player1Id")
  @Mapping(source = "player2", target = "player2Id")
  @Mapping(source = "player3", target = "player3Id")
  @Mapping(source = "player4", target = "player4Id")
  MatchDTO convertEntityToMatchDTO(Match match);
}
