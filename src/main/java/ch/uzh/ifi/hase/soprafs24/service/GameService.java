package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.constant.Rank;
import ch.uzh.ifi.hase.soprafs24.constant.Suit;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayerCards;
import ch.uzh.ifi.hase.soprafs24.entity.PassedCard;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.model.Card;
import ch.uzh.ifi.hase.soprafs24.model.CardResponse;
import ch.uzh.ifi.hase.soprafs24.model.DrawCardResponse;
import ch.uzh.ifi.hase.soprafs24.model.NewDeckResponse;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameStatsRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerCardsRepository;
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
    private final MatchRepository matchRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final GameStatsRepository gameStatsRepository;
    private final ExternalApiClientService externalApiClientService;
    private final UserService userService;
    private final PassedCardRepository passedCardRepository;
    private final MatchPlayerCardsRepository matchPlayerCardsRepository;
    private final GameStatsService gameStatsService;

    // or via constructor injection

    @Autowired
    public GameService(
            @Qualifier("matchRepository") MatchRepository matchRepository,
            @Qualifier("userRepository") UserRepository userRepository,
            @Qualifier("matchPlayerRepository") MatchPlayerRepository matchPlayerRepository,
            @Qualifier("gameStatsRepository") GameStatsRepository gameStatsRepository,
            @Qualifier("gameRepository") GameRepository gameRepository,
            @Qualifier("matchPlayerCardsRepository") MatchPlayerCardsRepository matchPlayerCardsRepository,
            PassedCardRepository passedCardRepository,
            ExternalApiClientService externalApiClientService,
            UserService userService,
            GameStatsService gameStatsService) {
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.matchPlayerRepository = matchPlayerRepository;
        this.gameStatsRepository = gameStatsRepository;
        this.externalApiClientService = externalApiClientService;
        this.userService = userService;
        this.gameRepository = gameRepository;
        this.passedCardRepository = passedCardRepository;
        this.matchPlayerCardsRepository = matchPlayerCardsRepository;
        this.gameStatsService = gameStatsService;
    }

    private static final int EXPECTED_CARD_COUNT = 52;

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
            pointsOfPlayers.put(mp.getSlot(), mp.getScore());
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
            List<Card> playableCards = getPlayableCardsForPlayer(match, game, user, matchPlayer.getSlot());

            playableCardDTOList = playableCards.stream().map(card -> {
                PlayerCardDTO dtoCard = new PlayerCardDTO();
                dtoCard.setCard(card.getCode());
                dtoCard.setGameId(game.getGameId());
                dtoCard.setGameNumber(game.getGameNumber());
                dtoCard.setPlayerId(user.getId());
                return dtoCard;
            }).toList();
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

        /// AI PLAYER
        int currentSlot = game.getCurrentSlot();
        System.out.println("\nNext Player is " + currentSlot);
        User currentSlotUser = match.getUserBySlot(currentSlot);
        System.out.println(
                "Location: Polling. Slot: " + currentSlot + ". getId = " + currentSlotUser.getId() + " IsAiPlayer = "
                        + currentSlotUser.getIsAiPlayer() + ". game.getPhase() = " + game.getPhase() + ".");

        if (currentSlotUser != null
                && Boolean.TRUE.equals(currentSlotUser.getIsAiPlayer())
                && (game.getPhase() == GamePhase.FIRSTROUND || game.getPhase() == GamePhase.NORMALROUND
                        || game.getPhase() == GamePhase.FINALROUND)
                && requestingMatchPlayer.getSlot() == 1) { // <-- check that user polling is in slot 1
            System.out.println("Location: Polling. It is an AI player's turn: Slot " + currentSlot);
            playAiTurns(matchId);
        } else if (currentSlotUser != null
                && (game.getPhase() == GamePhase.FIRSTROUND || game.getPhase() == GamePhase.NORMALROUND
                        || game.getPhase() == GamePhase.FINALROUND)) {
            System.out.println("Location: Polling. Waiting for humanPlayer to play a card: "
                    + currentSlotUser.getUsername() + "(id="
                    + currentSlotUser.getId() + ", slot=" + currentSlot + ")");
        }

        /// END: AI PLAYER
        return dto;
    }

    /**
     * Determines the winner of the current trick.
     * 
     * @param trick List of 4 GameStats representing a complete trick.
     * @return int slot number (1–4) of the player who won the trick.
     */
    public int determineTrickWinner(List<GameStats> trick) {
        if (trick == null || trick.size() != 4) {
            throw new IllegalArgumentException("Trick must contain exactly 4 cards.");
        }

        Suit leadSuit = trick.get(0).getSuit(); // suit of the first card
        GameStats winningCard = trick.get(0); // start with first card as highest

        for (int i = 1; i < 4; i++) {
            GameStats currentCard = trick.get(i);
            if (currentCard.getSuit() == leadSuit &&
                    currentCard.getRank().ordinal() > winningCard.getRank().ordinal()) {
                winningCard = currentCard;
            }
        }

        return winningCard.getPlayedBy(); // returns slot number (1–4)
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
    public void startMatch(Long matchId, String token, Long seed) {
        User givenUser = userRepository.findUserByToken(token);
        Match givenMatch = matchRepository.findMatchByMatchId(matchId);

        if (givenUser == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        if (givenMatch == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }

        if (givenMatch.getPhase() == MatchPhase.IN_PROGRESS
                || givenMatch.getPhase() == MatchPhase.BETWEEN_GAMES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This match has already been started.");
        } else if (givenMatch.getPhase() == MatchPhase.ABORTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This match has been cancelled by the match owner.");
        } else if (givenMatch.getPhase() == MatchPhase.FINISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This match has already been finished.");
        }

        if (!givenUser.getId().equals(givenMatch.getHostId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Only the host can start the match");
        }

        Map<Integer, Long> invites = givenMatch.getInvites();
        if (invites == null) {
            invites = new HashMap<>();
        }

        Map<Long, String> joinRequests = givenMatch.getJoinRequests();
        if (joinRequests == null) {
            joinRequests = new HashMap<>();
        }

        for (Long invitedUserId : invites.values()) {
            String status = joinRequests.get(invitedUserId);
            if (!"accepted".equalsIgnoreCase(status)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot start match: not all invited users have accepted the invitation.");
            }
        }

        if (givenMatch.getPlayer1() == null || givenMatch.getPlayer2() == null ||
                givenMatch.getPlayer3() == null || givenMatch.getPlayer4() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot start match: not all player slots are filled");
        }

        // Prevent creating a new game if an active one already exists
        if (givenMatch.getActiveGame() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An active game already exists for this match.");
        }

        // Determine next game number (increment from existing games)
        int nextGameNumber = givenMatch.getGames().stream()
                .mapToInt(Game::getGameNumber)
                .max()
                .orElse(0) + 1;

        // Create new game
        Game game = new Game();
        game.setMatch(givenMatch);
        game.setGameNumber(nextGameNumber);
        game.setPhase(GamePhase.PRESTART);

        gameRepository.save(game);
        gameRepository.flush();

        Long savedGameId = game.getGameId();

        givenMatch.getGames().add(game);
        givenMatch.setPhase(MatchPhase.READY);
        givenMatch.setStarted(true);
        matchRepository.save(givenMatch);
        matchRepository.flush();

        List<MatchPlayer> matchPlayers = givenMatch.getMatchPlayers();

        for (MatchPlayer matchPlayer : matchPlayers) {
            matchPlayer.setScore(0);
            matchPlayer.setPerfectGames(0);
            matchPlayer.setShotTheMoonCount(0);
        }

        Mono<NewDeckResponse> newDeckResponseMono = externalApiClientService.createNewDeck();

        newDeckResponseMono.subscribe(response -> {
            // System.out.println("Deck id: " + response.getDeck_id());

            Game savedGame = gameRepository.findById(savedGameId)
                    .orElseThrow(() -> new EntityNotFoundException("Game not found with id: " + savedGameId));

            savedGame.setDeckId(response.getDeck_id());

            Match savedMatch = matchRepository.findMatchByMatchId(givenMatch.getMatchId());
            savedMatch.setDeckId(response.getDeck_id());

            matchRepository.save(savedMatch);
            matchRepository.flush();

            gameRepository.save(savedGame);
            gameRepository.flush();

            gameStatsService.initializeGameStats(savedMatch, savedGame);

            distributeCards(savedMatch, savedGame, seed);
        });
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
            playAiTurns(match.getMatchId());
        }
    }

    /**
     * Distributes 13 cards to each player
     */
    public void distributeCards(Match match, Game game, Long seed) {
        Mono<DrawCardResponse> drawCardResponseMono = externalApiClientService.drawCard(match.getDeckId(), 52);

        drawCardResponseMono.subscribe(response -> {
            List<CardResponse> responseCards = response.getCards();

            for (MatchPlayer matchPlayer : match.getMatchPlayers()) {
                List<MatchPlayerCards> cards = new ArrayList<>();
                int counter = 0;

                while (counter < 13 && !responseCards.isEmpty()) {
                    String code = responseCards.get(0).getCode();

                    Rank rank = Rank.fromSymbol(code.substring(0, code.length() - 1));
                    Suit suit = Suit.fromSymbol(code.substring(code.length() - 1));

                    if (code.equals("2C")) {
                        int slot = matchPlayer.getSlot();
                        if (slot < 1 || slot > 4) {
                            throw new IllegalStateException("Invalid slot [6372]: " + slot);
                        }
                        game.setCurrentSlot(slot);
                        gameRepository.save(game);
                        gameRepository.flush();
                    }

                    MatchPlayerCards matchPlayerCards = new MatchPlayerCards();
                    matchPlayerCards.setCard(code);
                    matchPlayerCards.setMatchPlayer(matchPlayer);

                    GameStats gameStats = gameStatsRepository.findByRankAndSuitAndGame(rank, suit, game);
                    if (gameStats != null) {
                        gameStats.setCardHolder(matchPlayer.getSlot());
                        gameStatsRepository.save(gameStats);
                    }

                    cards.add(matchPlayerCards);
                    counter++;
                    responseCards.remove(0);
                }

                matchPlayer.setCardsInHand(cards);
                matchPlayerRepository.save(matchPlayer);
            }
            matchPlayerRepository.flush();
            gameStatsRepository.flush();
            match.setPhase(MatchPhase.IN_PROGRESS);
            matchRepository.save(match);
            game.setPhase(GamePhase.PASSING);
            gameRepository.save(game);
            if (seed != null && seed % 10000 == 9247) {
                List<CardResponse> deterministicDeck = generateDeterministicDeck(seed);
                overwriteGameStatsWithSeed(deterministicDeck, match, game);
                dealToPlayers(deterministicDeck, match, game);
            }
        });
    }

    private void dealToPlayers(List<CardResponse> responseCards, Match match, Game game) {
        for (MatchPlayer matchPlayer : match.getMatchPlayers()) {
            List<MatchPlayerCards> cards = new ArrayList<>();
            int counter = 0;

            while (counter < 13 && !responseCards.isEmpty()) {
                String code = responseCards.get(0).getCode();

                Rank rank = Rank.fromSymbol(code.substring(0, code.length() - 1));
                Suit suit = Suit.fromSymbol(code.substring(code.length() - 1));

                if (code.equals("2C")) {
                    int slot = matchPlayer.getSlot();
                    game.setCurrentSlot(slot);
                    gameRepository.save(game);
                }

                MatchPlayerCards matchPlayerCards = new MatchPlayerCards();
                matchPlayerCards.setCard(code);
                matchPlayerCards.setMatchPlayer(matchPlayer);

                GameStats gameStats = gameStatsRepository.findByRankAndSuitAndGame(rank, suit, game);
                if (gameStats != null) {
                    gameStats.setCardHolder(matchPlayer.getSlot());
                    gameStatsRepository.save(gameStats);
                }

                cards.add(matchPlayerCards);
                counter++;
                responseCards.remove(0);
            }

            matchPlayer.setCardsInHand(cards);
            matchPlayerRepository.save(matchPlayer);
        }

        matchPlayerRepository.flush();
        gameStatsRepository.flush();
        match.setPhase(MatchPhase.IN_PROGRESS);
        matchRepository.save(match);
        game.setPhase(GamePhase.PASSING);
        gameRepository.save(game);
    }

    private List<CardResponse> generateDeterministicDeck(long seed) {
        List<CardResponse> deck = new ArrayList<>();
        String[] suits = { "C", "D", "H", "S" };
        String[] ranks = { "2", "3", "4", "5", "6", "7", "8", "9", "0", "J", "Q", "K", "A" };

        for (String suit : suits) {
            for (String rank : ranks) {
                CardResponse card = new CardResponse();
                card.setCode(rank + suit); // e.g., "2C"
                deck.add(card);
            }
        }

        Random rng = new Random(seed);
        for (int i = deck.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            CardResponse temp = deck.get(i);
            deck.set(i, deck.get(j));
            deck.set(j, temp);
        }

        return deck;
    }

    private void overwriteGameStatsWithSeed(List<CardResponse> deck, Match match, Game game) {
        List<MatchPlayer> players = match.getMatchPlayers();
        int cardsPerPlayer = 13;
        int cardIndex = 0;

        for (MatchPlayer player : players) {
            for (int i = 0; i < cardsPerPlayer; i++) {
                String code = deck.get(cardIndex).getCode();
                Card card = CardUtils.fromCode(code); // your Card class
                Rank rank = Rank.fromSymbol(card.getRank());
                Suit suit = Suit.fromSymbol(card.getSuit());
                GameStats stats = gameStatsRepository.findByRankAndSuitAndGame(rank, suit, game);

                if (stats != null) {
                    stats.setCardHolder(player.getSlot());
                    gameStatsRepository.save(stats);
                }

                if (code.equals("2C")) {
                    game.setCurrentSlot(player.getSlot());
                    gameRepository.save(game);
                }

                cardIndex++;
            }
        }
        gameStatsRepository.flush();
    }

    private boolean isGameFinished(Game game) {
        long playedCount = gameStatsRepository.countByGameAndPlayedByGreaterThan(game, 0);
        return playedCount >= EXPECTED_CARD_COUNT;
    }

    public void playCard(String token, Long matchId, PlayedCardDTO dto) {
        User user = userService.getUserByToken(token);
        Game game = getActiveGameByMatchId(matchId);
        Match match = game.getMatch();

        MatchPlayer matchPlayer = (dto.getPlayerSlot() == 0)
                ? matchPlayerRepository.findByUserAndMatch(user, match)
                : matchPlayerRepository.findByUserAndMatchAndSlot(user, match, dto.getPlayerSlot());

        if (matchPlayer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not in match.");
        }

        if (matchPlayer.getSlot() != game.getCurrentSlot()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "It is not your turn. You are slot " + matchPlayer.getSlot() + ", but current slot is "
                            + game.getCurrentSlot());
        }
        System.out.println(
                "\nHUMAN\nLocation: playCard. I received " + dto.getCard() + " from username " + user.getUsername());
        executeCardPlay(matchPlayer, match, game, dto.getCard());
    }

    public void playCardAsAi(Long matchId, int slot, String cardCode) {
        Game game = getActiveGameByMatchId(matchId);
        Match match = game.getMatch();

        MatchPlayer matchPlayer = matchPlayerRepository.findByMatchAndSlot(match, slot);

        if (matchPlayer == null || !Boolean.TRUE.equals(matchPlayer.getUser().getIsAiPlayer())) {
            throw new IllegalStateException("Slot " + slot + " is not controlled by an AI.");
        }

        if (slot != game.getCurrentSlot()) {
            throw new IllegalStateException("AI tried to play out of turn (slot " + slot + ").");
        }
        System.out.println(
                "Location: playCardAsAi. I received " + cardCode + " from userId " + matchPlayer.getUser().getId()
                        + " in slot " + slot);

        executeCardPlay(matchPlayer, match, game, cardCode);
    }

    private void executeCardPlay(MatchPlayer matchPlayer, Match match, Game game, String cardCode) {
        // Ensure card is in hand
        boolean hasCard = matchPlayer.getCardsInHand().stream()
                .anyMatch(card -> card.getCard().equals(cardCode));

        if (!hasCard) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Card " + cardCode + " not in hand.");
        }

        System.out.println(
                "Location: executeCardPlay. I received " + cardCode + " from userId " + matchPlayer.getUser().getId()
                        + " in slot " + matchPlayer.getSlot());

        boolean isFirstCardOfGame = gameStatsRepository.countByGameAndPlayedByGreaterThan(game, 0) == 0;
        boolean isFirstTrick = gameStatsRepository.countByGameAndPlayedByGreaterThan(game, 0) < 4;

        if (isFirstCardOfGame && !cardCode.equals("2C")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "First card of the game must be the 2♣, you played " + "cardCode" + ".");
        }

        if (isFirstTrick) {
            Card card = new Card();
            card.setCode(cardCode);

            if (card.getSuit().equals("H") || cardCode.equals("QS")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot play Hearts or Queen of Spades during the first trick.");
            }
        }

        // Validate play according to rules
        validateCardPlay(matchPlayer, game, cardCode);

        // Remove from hand
        matchPlayer.getCardsInHand().removeIf(card -> card.getCard().equals(cardCode));
        matchPlayerRepository.save(matchPlayer);

        // Create or update GameStats entry
        GameStats gameStats = gameStatsRepository.findByGameAndRankSuit(game, cardCode);
        if (gameStats == null) {
            gameStats = new GameStats();
            gameStats.setCardFromString(cardCode);
            gameStats.setGame(game);
            gameStats.setMatch(match);
        }

        long playedCount = gameStatsRepository.countByGameAndPlayedByGreaterThan(game, 0);
        int slot = matchPlayer.getSlot();

        gameStats.setPlayOrder((int) playedCount + 1);
        gameStats.setPlayedBy(slot);
        gameStats.setCardHolder(slot);
        gameStats.setTrickNumber((int) playedCount % 4 + 1);
        gameStatsRepository.saveAndFlush(gameStats);

        handleCardPlayAndTrickCompletion(cardCode, slot, game, match);

        if (isGameFinished(game)) {
            game.setPhase(GamePhase.FINISHED);
        }

        gameRepository.save(game);
    }

    // Method to handle card play and trick completion
    public void handleCardPlayAndTrickCompletion(String playedCardCode, int playerSlot, Game game, Match match) {
        // Add the card to the current trick and update the next player slot
        addCardToCurrentTrick(playedCardCode, game, playerSlot);

        // Now check if the trick is complete
        if (game.getCurrentTrickSize() == 4) {
            // A trick just ended, process the end of the trick
            handleTrickCompletion(game, match);
        }
    }

    // Method to add a card to the current trick
    private void addCardToCurrentTrick(String playedCardCode, Game game, int playerSlot) {
        game.addCardToCurrentTrick(playedCardCode);
        int nextSlot = (playerSlot % 4) + 1;
        game.setCurrentSlot(nextSlot);
    }

    private void handleTrickCompletion(Game game, Match match) {
        System.out.println("Location: handleTrickCompletion.");

        List<GameStats> allPlays = gameStatsRepository.findByGameAndPlayedByGreaterThan(game, 0);
        int numberOfPlayedCards = allPlays.size();

        int trickSize = numberOfPlayedCards % 4;

        if (trickSize == 0 && numberOfPlayedCards > 0) {
            List<GameStats> lastTrickGameStats = allPlays.subList(numberOfPlayedCards - 4, numberOfPlayedCards);

            // Determine the winner
            int winnerSlot = determineTrickWinner(lastTrickGameStats);
            System.out.println("Location: handleTrickCompletion, determining winnerSlot as " + winnerSlot + ".");

            // Assign points
            assignPointsToWinner(game, lastTrickGameStats, winnerSlot);

            // Update score
            updateWinnerScore(match, winnerSlot, calculateTrickPoints(lastTrickGameStats));

            // Handle game phase transitions
            updateGamePhase(game, numberOfPlayedCards);

            // Set new trick leader
            game.setCurrentSlot(winnerSlot); // ###

            // Move currentTrick into lastTrick
            List<String> finishedTrick = game.getCurrentTrick();
            game.setLastTrick(finishedTrick);
            game.setLastTrickWinnerSlot(winnerSlot);
            game.setLastTrickPoints(calculateTrickPoints(lastTrickGameStats));

            // Clear current trick
            game.clearCurrentTrick();

            // Update trick number if needed
            game.setCurrentTrickNumber(game.getCurrentTrickNumber() + 1);
            gameRepository.save(game);
        }
    }

    // Method to assign points to the winner of the trick
    // Updated method
    private void assignPointsToWinner(Game game, List<GameStats> lastTrick, int winnerSlot) {
        for (GameStats card : lastTrick) {
            card.setPointsBilledTo(winnerSlot);
        }
        gameStatsRepository.saveAll(lastTrick);

        // Now safe to call
        concludeGameIfFinished(game);
    }

    // Method to update the winner's score
    private void updateWinnerScore(Match match, int winnerSlot, int trickPoints) {
        MatchPlayer winner = match.getMatchPlayers().stream()
                .filter(mp -> mp.getSlot() == winnerSlot)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Winner slot not found in match players"));

        winner.setScore(winner.getScore() + trickPoints);
        matchPlayerRepository.save(winner); // Save score update
    }

    // Method to update the game phase based on the number of played cards
    private void updateGamePhase(Game game, int numberOfPlayedCards) {
        if (game.getPhase() == GamePhase.FIRSTROUND) {
            game.setPhase(GamePhase.NORMALROUND);
        }
        if (numberOfPlayedCards == 48) { // After all cards are played, transition to final round
            game.setPhase(GamePhase.FINALROUND);
        }
    }

    private void validateCardPlay(MatchPlayer player, Game game, String playedCardCode) {
        List<String> currentTrickCodes = game.getCurrentTrick();
        List<Card> currentTrickAsCards = currentTrickCodes.stream()
                .map(CardUtils::fromCode)
                .collect(Collectors.toList());

        boolean isFirstCardOfGame = gameStatsRepository.countByGameAndPlayedByGreaterThan(game, 0) == 0;
        boolean isFirstTrick = gameStatsRepository.countByGameAndPlayedByGreaterThan(game, 0) < 4;

        System.out.println("isFirstCardOfGame: " + (isFirstCardOfGame ? "yes" : "no") + ", isFirstTrick: "
                + (isFirstTrick ? "yes" : "no"));
        Card attemptedCard = CardUtils.fromCode(playedCardCode);
        List<Card> hand = player.getCardsInHand().stream()
                .map(c -> CardUtils.fromCode(c.getCard()))
                .toList();

        boolean isLeadingTrick = currentTrickAsCards.isEmpty();

        System.out.println("isLeadingTrick: " + (isLeadingTrick ? "yes" : "no"));

        if (isFirstCardOfGame && !playedCardCode.equals("2C")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "First card must be 2♣, you played " + playedCardCode + ".");
        }

        final String leadSuit = currentTrickAsCards.isEmpty() ? null : currentTrickAsCards.get(0).getSuit();
        boolean hasLeadSuit = (leadSuit != null) && hand.stream().anyMatch(c -> c.getSuit().equals(leadSuit));

        System.out.println("leadSuit: " + leadSuit + ", hasLeadSuit: " + hasLeadSuit);
        System.out.println("attemptedSuit: " + attemptedCard.getSuit());

        if (hasLeadSuit && !attemptedCard.getSuit().equals(leadSuit)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You must follow suit, expected " + leadSuit + ", received " + playedCardCode + ".");
        }
        boolean isHeart = attemptedCard.getSuit().equals("H");
        boolean isQueenOfSpades = attemptedCard.getCode().equals("QS");

        if (isFirstTrick && (isHeart || isQueenOfSpades)) {
            if (hasLeadSuit) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot play Hearts or Queen of Spades during the first trick unless you have no cards in the lead suit. Lead suit "
                                + leadSuit + ", you played " + playedCardCode + ".");
            }
        }

        if (isLeadingTrick && isHeart && !game.getHeartsBroken()) {
            boolean onlyHeartsLeft = hand.stream().allMatch(c -> c.getSuit().equals("H"));
            if (!onlyHeartsLeft) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "You cannot lead with Hearts (" + playedCardCode
                                + ") until they are broken, unless you only have Hearts.");
            }
        }

        checkAndBreakHearts(game, attemptedCard);

    }

    public List<GameStats> getCurrentTrick(Game game) {
        // Fetch all plays with a playOrder greater than 0, sorted by playOrder in
        // ascending order
        List<GameStats> allPlays = gameStatsRepository.findByGameAndPlayOrderGreaterThanOrderByPlayOrderAsc(game, 0);

        // Reverse the order to get the highest playOrder values first (descending
        // order)
        Collections.reverse(allPlays);

        // Calculate the trick size based on the number of cards played so far
        int trickSize = allPlays.size() % 4;

        // If trickSize is 0, it means it's the last trick and should contain 4 cards
        if (trickSize == 0 && allPlays.size() > 0) {
            trickSize = 4;
        }

        // Get the last 'trickSize' cards from allPlays (after reversing to ensure
        // highest playOrder values)
        return allPlays.subList(0, trickSize);
    }

    @Transactional
    public void makePassingHappen(Long matchId, GamePassingDTO passingDTO, String token) {
        User user = userService.getUserByToken(token);

        Long playerId = user.getId();
        List<String> cardsToPass = passingDTO.getCards();

        if (cardsToPass == null || cardsToPass.size() != 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exactly 3 cards must be passed.");
        }

        Game game = getActiveGameByMatchId(matchId);

        Match match = game.getMatch();

        if (!match.containsPlayer(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Player is not part of this match.");
        }

        int slot = match.getSlotByPlayerId(playerId);

        for (String cardCode : cardsToPass) {
            if (!isValidCardFormat(cardCode)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid card format: " + cardCode);
            }

            GameStats card = gameStatsRepository.findByRankSuitAndGameAndCardHolder(cardCode, game, slot);
            if (card == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Card " + cardCode + " is not owned by player in slot " + slot);
            }

            if (passedCardRepository.existsByGameAndFromSlotAndRankSuit(game, slot, cardCode)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Card " + cardCode + " has already been passed by slot " + slot);
            }

            if (passedCardRepository.existsByGameAndRankSuit(game, cardCode)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Card " + cardCode + " has already been passed by another player.");
            }
        }

        // Save all passed cards in a batch
        List<PassedCard> passedCards = cardsToPass.stream()
                .map(cardCode -> new PassedCard(game, cardCode, slot, game.getGameNumber()))
                .collect(Collectors.toList());
        passedCardRepository.saveAll(passedCards);

        // Count how many cards have been passed in total
        long passedCount = passedCardRepository.countByGame(game);

        // If not all 12 passed yet, check if AI players need to pass
        if (passedCount < 12) {
            int expectedHumanPasses = (int) match.getMatchPlayers().stream()
                    .filter(mp -> !Boolean.TRUE.equals(mp.getUser().getIsAiPlayer()))
                    .count() * 3;

            if (passedCount == expectedHumanPasses) {
                passForAllAiPlayers(game);
                passedCount = passedCardRepository.countByGame(game);
            }
        }

        // If all 12 cards passed, proceed to collect
        if (passedCount == 12) {
            collectPassedCards(matchId);
        }

    }

    private List<String> pickThreeCardsForAI(List<GameStats> hand) {
        // No shuffling for testing
        // Collections.shuffle(hand);
        return hand.stream()
                .limit(3)
                .map(GameStats::getRankSuit)
                .collect(Collectors.toList());
    }

    private void passForAllAiPlayers(Game game) {
        Match match = game.getMatch();

        for (int slot = 1; slot <= 4; slot++) {
            User player = match.getUserBySlot(slot);
            if (Boolean.TRUE.equals(player.getIsAiPlayer())) {
                List<GameStats> hand = gameStatsRepository.findByGameAndCardHolder(game, slot);
                List<String> cardsToPass = pickThreeCardsForAI(hand);

                for (String cardCode : cardsToPass) {
                    if (!passedCardRepository.existsByGameAndRankSuit(game, cardCode)) {
                        PassedCard passedCard = new PassedCard(game, cardCode, slot, game.getGameNumber());
                        passedCardRepository.save(passedCard);
                    }
                }
            }
        }
    }

    private boolean isValidCardFormat(String cardCode) {
        return cardCode != null && cardCode.matches("^[02-9JQKA][HDCS]$");
    }

    public void collectPassedCards(Long matchId) {
        Game game = getActiveGameByMatchId(matchId);
        List<PassedCard> passedCards = passedCardRepository.findByGame(game);
        if (passedCards.size() != 12) {
            throw new IllegalStateException("Cannot collect cards: not all cards have been passed yet.");
        }

        // Build a map from slot to cards passed
        Map<Integer, List<PassedCard>> cardsBySlot = new HashMap<>();
        for (PassedCard passed : passedCards) {
            int fromSlot = passed.getFromSlot();
            cardsBySlot.computeIfAbsent(fromSlot, k -> new ArrayList<>()).add(passed);
        }

        // Determine passing direction
        Map<Integer, Integer> passTo = determinePassingDirection(game.getGameNumber());

        // Reassign card ownership
        for (Map.Entry<Integer, List<PassedCard>> entry : cardsBySlot.entrySet()) {
            int fromSlot = entry.getKey();
            int toSlot = passTo.get(fromSlot);

            for (PassedCard card : entry.getValue()) {
                // Update ownership in GameStats
                GameStats gameStat = gameStatsRepository.findByRankSuitAndGameAndCardHolder(
                        card.getRankSuit(), game, fromSlot);

                if (gameStat != null) {
                    System.out.printf("Passing %s: %d → %d%n", card.getRankSuit(), fromSlot, toSlot);
                    gameStat.setCardHolder(toSlot);
                    gameStatsRepository.save(gameStat);
                    // Also update MatchPlayer.cardsInHand to include received cards
                    MatchPlayer receiver = game.getMatch().getMatchPlayers().stream()
                            .filter(mp -> mp.getSlot() == toSlot)
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("No MatchPlayer found for slot " + toSlot));

                    MatchPlayerCards newCard = new MatchPlayerCards();
                    newCard.setCard(card.getRankSuit());
                    newCard.setMatchPlayer(receiver);

                    if (receiver.getCardsInHand() == null) {
                        receiver.setCardsInHand(new ArrayList<>());
                    }
                    receiver.getCardsInHand().add(newCard);
                } else {
                    System.out.printf("WARN: Could not find GameStat for %s from slot %d%n", card.getRankSuit(),
                            fromSlot);
                }
            }
        }

        // Cleanup
        passedCardRepository.deleteAll(passedCards);

        // Ensure we're working with a managed Game entity
        Game managedGame = gameRepository.findById(game.getGameId())
                .orElseThrow(() -> new EntityNotFoundException("Game not found"));

        GameStats twoOfClubs = gameStatsRepository.findByRankAndSuitAndGame(Rank._2, Suit.C, managedGame);
        if (twoOfClubs != null) {
            managedGame.setCurrentSlot(twoOfClubs.getCardHolder());
            managedGame.setTrickLeaderSlot(twoOfClubs.getCardHolder());
            System.out.printf("Passed cards: 2C holder is slot %d%n", twoOfClubs.getCardHolder());
        } else {
            throw new IllegalStateException("2♣ not found after passing.");
        }

        managedGame.setPhase(GamePhase.FIRSTROUND);
        gameRepository.save(managedGame);
        gameRepository.flush();

        Match match = managedGame.getMatch();

        MatchPlayer currentMatchPlayer = matchPlayerRepository.findByMatchAndSlot(match, managedGame.getCurrentSlot());

        User user = currentMatchPlayer.getUser();

        if (user.getIsAiPlayer()) {
            System.err.println("Location: collectPassedCards. triggerAiTurns was called in collectPassedCards");
            triggerAiTurns(match, managedGame);
        }
    }

    private Map<Integer, Integer> determinePassingDirection(int gameNumber) {
        Map<Integer, Integer> direction = new HashMap<>();

        switch (gameNumber % 4) {
            case 1: // Left
                direction.put(1, 2);
                direction.put(2, 3);
                direction.put(3, 4);
                direction.put(4, 1);
                break;
            case 2: // Right
                direction.put(1, 4);
                direction.put(2, 1);
                direction.put(3, 2);
                direction.put(4, 3);
                break;
            case 3: // Across
                direction.put(1, 3);
                direction.put(2, 4);
                direction.put(3, 1);
                direction.put(4, 2);
                break;
            case 0: // No passing
            default:
                direction.put(1, 1);
                direction.put(2, 2);
                direction.put(3, 3);
                direction.put(4, 4);
                break;
        }

        return direction;
    }

    public List<Card> getPlayableCardsForPlayer(Match match, Game game, User player, int slot) {
        MatchPlayer matchPlayer = matchPlayerRepository.findByUserAndMatchAndSlot(player, match, slot);
        if (matchPlayer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not in this match.");
        }

        // Get unplayed cards in hand
        List<GameStats> handStats = gameStatsRepository.findByGameAndCardHolderAndPlayedBy(game, slot, 0);

        List<Card> hand = handStats.stream()
                .map(CardUtils::fromGameStats)
                .collect(Collectors.toList());

        List<Card> sortedHand = CardUtils.sortCardsByCardOrder(hand);

        String cardsAsString = sortedHand.stream()
                .map(Card::getCode)
                .collect(Collectors.joining(", "));
        System.out.println("Location: getPlayableCardsForPlayer "
                + (player.getIsAiPlayer() ? "ai" : "human")
                + " player [id=" + player.getId() + "], hand: " + cardsAsString);

        // Game state flags
        boolean isFirstRound = game.getGameNumber() == 1;
        boolean heartsBroken = Boolean.TRUE.equals(game.getHeartsBroken());
        boolean isFirstCardOfGame = gameStatsRepository.countByGameAndPlayedByGreaterThan(game, 0) == 0;

        // Current trick
        List<String> currentTrickCodes = game.getCurrentTrick();
        List<Card> currentTrickAsCards = currentTrickCodes.stream()
                .map(CardUtils::fromCode)
                .collect(Collectors.toList());

        boolean isLeading = currentTrickAsCards.isEmpty();

        final String trickSuitLocal = isLeading ? null : currentTrickAsCards.get(0).getSuit();

        boolean hasSuitInHand = trickSuitLocal != null &&
                sortedHand.stream().anyMatch(c -> c.getSuit().equals(trickSuitLocal));

        List<Card> playable = new ArrayList<>();

        for (Card card : sortedHand) {
            String code = card.getCode();
            String suit = card.getSuit();

            if (isFirstCardOfGame) {
                if ("2C".equals(code)) {
                    playable.add(card);
                }
                continue;
            }

            if (isFirstRound && (suit.equals("H") || code.equals("QS"))) {
                continue; // Cannot play hearts or Queen of Spades in first round unless forced
            }

            if (isLeading) {
                if (suit.equals("H") && !heartsBroken) {
                    boolean onlyHearts = sortedHand.stream().allMatch(c -> c.getSuit().equals("H"));
                    if (!onlyHearts) {
                        continue; // Cannot lead hearts if hearts not broken
                    }
                }
                playable.add(card);
            } else {
                if (hasSuitInHand) {
                    // Must follow suit
                    if (suit.equals(trickSuitLocal)) {
                        playable.add(card);
                    }
                } else {
                    // No card of lead suit -> can play anything (even Hearts/Queen)
                    playable.add(card);
                }
            }

        }

        List<Card> sortedPlayable = CardUtils.sortCardsByCardOrder(playable);

        // Debugging outputs
        String currentTrickAsString = currentTrickAsCards.stream()
                .map(Card::getCode)
                .collect(Collectors.joining(", "));
        System.out.println("Current Trick: " + currentTrickAsString);

        String sortedPlayableAsString = sortedPlayable.stream()
                .map(Card::getCode)
                .collect(Collectors.joining(", "));
        System.out.println("Playable cards for " + (player.getIsAiPlayer() ? "ai" : "human")
                + " player [id=" + player.getId() + "]: " + sortedPlayableAsString);

        return sortedPlayable;
    }

    @Transactional
    public void playAiTurns(Long matchId) {
        Game currentGame = getActiveGameByMatchId(matchId);
        if (currentGame == null) {
            System.out.println(
                    "Location: playAiTurns. Initiating an AiTurn, but not finding a Game for matchId=" + matchId + ".");
            return;
        }

        Match match = currentGame.getMatch();

        // Handle full trick before playing
        if (currentGame.getCurrentTrick().size() == 4) {
            System.out.println("Current trick is full. Handling trick completion.");
            handleTrickCompletion(currentGame, match); // PASS BOTH game and match
            currentGame = getActiveGameByMatchId(matchId); // Reload updated game state after trick is completed
            match = currentGame.getMatch();
        }

        System.out.println(
                "\nAIPLAYER\nLocation: playAiTurns. Initiating an AiTurn. GamePhase=" + currentGame.getPhase());

        if (isGameFinished(currentGame)) {
            System.out.println(
                    "Location: playAiTurns. Game is apparently finished. GamePhase=" + currentGame.getPhase() + ".");
            return;
        }

        int currentSlot = currentGame.getCurrentSlot();
        User aiPlayer = match.getUserBySlot(currentSlot);

        System.out.println("Location: playAiTurns. UserId: " + (aiPlayer != null ? aiPlayer.getId() : "null"));

        // Stop if it's a human's turn or no AI player
        if (aiPlayer == null || !Boolean.TRUE.equals(aiPlayer.getIsAiPlayer())) {
            return;
        }

        List<Card> playableCards = getPlayableCardsForPlayer(match, currentGame, aiPlayer, currentSlot);
        if (playableCards.isEmpty()) {
            return; // Something went wrong — no cards to play
        }

        // Randomization not very practical for testing
        // Collections.shuffle(playableCards);
        String cardCode = playableCards.get(0).getCode();
        System.out.println("Location: playAiTurns ready to playCardAsAi. cardCode: " + cardCode);

        // Add delay
        try {
            // Thread.sleep(300 + new Random().nextInt(400)); // fancy version
            Thread.sleep(50); // short and simple
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        playCardAsAi(matchId, currentSlot, cardCode);
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

    public long countPlayedCards(Game game) {
        return gameStatsRepository.countByGameAndPlayedByGreaterThan(game, 0);
    }

    private boolean hasOnlyHearts(List<Card> hand) {
        return hand.stream().allMatch(card -> card.getSuit().equals("H"));
    }

    private void checkAndBreakHearts(Game game, Card attemptedCard) {
        if ("H".equals(attemptedCard.getSuit()) && !game.getHeartsBroken()) {
            game.setHeartsBroken(true);
            System.out.println("Hearts are now broken due to card: " + attemptedCard.getCode());
        }
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
                    player.setScore(player.getScore());
                } else {
                    player.setScore(player.getScore() + 26);
                }
                matchPlayerRepository.save(player);
            }
        } else {
            // Normal score adding
            for (MatchPlayer player : matchPlayers) {
                int points = pointsPerSlot.getOrDefault(player.getSlot(), 0);
                player.setScore(player.getScore() + points);
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

        gameStatsService.initializeGameStats(match, newGame);

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
            score.setTotalScore(player.getScore());

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
