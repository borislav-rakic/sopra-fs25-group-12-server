package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.model.Card;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayerCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PollingDTO;
import ch.uzh.ifi.hase.soprafs24.util.CardUtils;

@Service
@Qualifier("pollingService")
public class PollingService {

    // private final Logger log = LoggerFactory.getLogger(PollingService.class);

    private final CardRulesService cardRulesService;
    private final HtmlSummaryService htmlSummaryService;
    private final MatchMessageService matchMessageService;

    @Autowired
    public PollingService(
            @Qualifier("cardRulesService") CardRulesService cardRulesService,
            @Qualifier("htmlSummaryService") HtmlSummaryService htmlSummaryService,
            @Qualifier("matchMessageService") MatchMessageService matchMessageService) {
        this.cardRulesService = cardRulesService;
        this.htmlSummaryService = htmlSummaryService;
        this.matchMessageService = matchMessageService;

    }

    /**
     * Gets the necessary information for a player.
     * 
     * @param token   The player's token
     * @param matchId The id of the match the user is in
     * @return Information specific to a player (e.g. their current cards)
     */
    public PollingDTO getPlayerPolling(User user, Match match, MatchPlayerRepository matchPlayerRepository) {
        // MATCH [1], [2], [3], [4]
        // GAME [11], [12]
        Game game = match.getActiveGame();
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no active  game in this match.");
        }

        // TRICK [12]
        boolean isTrickInProgress = game.getCurrentTrickSize() > 0 && game.getCurrentTrickSize() < 4;

        // Current trick as List<Card> [14]
        List<Card> currentTrickAsCards = game.getCurrentTrick().stream()
                .map(CardUtils::fromCode)
                .collect(Collectors.toList());

        // Previous trick as List<Card> [14]
        List<Card> previousTrickAsCards = game.getPreviousTrick().stream()
                .map(CardUtils::fromCode)
                .collect(Collectors.toList());

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

        /// Number of cards in hand
        Map<Integer, Integer> handCounts = new HashMap<>();

        for (MatchPlayer mp : match.getMatchPlayers()) {
            int count = mp.getHandCardsArray().length;
            handCounts.put(mp.getMatchPlayerSlot(), count);
        }
        //// Points per player
        Map<Integer, Integer> pointsOfPlayers = new HashMap<>();
        for (MatchPlayer mp : match.getMatchPlayers()) {
            pointsOfPlayers.put(mp.getMatchPlayerSlot(), mp.getMatchScore());
        }

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

        List<PlayerCardDTO> playerCardDTOList = new ArrayList<>();
        String hand = CardUtils.normalizeCardCodeString(matchPlayer.getHand());

        for (String cardCode : matchPlayer.getHandCardsArray()) {
            PlayerCardDTO dtoCard = new PlayerCardDTO();
            dtoCard.setCard(cardCode);
            dtoCard.setGameId(game.getGameId());
            dtoCard.setGameNumber(game.getGameNumber());
            dtoCard.setPlayerId(user.getId());
            playerCardDTOList.add(dtoCard);
        }

        // Playable cards in my hand
        List<PlayerCardDTO> playableCardDTOList = new ArrayList<>();
        String playableCards = "";

        // Only show playable cards if it is this player's turn
        if (matchPlayer.getMatchPlayerSlot() == game.getCurrentMatchPlayerSlot()) {
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

        if (match.getPhase() == MatchPhase.FINISHED) {
            dto.setMatchPhase(MatchPhase.FINISHED);

            if (Boolean.TRUE.equals(matchPlayer.getIsReady())) {
                dto.setResultHtml(match.getSummary());
            } else {
                dto.setResultHtml(htmlSummaryService.buildGameResultHtml(match, game));
            }

            return dto;
        }

        dto.setMatchId(match.getMatchId()); // [1]
        dto.setMatchGoal(match.getMatchGoal()); // [2]
        dto.setHostId(match.getHostId()); // [3]
        dto.setMatchPhase(match.getPhase()); // [4]

        dto.setGamePhase(game.getPhase()); // [11]
        dto.setTrickInProgress(isTrickInProgress); // [12]
        dto.setHeartsBroken(game.getHeartsBroken()); // [13]
        dto.setCurrentTrick(currentTrickAsCards); // [14]

        dto.setCurrentTrickLeaderMatchPlayerSlot(game.getTrickLeaderMatchPlayerSlot()); // [15a]
        dto.setCurrentTrickLeaderPlayerSlot(game.getTrickLeaderMatchPlayerSlot() - 1); // [15b]

        dto.setPreviousTrick(previousTrickAsCards); // [16]
        if (game.getPreviousTrickWinnerMatchPlayerSlot() != null) {
            dto.setPreviousTrickWinnerMatchPlayerSlot(game.getPreviousTrickWinnerMatchPlayerSlot());
            dto.setPreviousTrickWinnerPlayerSlot(game.getPreviousTrickWinnerMatchPlayerSlot() - 1);
        }
        dto.setPreviousTrickPoints(game.getPreviousTrickPoints()); // [18a]

        dto.setMessages(matchMessageService.messages(match, game, matchPlayer)); // [18c]

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
        dto.setMyTurn(matchPlayer.getMatchPlayerSlot() == game.getCurrentMatchPlayerSlot()); // [32]
        dto.setPlayerCards(playerCardDTOList); // [33a]
        dto.setPlayerCardsAsString(hand); // [33b]
        dto.setPlayableCards(playableCardDTOList); // [34a]
        dto.setPlayableCardsAsString(playableCards); // [34b]
        return dto;
    }

}
