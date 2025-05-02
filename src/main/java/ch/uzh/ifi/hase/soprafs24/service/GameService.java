package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.GameConstants;
import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.constant.Strategy;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.model.Card;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePassingDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayerCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PollingDTO;
import ch.uzh.ifi.hase.soprafs24.util.CardUtils;
import ch.uzh.ifi.hase.soprafs24.util.StrategyRegistry;

/**
 * Game Service
 * This class is the "worker" and responsible for all functionality related to
 * currently ongoing games, e.g. updating the player's scores, requesting
 * information
 * from the deck of cards API, etc.
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */

@Service
@Transactional
public class GameService {
    private final AiPlayingService aiPlayingService;
    private final CardPassingService cardPassingService;
    private final CardRulesService cardRulesService;
    private final GameRepository gameRepository;
    private final GameStatsService gameStatsService;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;

    // or via constructor injection

    @Autowired
    public GameService(
            @Qualifier("aiPlayingService") AiPlayingService aiPlayingService,
            @Qualifier("cardPassingService") CardPassingService cardPassingService,
            @Qualifier("cardRulesService") CardRulesService cardRulesService,
            @Qualifier("gameRepository") GameRepository gameRepository,
            @Qualifier("gameStatsService") GameStatsService gameStatsService,
            @Qualifier("matchPlayerRepository") MatchPlayerRepository matchPlayerRepository,
            @Qualifier("matchRepository") MatchRepository matchRepository) {
        this.aiPlayingService = aiPlayingService;
        this.cardPassingService = cardPassingService;
        this.cardRulesService = cardRulesService;
        this.gameRepository = gameRepository;
        this.gameStatsService = gameStatsService;
        this.matchPlayerRepository = matchPlayerRepository;
        this.matchRepository = matchRepository;

    }

    private static final Logger log = LoggerFactory.getLogger(GameService.class);
    private static final Boolean PLAY_ALL_AI_TURNS_AT_ONCE = false;

    /**
     * Gets the necessary information for a player.
     * 
     * @param token   The player's token
     * @param matchId The id of the match the user is in
     * @return Information specific to a player (e.g. their current cards)
     */
    public PollingDTO getPlayerPolling(User user, Match match) {
        // MATCH [1], [2], [3], [4]
        // GAME [11], [12]
        Game game = match.getActiveGame();
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no active  game in this match.");
        }
        // determine if owner is polling
        int currentSlot = game.getCurrentMatchPlayerSlot();
        User currentSlotUser = match.requireUserBySlot(currentSlot);

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

        boolean poolingMatchOwner = (requestingMatchPlayer.getMatchPlayerSlot() == 1);

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

        if (poolingMatchOwner || match.getPhase() == MatchPhase.FINISHED) {
            // Does match happen to be over?
            if (match.getPhase() == MatchPhase.FINISHED) {
                dto.setMatchPhase(MatchPhase.FINISHED);
                dto.setResultHtml(buildMatchResultHtml(match, game));
                return dto;
            }
            // does game happen to be over?
            if (game.getPhase() == GamePhase.FINALTRICK
                    && game.getCurrentPlayOrder() >= GameConstants.FULL_DECK_CARD_COUNT) {
                // set all MatchPlayers to NOT ready.
                resetAllPlayersReady(match.getMatchId());
                dto.setResultHtml(buildGameResultHtml(match, game));
                dto.setGamePhase(GamePhase.RESULT);
                return dto;
            }
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
        dto.setPreviousTrickPoints(game.getPreviousTrickPoints()); // [18]

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

        /// See if an AI PLAYER is up for their turn.
        if (
        // ... user 1, i.e. the match owner, happens to poll ...
        poolingMatchOwner
                // ... there is a user in current matchPlayerSlot ...
                && currentSlotUser != null
                // ... this user happens to be an ai player ...
                && Boolean.TRUE.equals(currentSlotUser.getIsAiPlayer())
                // ... we are in the middle of playing an actual trick
                && (game.getPhase() == GamePhase.FIRSTTRICK || game.getPhase() == GamePhase.NORMALTRICK
                        || game.getPhase() == GamePhase.FINALTRICK)) {
            playAiTurnsUntilHuman(game.getGameId()); // do not inject matchPlayer (runs independent of game owner)
        }
        /// END: AI PLAYER
        return dto;
    }

    public void playAiTurnsUntilHuman(Long gameId) {
        while (true) {
            boolean played = playSingleAiTurn(gameId);
            if (!played || !PLAY_ALL_AI_TURNS_AT_ONCE)
                break;
        }
    }

    @Transactional
    public boolean playSingleAiTurn(Long gameId) {
        Game game = gameRepository.findByGameId(gameId);
        if (game == null) {
            System.out.println(
                    "Location: playAiTurns. Initiating an AiTurn, but the passed game argument is null.");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Could not find game (playSingleAiTurn)."));
        }

        Match match = game.getMatch();

        MatchPlayer aiPlayer = match.requireMatchPlayerBySlot(game.getCurrentMatchPlayerSlot());

        // Stop if it's a human's turn or no AI player
        if (aiPlayer == null || !Boolean.TRUE.equals(aiPlayer.getIsAiPlayer())) {
            if (game.getCurrentPlayOrder() > GameConstants.FULL_DECK_CARD_COUNT) {
                return false;
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No Player found a this slot.");
        }
        Strategy strategy = StrategyRegistry.getStrategyForUserId(aiPlayer.getUser().getId());
        // For now always play predictably the leftmost card.
        strategy = Strategy.LEFTMOST;
        String cardCode = aiPlayingService.selectCardToPlay(game, aiPlayer, strategy);
        try {
            // Thread.sleep(300 + new Random().nextInt(400)); // fancy version
            Thread.sleep(50); // short and simple
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        playCardAsAi(game, aiPlayer, cardCode);
        return true;
    }

    public void playCardAsHuman(Game game, MatchPlayer matchPlayer, String cardCode) {
        log.info("=== PLAY CARD AS HUMAN ({}), CurrentSlot: {}.",
                game.getCurrentPlayOrder(), game.getCurrentMatchPlayerSlot());
        log.info("= HUMAN at matchPlayerSlot {} attempting to play card {}.", matchPlayer.getInfo(),
                cardCode);
        // Defensive checks
        if (game.getPhase().isNotActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Game is not active.");
        }
        int matchPlayerSlot = matchPlayer.getMatchPlayerSlot();
        int playerSlot = matchPlayerSlot - 1;
        int currentMatchPlayerSlot = game.getCurrentMatchPlayerSlot();
        int currentPlayerSlot = currentMatchPlayerSlot - 1;
        if (matchPlayerSlot != game.getCurrentMatchPlayerSlot()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    String.format("Not your turn, playerSlot %d; currentPlayerSlot is %d.", playerSlot,
                            currentPlayerSlot));
        }
        log.info("= HUMAN at matchPlayerSlot {} attempting to play card {}.", matchPlayer.getInfo(),
                cardCode);
        log.info(
                "= Cards in hand are: {}. TrickLeader is: {}. Cards played in this trick: {}. PlayOrder: {}. TrickNumber: {}",
                matchPlayer.getHand(),
                game.getTrickLeaderMatchPlayerSlot(), game.getCurrentTrick(), game.getCurrentPlayOrder(),
                game.getCurrentTrickNumber());

        if (cardCode == "XX") {
            cardCode = aiPlayingService.selectCardToPlay(game, matchPlayer, Strategy.RANDOM);
        }
        cardRulesService.validateMatchPlayerCardCode(game, matchPlayer, cardCode);
        log.info(
                "= About to executeValidatedCardPlay");
        executeValidatedCardPlay(game, matchPlayer, cardCode);
        log.info("=== PLAY CARD AS HUMAN CONCLUDED ({}) ===", game.getCurrentPlayOrder());
    }

    public void playCardAsAi(Game game, MatchPlayer aiPlayer, String cardCode) {
        CardUtils.requireValidCardFormat(cardCode);

        String hand = aiPlayer.getHand();
        if (hand == null || hand.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The AiPlayer {} has no cards in hand.", aiPlayer.getInfo()));
        }

        if (!aiPlayer.hasCardCodeInHand(cardCode)) {
            int playerSlot = aiPlayer.getMatchPlayerSlot();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The AiPlayer in playerSlot " + playerSlot + " does not have the card " + cardCode
                            + " in hand.");
        }

        int matchPlayerSlot = aiPlayer.getMatchPlayerSlot();
        if (matchPlayerSlot < 1 || matchPlayerSlot > 4) {
            int playerSlot = matchPlayerSlot - 1;
            throw new IllegalStateException(
                    "The AI player " + aiPlayer.getMatchPlayerId() + " is in an invalid playerSlot (" + playerSlot
                            + ").");
        }

        if (!Boolean.TRUE.equals(aiPlayer.getUser().getIsAiPlayer())) {
            int playerSlot = matchPlayerSlot - 1;
            throw new IllegalStateException("playerSlot " + playerSlot + " is not controlled by an AI player.");
        }

        if (matchPlayerSlot != game.getCurrentMatchPlayerSlot()) {
            int playerSlot = matchPlayerSlot - 1;
            throw new IllegalStateException("AI tried to play out of turn (playerSlot " + playerSlot + ").");
        }

        cardRulesService.validateMatchPlayerCardCode(game, aiPlayer, cardCode);
        executeValidatedCardPlay(game, aiPlayer, cardCode);
    }

    public void executeValidatedCardPlay(Game game, MatchPlayer matchPlayer, String cardCode) {
        log.info("   +-- executeValidatedCardPlay ---");

        log.info("   | Playing card: {} by MatchPlayer {} (before play: hand = {})",
                cardCode, matchPlayer.getInfo(), matchPlayer.getHand());

        // Step 1: Remove the card from hand
        if (!matchPlayer.removeCardCodeFromHand(cardCode)) {
            throw new IllegalStateException("Tried to remove a card that wasn't in hand: " + cardCode);
        }

        // Step 2: Add the card to the trick
        game.addCardCodeToCurrentTrick(cardCode);

        log.info("   | Card {} added to trick: {}. Hand now: {}", cardCode, game.getCurrentTrick(),
                matchPlayer.getHand());
        game.setCurrentPlayOrder(game.getCurrentPlayOrder() + 1);
        log.info("CURRENTPLAYORDER {}", game.getCurrentPlayOrder());
        game.updateGamePhaseBasedOnPlayOrder();

        // Step 3: Advance the turn if trick is not complete
        if (game.getCurrentTrickSize() < 4) {
            int nextSlot = (game.getCurrentMatchPlayerSlot() % 4) + 1;
            game.setCurrentMatchPlayerSlot(nextSlot);
            log.info("   | Turn advanced to next matchPlayerSlot: {}", game.getCurrentMatchPlayerSlot());
        }

        // Step 4: Handle potential trick completion
        handlePotentialTrickCompletion(game);

        // Step 5: Record the completed play — now that state is stable
        gameStatsService.recordCardPlay(game, matchPlayer, cardCode);
        log.info("   | Stats recorded for card {} by MatchPlayer {}", cardCode, matchPlayer.getInfo());
        log.info("   +--- executeValidatedCardPlay ---");
    }

    private void handlePotentialTrickCompletion(Game game) {
        log.info(" (No trick completion yet.)");
        if (game.getCurrentTrickSize() != 4) {
            return; // Trick is not complete yet
        }
        log.info(" &&& TRICK COMPLETION: {}. &&&", game.getCurrentTrick());

        // Step 1: Determine winner and points
        int winnerMatchPlayerSlot = cardRulesService.determineTrickWinner(game);
        int points = cardRulesService.calculateTrickPoints(game.getCurrentTrick());

        log.info(" & Trick winnerMatchPlayerSlot {} ({} points)", winnerMatchPlayerSlot, points);

        // Step 2: Archive the trick
        game.setPreviousTrick(game.getCurrentTrick());
        game.setPreviousTrickWinnerMatchPlayerSlot(winnerMatchPlayerSlot);
        game.setPreviousTrickPoints(points);
        game.setPreviousTrickLeaderMatchPlayerSlot(game.getTrickLeaderMatchPlayerSlot());

        // Step 3: Prepare for the next trick
        game.updateGamePhaseBasedOnPlayOrder();
        game.setCurrentTrickNumber(game.getCurrentTrickNumber() + 1);
        game.clearCurrentTrick(); // also clears currentTrickMatchPlayerSlot internally

        // Step 4: Set next trick leader — which resets trick matchPlayerSlots properly
        game.setTrickLeaderMatchPlayerSlot(winnerMatchPlayerSlot); // internally sets new matchPlayerSlots order
        game.setCurrentMatchPlayerSlot(winnerMatchPlayerSlot);

        log.info(" & New trick lead is matchPlayerSlot {}. New trickMatchPlayerSlotOrder is: {}.",
                winnerMatchPlayerSlot, game.getTrickMatchPlayerSlotOrderAsString());

        // Step 5: Persist state
        gameRepository.save(game);

        log.info(" & Trick transitioned. New currentMatchPlayerSlot: {}. Current trick #: {}.",
                game.getCurrentMatchPlayerSlot(), game.getCurrentTrickNumber());
        log.info(" &&& TRICK COMPLETION CONCLUDED &&&");
    }

    @Transactional
    public void passingAcceptCards(Game game, MatchPlayer matchPlayer, GamePassingDTO passingDTO,
            Boolean pickRandomly) {
        cardPassingService.passingAcceptCards(game, matchPlayer, passingDTO, pickRandomly);
        gameRepository.saveAndFlush(game);
    }

    public Game getActiveGameByMatchId(Long matchId) {
        Match match = matchRepository.findMatchByMatchId(matchId);
        if (match == null) {
            throw new EntityNotFoundException("Match not found");
        }

        // Always fetch the Game from the database based on match and phase
        Game game = gameRepository.findFirstByMatchAndPhaseNotIn(
                match, List.of(GamePhase.FINISHED, GamePhase.ABORTED));

        if (game == null) {
            throw new IllegalStateException("No active game found for this match");
        }

        return game;
    }

    @Transactional
    public void resetAllPlayersReady(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));

        List<MatchPlayer> players = match.getMatchPlayers();

        if (players.size() != 4) {
            throw new IllegalStateException("Expected 4 match players, but found " + players.size());
        }

        for (MatchPlayer player : players) {
            player.resetReady(); // Custom logic
        }
    }

    public String buildGameResultHtml(Match match, Game game) {
        String html = "<div>GAME OVER</div>";
        return html;
    }

    public String buildMatchResultHtml(Match match, Game game) {
        String html = "<div>MATCH OVER</div>";
        return html;
    }
}
