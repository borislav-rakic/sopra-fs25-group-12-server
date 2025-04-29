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

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.constant.Suit;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.model.Card;
import ch.uzh.ifi.hase.soprafs24.model.CardResponse;
import ch.uzh.ifi.hase.soprafs24.model.DrawCardResponse;
import ch.uzh.ifi.hase.soprafs24.model.NewDeckResponse;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameStatsRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import ch.uzh.ifi.hase.soprafs24.repository.PassedCardRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePassingDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameResultDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayedCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayerCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayerMatchInformationDTO;
import ch.uzh.ifi.hase.soprafs24.util.CardUtils;
import reactor.core.publisher.Mono;

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
    private final ExternalApiClientService externalApiClientService;
    private final GameRepository gameRepository;
    private final GameStatsRepository gameStatsRepository;
    private final GameStatsService gameStatsService;
    private final MatchPlayerRepository matchPlayerRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final MatchRepository matchRepository;

    // or via constructor injection

    @Autowired
    public GameService(
            @Qualifier("aiPlayingService") AiPlayingService aiPlayingService,
            @Qualifier("cardPassingService") CardPassingService cardPassingService,
            @Qualifier("cardRulesService") CardRulesService cardRulesService,
            @Qualifier("externalApiClientService") ExternalApiClientService externalApiClientService,
            @Qualifier("gameRepository") GameRepository gameRepository,
            @Qualifier("gameStatsRepository") GameStatsRepository gameStatsRepository,
            @Qualifier("gameStatsService") GameStatsService gameStatsService,
            @Qualifier("matchPlayerRepository") MatchPlayerRepository matchPlayerRepository,
            @Qualifier("matchRepository") MatchRepository matchRepository,
            @Qualifier("passedCardRepository") PassedCardRepository passedCardRepository,
            @Qualifier("userRepository") UserRepository userRepository,
            @Qualifier("userService") UserService userService) {
        this.aiPlayingService = aiPlayingService;
        this.cardPassingService = cardPassingService;
        this.cardRulesService = cardRulesService;
        this.externalApiClientService = externalApiClientService;
        this.gameRepository = gameRepository;
        this.gameStatsRepository = gameStatsRepository;
        this.gameStatsService = gameStatsService;
        this.matchPlayerRepository = matchPlayerRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.userService = userService;

    }

    private static final Logger log = LoggerFactory.getLogger(GameService.class);

    /**
     * Gets the necessary information for a player.
     * 
     * @param token   The player's token
     * @param matchId The id of the match the user is in
     * @return Information specific to a player (e.g. their current cards)
     */
    public PlayerMatchInformationDTO getPlayerMatchInformation(String token, Long matchId) {
        // MATCH [1], [2], [3], [4]
        Match match = matchRepository.findMatchByMatchId(matchId);
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }

        // USER
        User user = userRepository.findUserByToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        // GAME [11], [12]
        Game game = getActiveGameByMatchId(matchId);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no active  game in this match.");
        }

        // TRICK [12]
        boolean isTrickInProgress = game.getCurrentTrickSize() > 0 && game.getCurrentTrickSize() < 4;

        // Current trick as List<Card> [14]
        List<Card> currentTrickAsCards = game.getCurrentTrick().stream()
                .map(CardUtils::fromCode)
                .collect(Collectors.toList());

        // Last trick as List<Card> [14]
        List<Card> lastTrickAsCards = game.getLastTrick().stream()
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
            int count = gameStatsRepository
                    .findByGameAndCardHolderAndPlayedBy(game, mp.getSlot(), 0)
                    .size();
            handCounts.put(mp.getSlot(), count);
        }
        //// Points per player
        Map<Integer, Integer> pointsOfPlayers = new HashMap<>();
        for (MatchPlayer mp : match.getMatchPlayers()) {
            pointsOfPlayers.put(mp.getSlot(), mp.getMatchScore());
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

        List<GameStats> hand = gameStatsRepository.findByGameAndCardHolderAndPlayedBy(game, matchPlayer.getSlot(),
                0);
        for (GameStats gs : hand) {
            PlayerCardDTO dtoCard = new PlayerCardDTO();
            dtoCard.setCard(gs.getRankSuit());
            dtoCard.setGameId(game.getGameId());
            dtoCard.setGameNumber(game.getGameNumber());
            dtoCard.setPlayerId(user.getId());
            playerCardDTOList.add(dtoCard);
        }

        // Playable cards in my hand
        List<PlayerCardDTO> playableCardDTOList = new ArrayList<>();

        // Only show playable cards if it is this player's turn
        if (matchPlayer.getSlot() == game.getCurrentSlot()) {
            String playableCards = cardRulesService.getPlayableCardsForMatchPlayer(matchPlayer);

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

        // START AGGREGATING INFO ON PlayerMatchInformation

        PlayerMatchInformationDTO dto = new PlayerMatchInformationDTO();

        dto.setMatchId(match.getMatchId()); // [1]
        dto.setMatchGoal(match.getMatchGoal()); // [2]
        dto.setHostId(match.getHostId()); // [3]
        dto.setMatchPhase(match.getPhase()); // [4]

        dto.setGamePhase(game.getPhase()); // [11]
        dto.setTrickInProgress(isTrickInProgress); // [12]
        dto.setHeartsBroken(game.getHeartsBroken()); // [13]
        dto.setCurrentTrick(currentTrickAsCards); // [14]

        dto.setCurrentTrickLeaderSlot(game.getTrickLeaderSlot()); // [15]
        dto.setLastTrick(lastTrickAsCards); // [16]
        dto.setLastTrickWinnerSlot(game.getLastTrickWinnerSlot()); // [17]
        dto.setLastTrickPoints(game.getLastTrickPoints()); // [18]

        // Info about the other players
        dto.setMatchPlayers(matchPlayers); // [21]
        dto.setAvatarUrls(avatarUrls); // [22]
        dto.setCardsInHandPerPlayer(handCounts); // [23]
        dto.setPlayerPoints(pointsOfPlayers); // [24]
        dto.setAiPlayers(match.getAiPlayers()); // [25]

        // Info about myself
        dto.setSlot(matchPlayer.getSlot()); // [31]
        dto.setMyTurn(matchPlayer.getSlot() == game.getCurrentSlot()); // [32]
        dto.setPlayerCards(playerCardDTOList); // [33]
        dto.setPlayableCards(playableCardDTOList); // [34]

        /// See if an AI PLAYER is up for their turn.
        int currentSlot = game.getCurrentSlot();
        User currentSlotUser = match.getUserBySlot(currentSlot);
        if (
        // ... user 1, i.e. the match owner, happens to poll ...
        requestingMatchPlayer.getSlot() == 1
                // ... there is a user in current slot ...
                && currentSlotUser != null
                // ... this user happens to be an ai player ...
                && Boolean.TRUE.equals(currentSlotUser.getIsAiPlayer())
                // ... we are in the middle of playing an actual trick
                && (game.getPhase() == GamePhase.FIRSTTRICK || game.getPhase() == GamePhase.NORMALTRICK
                        || game.getPhase() == GamePhase.FINALTRICK)) {
            playAiTurns(game);
        }
        /// END: AI PLAYER
        return dto;
    }

    public int calculateTrickPoints(List<GameStats> trick) {
        return trick.stream()
                .mapToInt(gs -> {
                    if (gs.getSuit() == Suit.H)
                        return 1; // each heart = 1
                    if (gs.getRank().toString().equals("Q") && gs.getSuit() == Suit.S)
                        return 13; // QS = 13
                    return 0;
                })
                .sum();
    }

    /**
     * Starts the match when the host clicks on the start button
     * 
     * @param matchId The match's id
     * @param token   The token of the player sending the request
     */
    @Transactional
    public void startMatch(Long matchId, String token, Long seed) {
        User user = userRepository.findUserByToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        Match match = matchRepository.findMatchByMatchId(matchId);
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }

        Game game = createNewGameInMatch(match);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Game could not be created.");
        }
        Long savedGameId = game.getGameId();

        match.getMatchPlayers().forEach(MatchPlayer::resetMatchStats);

        if (seed != null && seed != 0) {
            game.setDeckId(ExternalApiClientService.buildSeedString(seed));
            gameRepository.save(game);
            distributeCards(match, game, seed);
        } else {
            fetchDeckAndDistributeCardsAsync(savedGameId, matchId);
        }
    }

    // fetchDeckAndDistributeCardsAsync()
    // is taking place outside the transactional method!

    public void fetchDeckAndDistributeCardsAsync(Long savedGameId, Long matchId) {
        Mono<NewDeckResponse> newDeckResponseMono = externalApiClientService.createNewDeck();

        newDeckResponseMono.subscribe(response -> {
            // Fresh DB fetch inside async
            Game savedGame = gameRepository.findById(savedGameId)
                    .orElseThrow(() -> new EntityNotFoundException("Game not found"));

            Match savedMatch = matchRepository.findMatchByMatchId(matchId);
            if (savedMatch == null) {
                throw new EntityNotFoundException("Match not found");
            }

            savedGame.setDeckId(response.getDeck_id());
            gameRepository.save(savedGame);

            distributeCards(savedMatch, savedGame, null);
        }, error -> {
            // Good practice: catch errors inside subscribe!
            log.error("Failed to fetch deck from external API", error);
        });
    }

    /**
     * Returns the next id of a game about to be started.
     * 
     * @param match The match to be started.
     * @return game The new Game instance.
     */
    public int determineNextGameNumber(Match match) {
        return match.getGames().stream()
                .mapToInt(Game::getGameNumber)
                .max()
                .orElse(0) + 1;
    }

    /**
     * Creates and persists a new Game for the given Match,
     * assigning the next available game number and updating Match status
     * accordingly.
     *
     * @param match The Match entity to which the new Game will belong.
     * @return The newly created and saved Game entity.
     */
    private Game createNewGameInMatch(Match match) {
        int nextGameNumber = determineNextGameNumber(match);

        Game game = new Game();
        game.setGameNumber(nextGameNumber);
        game.setPhase(GamePhase.PRESTART);
        game.setCurrentPlayOrder(1);

        match.addGame(game);

        gameRepository.save(game);
        gameRepository.flush();

        match.setPhase(MatchPhase.READY);
        match.setStarted(true);
        matchRepository.save(match);
        gameStatsService.initializeGameStats(match, game);
        gameStatsRepository.flush();

        return game;
    }

    private void triggerAiTurns(Match match, Game game) {
        System.out.println("Location triggerAiTurns. Triggering a first AI turn.");

        int currentSlot = game.getCurrentSlot();

        // Find the MatchPlayer for the current slot
        MatchPlayer currentPlayer = match.getMatchPlayers().stream()
                .filter(mp -> mp.getSlot() == currentSlot)
                .findFirst()
                .orElse(null);

        // If current player is an AI, trigger their turn(s)
        if (currentPlayer != null && Boolean.TRUE.equals(currentPlayer.getUser().getIsAiPlayer())) {
            System.out.println("Location: triggerAiTurns. Repeated Call.");
            playAiTurns(game);
        }
    }

    /**
     * Distributes 13 cards to each player
     */
    public void distributeCards(Match match, Game game, Long seed) {
        if (seed != null && seed % 10000 == 9247) {
            List<CardResponse> deterministicDeck = ExternalApiClientService.generateDeterministicDeck(52, seed);
            distributeFullDeckToPlayers(match, game, deterministicDeck);
            return;
        }
        Long gameId = game.getGameId();
        Long matchId = match.getMatchId();
        Mono<DrawCardResponse> drawCardResponseMono = externalApiClientService.drawCard(game.getDeckId(), 52);
        drawCardResponseMono.subscribe(response -> {
            // Manually draw fresh game object from DB.
            Game refreshedGame = gameRepository.findById(gameId)
                    .orElseThrow(() -> new EntityNotFoundException("Game not found with id: " + gameId));
            // Manually draw fresh match object from DB:
            Match refreshedMatch = matchRepository.findById(matchId)
                    .orElseThrow(() -> new EntityNotFoundException("Match not found with id: " + matchId));
            List<CardResponse> responseCards = response.getCards();
            distributeFullDeckToPlayers(refreshedMatch, refreshedGame, responseCards);
        });
    }

    private void distributeFullDeckToPlayers(Match match, Game game, List<CardResponse> responseCards) {
        if (responseCards == null || responseCards.size() != 52) {
            throw new IllegalArgumentException("Expected 52 cards to distribute.");
        }

        List<MatchPlayer> players = match.getMatchPlayers();
        if (players.size() != 4) {
            throw new IllegalStateException("Expected exactly 4 players to distribute cards.");
        }

        int cardIndex = 0;
        for (MatchPlayer matchPlayer : players) {
            StringBuilder handBuilder = new StringBuilder();

            for (int i = 0; i < 13; i++) {
                String code = responseCards.get(cardIndex).getCode();

                if (code.equals("2C")) {
                    int slot = matchPlayer.getSlot();
                    if (slot < 1 || slot > 4) {
                        throw new IllegalStateException("Invalid slot [6372]: " + slot);
                    }
                    game.setCurrentSlot(slot); // Set starting player
                }

                if (handBuilder.length() > 0) {
                    handBuilder.append(",");
                }
                handBuilder.append(code);

                cardIndex++;
            }

            matchPlayer.setHand(handBuilder.toString());
        }

        match.setPhase(MatchPhase.IN_PROGRESS);
        game.setPhase(GamePhase.PASSING);

        // Save updated game + match
        gameRepository.save(game);
        matchRepository.save(match);
        gameStatsService.updateGameStatsFromPlayers(match, game);
    }

    public void playCardAsHuman(String token, Long matchId, PlayedCardDTO dto) {
        User user = userService.getUserByToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No user found with your credential.");
        }
        System.out.println("this is user " + user.getId() + " calling playCardAsHuman");
        Match match = matchRepository.findMatchByMatchId(matchId);
        Game game = match.getActiveGame();
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No active game found for match matchId=" + matchId + ".");
        }
        System.out.println("human " + dto.getCard());

        MatchPlayer matchPlayer = matchPlayerRepository.findByUserAndMatch(user, match);
        if (matchPlayer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not in match.");
        }

        if (matchPlayer.getSlot() != game.getCurrentSlot()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "It is not your turn. You are slot " + matchPlayer.getSlot() + ", but current slot is "
                            + game.getCurrentSlot());
        }
        executeCardPlay(matchPlayer, dto.getCard());
    }

    public void playCardAsAi(MatchPlayer aiPlayer, String cardCode) {
        Match match = aiPlayer.getMatch();
        if (match == null) {
            throw new IllegalStateException("MatchPlayer " + aiPlayer.getMatchPlayerId() + " is not part of a match.");
        }

        CardUtils.requireValidCardFormat(cardCode);

        String hand = aiPlayer.getHand();
        if (hand == null || hand.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The AiPlayer of match " + match.getMatchId() + " in slot " + aiPlayer.getSlot()
                            + " has no cards in hand.");
        }

        if (!aiPlayer.hasCardCodeInHand(cardCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The AiPlayer in slot " + aiPlayer.getSlot() + " does not have the card " + cardCode + " in hand.");
        }

        Game game = match.getActiveGameOrThrow();
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Game not found for match " + match.getMatchId());
        }

        int slot = aiPlayer.getSlot();
        if (slot < 1 || slot > 4) {
            throw new IllegalStateException(
                    "The AI player " + aiPlayer.getMatchPlayerId() + " is in an invalid slot (" + slot + ").");
        }

        if (!Boolean.TRUE.equals(aiPlayer.getUser().getIsAiPlayer())) {
            throw new IllegalStateException("Slot " + slot + " is not controlled by an AI player.");
        }

        if (slot != game.getCurrentSlot()) {
            throw new IllegalStateException("AI tried to play out of turn (slot " + slot + ").");
        }

        cardRulesService.validatePlayedCard(aiPlayer, cardCode);
        executeCardPlay(aiPlayer, cardCode);
    }

    public void executeCardPlay(MatchPlayer matchPlayer, String cardCode) {
        if (matchPlayer == null) {
            throw new IllegalStateException("No MatchPlayer passed.");
        }
        CardUtils.requireValidCardFormat(cardCode);

        Match match = matchPlayer.getMatch();
        if (match == null) {
            throw new IllegalStateException("Could not find a match for matchPlayer.");
        }

        Game activeGame = match.getActiveGameOrThrow();

        // Validate move
        cardRulesService.validatePlayedCard(matchPlayer, cardCode);

        // **Delete card properly from database**
        matchPlayer.removeCardCodeFromHand(cardCode);

        // Record card play
        gameStatsService.recordCardPlay(matchPlayer, cardCode);

        System.out.println("I add cardCode " + cardCode + " to current trick");
        addCardToCurrentTrick(matchPlayer, cardCode);

        // Check if trick completed
        handlePotentialTrickCompletion(activeGame);

        // Check if game completed
        if (cardRulesService.isGameReadyForResults(activeGame)) {
            activeGame.setPhase(GamePhase.RESULT);
            gameRepository.save(activeGame);
        }
    }

    private void requireNonNullMatchPlayer(MatchPlayer matchPlayer) {
        if (matchPlayer == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Encountered empty MatchPlayer.");
        }
    }

    private Match requireMatchByMatchPlayer(MatchPlayer matchPlayer) {
        Match match = matchPlayer.getMatch();
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Could not find match for match player.");
        }
        return match;
    }

    private Game requireActiveGameByMatch(Match match) {
        Game activeGame = match.getActiveGame();
        if (activeGame == null) {
            throw new IllegalStateException("No active game found for this match.");
        }
        return activeGame;
    }

    // Method to add a card to the current trick
    private void addCardToCurrentTrick(MatchPlayer matchPlayer, String cardCode) {
        // validate matchPlayer
        requireNonNullMatchPlayer(matchPlayer);
        CardUtils.requireValidCardFormat(cardCode);

        // Get match safely from matchPlayer
        Match match = requireMatchByMatchPlayer(matchPlayer);
        // Get active game safely from matchPlayer
        Game activeGame = requireActiveGameByMatch(match);
        if (activeGame.getCurrentTrickSize() == 4) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Previous trick was not handled properly, there are already four cards in the trick, sorry.");
        } else {
            System.out.println("BANANE" + cardCode);
            activeGame.addCardToCurrentTrick(cardCode);
        }
    }

    private void handlePotentialTrickCompletion(Game game) {
        if (game.getCurrentTrickSize() == 4) {
            // Trick complete

            // Determine winner, calculate points, etc.
            int winnerSlot = cardRulesService.determineTrickWinner(game);
            int points = cardRulesService.calculateTrickPoints(game.getCurrentTrick());

            game.setLastTrickWinnerSlot(winnerSlot);
            game.setLastTrickPoints(points);

            // Update currentTrickNumber +1
            game.setCurrentTrickNumber(game.getCurrentTrickNumber() + 1);

            // Move current trick to last trick
            game.setLastTrick(game.getCurrentTrick());
            game.emptyCurrentTrick();
            game.getCurrentTrickSlots().clear();

            // Set the new trick leader
            game.setCurrentSlot(winnerSlot);

            // Save changes
            gameRepository.save(game);
        }
    }

    @Transactional
    public void passingAcceptCards(Game game, MatchPlayer matchPlayer, GamePassingDTO passingDTO) {
        cardPassingService.passingAcceptCards(game, matchPlayer, passingDTO);
    }

    @Transactional
    public void playAiTurns(Game game) {
        if (game == null) {
            System.out.println(
                    "Location: playAiTurns. Initiating an AiTurn, but the passed game argument is null.");
            return;
        }

        Match match = game.getMatch();

        MatchPlayer aiPlayer = match.getMatchPlayerBySlot(game.getCurrentSlot());

        // Stop if it's a human's turn or no AI player
        if (aiPlayer == null || !Boolean.TRUE.equals(aiPlayer.getIsAiPlayer())) {
            return;
        }

        String cardCode = aiPlayingService.selectCardToPlay(aiPlayer);
        try {
            // Thread.sleep(300 + new Random().nextInt(400)); // fancy version
            Thread.sleep(50); // short and simple
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        playCardAsAi(aiPlayer, cardCode);
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
    public void concludeGameIfFinished(Game game) {
        long cardsPlayed = gameStatsRepository.countByGameAndPlayedByGreaterThan(game, 0);
        if (cardsPlayed < 52) {
            return; // not finished
        }

        Match match = game.getMatch();
        List<MatchPlayer> matchPlayers = match.getMatchPlayers();

        // Sum points per player
        Map<Integer, Integer> pointsPerSlot = new HashMap<>();
        for (MatchPlayer player : matchPlayers) {
            int slot = player.getSlot();
            int points = gameStatsRepository.sumPointsWorthByGameAndPlayedBy(game, slot);
            pointsPerSlot.put(slot, points);
        }

        // Check if anyone shot the moon
        Integer shooterSlot = null;
        for (Map.Entry<Integer, Integer> entry : pointsPerSlot.entrySet()) {
            if (entry.getValue() == 26) {
                shooterSlot = entry.getKey();
                break;
            }
        }

        if (shooterSlot != null) {
            // Moon shooter: all others get +26, shooter gets 0
            for (MatchPlayer player : matchPlayers) {
                if (player.getSlot() == shooterSlot) {
                    player.setGameScore(player.getGameScore());
                } else {
                    player.setGameScore(player.getGameScore() + 26);
                }
                matchPlayerRepository.save(player);
            }
        } else {
            // Normal score adding
            for (MatchPlayer player : matchPlayers) {
                int points = pointsPerSlot.getOrDefault(player.getSlot(), 0);
                player.setGameScore(player.getGameScore() + points);
                matchPlayerRepository.save(player);
            }
        }

        game.setPhase(GamePhase.FINISHED);
        gameRepository.save(game);

        // Start new game
        Game newGame = new Game();
        newGame.setMatch(match);
        newGame.setGameNumber(match.getGames().size() + 1);
        newGame.setPhase(GamePhase.PASSING);
        newGame.setCurrentSlot(1);
        newGame = gameRepository.save(newGame);

        // gameStatsService.initializeGameStats(match, newGame);

        match.getGames().add(newGame);
        matchRepository.save(match);

        GameResultDTO gameResult = buildGameResult(game);
        // We will have to figure out later how to deal with this...
    }

    private GameResultDTO buildGameResult(Game game) {
        Match match = game.getMatch();
        List<MatchPlayer> matchPlayers = match.getMatchPlayers();

        List<GameResultDTO.PlayerScore> scores = new ArrayList<>();

        for (MatchPlayer player : matchPlayers) {
            GameResultDTO.PlayerScore score = new GameResultDTO.PlayerScore();
            score.setUsername(player.getUser().getUsername());
            score.setTotalScore(player.getMatchScore());

            // Calculate points this game
            int pointsThisGame = gameStatsRepository.sumPointsWorthByGameAndPlayedBy(game, player.getSlot());
            score.setPointsThisGame(pointsThisGame);

            scores.add(score);
        }

        GameResultDTO resultDTO = new GameResultDTO();
        resultDTO.setMatchId(match.getMatchId());
        resultDTO.setGameNumber(game.getGameNumber());
        resultDTO.setPlayerScores(scores);

        return resultDTO;
    }
}
