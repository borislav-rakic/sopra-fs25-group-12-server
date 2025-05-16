package ch.uzh.ifi.hase.soprafs24.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.GameConstants;
import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.MatchSummary;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.logic.GameEnforcer;
//import ch.uzh.ifi.hase.soprafs24.model.Card;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayerCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PollingDTO;
import ch.uzh.ifi.hase.soprafs24.util.CardUtils;

@Service
@Qualifier("pollingService")
public class PollingService {

    private final Logger log = LoggerFactory.getLogger(PollingService.class);

    private final CardRulesService cardRulesService;
    private final MatchMessageService matchMessageService;
    private final GameTrickService gameTrickService;

    @Autowired
    public PollingService(
            @Qualifier("cardRulesService") CardRulesService cardRulesService,
            @Qualifier("matchMessageService") MatchMessageService matchMessageService,
            @Qualifier("gameTrickService") GameTrickService gameTrickService) {
        this.cardRulesService = cardRulesService;
        this.matchMessageService = matchMessageService;
        this.gameTrickService = gameTrickService;
    }

    /**
     * Gets the necessary information for a player.
     * 
     * @param token   The player's token
     * @param matchId The id of the match the user is in
     * @return Information specific to a player (e.g. their current cards)
     */
    public PollingDTO getPlayerPolling(User user, Match match, GameRepository gameRepository,
            MatchPlayerRepository matchPlayerRepository) {
        // MATCH [1], [2], [3], [4]
        // GAME [11], [12]

        //

        Game game = GameEnforcer.requireExactlyOneActiveGame(match);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "There is no active  game in this match (Polling).");
        }

        // MATCHPLAYER
        MatchPlayer matchPlayer = matchPlayerRepository.findByUserAndMatch(user, match);
        if (matchPlayer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }
        // Positively identify requesting user in list of matchPlayers
        MatchPlayer requestingMatchPlayer = null;
        for (MatchPlayer player : match.getMatchPlayers()) {
            if (player.getUser().getId().equals(user.getId())) {
                requestingMatchPlayer = player;
                break;
            }
        }
        if (requestingMatchPlayer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Requesting Player does not appear to be part of this Match.");
        }

        // Prevent "overpolling" no sooner than 25% last poll + poll duration.
        // if (GameConstants.PREVENT_OVERPOLLING && !canPoll(requestingMatchPlayer)) {
        // log.info("Detected overpolling by MatchPlayer in MatchPlayerSlot {}.",
        // requestingMatchPlayer.getMatchPlayerSlot());
        // throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "You are
        // polling too frequently.");
        // }

        // log their visit in "lastHeardFrom"
        requestingMatchPlayer.updateLastPollTime();
        requestingMatchPlayer.incrementPollCounter();
        int pollCounter = requestingMatchPlayer.getPollCounter();

        // The index in the player array that represents the requesting player
        // "me"____ = "south"_ = position 0 on client
        // "left"__ = "west"__ = position 1 on client
        // "across" = "north"_ = position 2 on client
        // "right"_ = "east"__ = position 3 on client

        // int positionIndex = requestingMatchPlayer.getMatchPlayerSlot() - 1;

        // OTHER PLAYERS

        /// AVATAR
        List<String> avatarUrls = new ArrayList<>();
        User[] players = {
                match.getPlayer1(),
                match.getPlayer2(),
                match.getPlayer3(),
                match.getPlayer4()
        };

        for (User player : players) {
            if (player != null && player.getAvatar() != null) {
                avatarUrls.add("/avatars_118x118/a" + player.getAvatar() + ".png");
            } else {
                avatarUrls.add("/avatars_118x118/a0.png"); // Or default avatar URL
            }
        }

        // Number of cards in hand per player, keyed by 0-based position index (slot 1 →
        // index 0, etc.)
        // The frontend rotates these values based on the client's relative position.
        Map<Integer, Integer> handCounts = new HashMap<>();
        for (MatchPlayer mp : match.getMatchPlayers()) {
            int count = mp.getHandCardsArray().length;
            handCounts.put(mp.getMatchPlayerSlot() - 1, count);
        }

        // Total match points per player, keyed by 0-based position index
        // (matchPlayerSlot - 1)
        // The frontend uses this to display relative scores around the table.
        Map<Integer, Integer> pointsOfPlayers = new HashMap<>();
        for (MatchPlayer mp : match.getMatchPlayers()) {
            pointsOfPlayers.put(mp.getMatchPlayerSlot() - 1, mp.getMatchScore());
        }

        // Usernames and User objects in position index order (0 = match owner,
        // clockwise)
        List<String> matchPlayers = new ArrayList<>();
        matchPlayers.add(match.getPlayer1() != null ? match.getPlayer1().getUsername() : null);
        matchPlayers.add(match.getPlayer2() != null ? match.getPlayer2().getUsername() : null);
        matchPlayers.add(match.getPlayer3() != null ? match.getPlayer3().getUsername() : null);
        matchPlayers.add(match.getPlayer4() != null ? match.getPlayer4().getUsername() : null);

        List<User> usersInMatch = new ArrayList<>();
        usersInMatch.add(match.getPlayer1());
        usersInMatch.add(match.getPlayer2());
        usersInMatch.add(match.getPlayer3());
        usersInMatch.add(match.getPlayer4());

        /// Info about me
        // My cards in my hand

        List<String> sortedHand = CardUtils.requireSplitCardCodesAsListOfStrings(
                CardUtils.normalizeCardCodeString(matchPlayer.getHand()));

        List<PlayerCardDTO> playerCardDTOList = sortedHand.stream()
                .map(cardCode -> {
                    PlayerCardDTO dtoCard = new PlayerCardDTO();
                    dtoCard.setCard(cardCode);
                    dtoCard.setGameId(game.getGameId());
                    dtoCard.setGameNumber(game.getGameNumber());
                    dtoCard.setPlayerId(user.getId());
                    return dtoCard;
                })
                .collect(Collectors.toList());

        String hand = CardUtils.normalizeCardCodeString(matchPlayer.getHand());

        // Playable cards in my hand
        List<PlayerCardDTO> playableCardDTOList = new ArrayList<>();
        String playableCards = "";

        // Only show playable cards if it is this player's turn and it is a nonPausing
        // TrickPhase
        if (matchPlayer.getMatchPlayerSlot() == game.getCurrentMatchPlayerSlot()
                && !game.getTrickPhase().inTransition()) {
            playableCards = CardUtils.normalizeCardCodeString(
                    cardRulesService.getPlayableCardsForMatchPlayerPolling(game, matchPlayer));

            if (playableCards != null && !playableCards.isBlank()) {
                playableCardDTOList = java.util.Arrays.stream(playableCards.split(","))
                        .map(cardCode -> {
                            PlayerCardDTO dtoCard = new PlayerCardDTO();
                            dtoCard.setCard(cardCode);
                            dtoCard.setGameId(game.getGameId());
                            dtoCard.setGameNumber(game.getGameNumber());
                            dtoCard.setPlayerId(user.getId());
                            return dtoCard;
                        })
                        .toList();
            }
        }

        // START AGGREGATING INFO ON PlayerPolling

        PollingDTO dto = new PollingDTO();

        dto.setPollCounter(pollCounter);

        dto.setMatchId(match.getMatchId()); // [1]
        dto.setMatchGoal(match.getMatchGoal()); // [2]
        dto.setHostId(match.getHostId()); // [3]
        dto.setMatchPhase(match.getPhase()); // [4]

        dto.setGamePhase(game.getPhase()); // [11a]
        dto.setTrickPhase(game.getTrickPhase()); // [12]
        dto.setHeartsBroken(game.getHeartsBroken()); // [13]
        dto.setCurrentTrickDTO(gameTrickService.prepareTrickDTO(match, game, matchPlayer)); // [14]
        dto.setPreviousTrickDTO(gameTrickService.preparePreviousTrickDTO(match, game, matchPlayer));

        // During GamePhase.RESULT show prepared game summary

        if (game.getPhase() == GamePhase.RESULT) {
            MatchSummary matchSummary = match.getMatchSummary();
            dto.setResultHtml(matchSummary.getGameSummaryHtml());
        }
        dto.setMatchMessages(matchMessageService.messages(match, game, matchPlayer)); // [18c]
        // Info about the other players
        dto.setMatchPlayers(matchPlayers); // [21]
        dto.setAvatarUrls(avatarUrls); // [22]
        dto.setCardsInHandPerPlayer(handCounts); // [23]
        dto.setPlayerPoints(pointsOfPlayers); // [24]
        dto.setAiPlayers(match.getAiPlayers()); // [25]
        dto.setCurrentPlayerSlot(game.getCurrentMatchPlayerSlot() - 1);
        dto.setCurrentPlayOrder(game.getCurrentPlayOrder());

        // Info about myself
        dto.setMatchPlayerSlot(matchPlayer.getMatchPlayerSlot());
        dto.setPlayerSlot(matchPlayer.getMatchPlayerSlot() - 1);
        dto.setMyTurn(
                // My slot is supposed to play
                matchPlayer.getMatchPlayerSlot() == game.getCurrentMatchPlayerSlot()
                        // TrickPhase is neither TRICKJUSTCOMPLETED nor PROCESSINGTRICK
                        && !game.getTrickPhase().inTransition()); // [32]
        dto.setPlayerCards(playerCardDTOList); // [33a]
        dto.setPlayerCardsAsString(hand); // [33b]
        dto.setPlayableCards(playableCardDTOList); // [34a]
        dto.setPlayableCardsAsString(playableCards); // [34b]
        if (game.getPhase() == GamePhase.PASSING
                || game.getPhase() == GamePhase.SKIP_PASSING) {
            String passingInfo = cardRulesService.namePassingRecipient(
                    match,
                    game.getGameNumber(),
                    matchPlayer.getMatchPlayerSlot());
            dto.setPassingInfo(passingInfo); // [34c]
            Integer passingToMatchPlayerSlot = cardRulesService.getPassingToMatchPlayerSlot(
                    game.getGameNumber(),
                    matchPlayer.getMatchPlayerSlot());
            // ofset by -1 for frontend-logic
            dto.setPassingToPlayerSlot(passingToMatchPlayerSlot - 1);
            log.info("Getting info on my (MPSlot={}) passing in Game#{}, to: {} in MatchPlayerSlot={}.",
                    matchPlayer.getMatchPlayerSlot(),
                    game.getGameNumber(),
                    passingInfo,
                    passingToMatchPlayerSlot);
        } else {
            dto.setPassingInfo("");
            dto.setPassingToPlayerSlot(null);
        }
        return dto;
    }

    /**
     * Gets the necessary information for a player in MatchPhase:
     * RESULT, FINISHED, or ABORTED.
     *
     * @param user           The user making the request
     * @param match          The match in question
     * @param showGameResult If true, the last game result will be shown instead of
     *                       match result
     * @return PollingDTO with the appropriate post-match content
     */
    public PollingDTO getPlayerPollingForPostMatchPhase(User user, Match match, boolean showGameResult) {
        if (showGameResult) {
            return gameResultMessage(match);
        }

        return switch (match.getPhase()) {
            case RESULT -> matchResultMessage(match);
            case FINISHED -> matchFinishedMessage(match);
            case ABORTED -> matchAbortedMessage(match);
            default -> throw new IllegalStateException(
                    "getPlayerPollingForPostMatchPhase called in unexpected MatchPhase: " + match.getPhase());
        };
    }

    private PollingDTO matchResultMessage(Match match) {
        PollingDTO dto = new PollingDTO();
        dto.setHostId(match.getHostId());
        dto.setMatchId(match.getMatchId());
        dto.setMatchGoal(match.getMatchGoal());
        dto.setMatchPlayers(match.getMatchPlayerNames());
        dto.setPlayerPoints(match.getMatchScoresMap());
        dto.setGamePhase(GamePhase.FINISHED);
        dto.setMatchPhase(MatchPhase.FINISHED);
        MatchSummary matchSummary = match.getMatchSummary();
        if (matchSummary != null) {
            dto.setResultHtml(matchSummary.getMatchSummaryHtml());
        } else {
            dto.setResultHtml("<div>This match is over.</div>");
        }
        return dto;
    }

    private PollingDTO gameResultMessage(Match match) {
        PollingDTO dto = new PollingDTO();
        dto.setHostId(match.getHostId());
        dto.setMatchId(match.getMatchId());
        dto.setMatchGoal(match.getMatchGoal());
        dto.setMatchPlayers(match.getMatchPlayerNames());
        dto.setPlayerPoints(match.getMatchScoresMap());
        dto.setGamePhase(GamePhase.FINISHED);
        dto.setMatchPhase(MatchPhase.RESULT); // This player has not yet confirmed the GameResult
        MatchSummary matchSummary = match.getMatchSummary();
        if (matchSummary != null) {
            dto.setResultHtml(matchSummary.getGameSummaryHtml());
        } else {
            dto.setResultHtml("<div>This match is over.</div>");
        }
        return dto;
    }

    private PollingDTO matchFinishedMessage(Match match) {
        return matchResultMessage(match);
    }

    private PollingDTO matchAbortedMessage(Match match) {
        return matchResultMessage(match);
    }

    public PollingDTO getSpectatorPolling(User user, Match match) {
        if (match.getPhase() == MatchPhase.RESULT) {
            return matchResultMessage(match);
        } else if (match.getPhase() == MatchPhase.FINISHED) {
            return matchFinishedMessage(match);
        } else if (match.getPhase() == MatchPhase.ABORTED) {
            return matchAbortedMessage(match);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This match is ongoing, but you are not part of it.");
        }
    }
}
