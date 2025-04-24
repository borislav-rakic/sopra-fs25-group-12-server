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
        Match match = matchRepository.findMatchByMatchId(matchId);
        User user = userRepository.findUserByToken(token);

        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        MatchPlayer matchPlayer = matchPlayerRepository.findByUserAndMatch(user, match);

        if (matchPlayer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
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

        // Variable to save the MatchPlayer entry from the requesting user from the
        // Match entity.
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

        // System.out.println(matchPlayers);

        PlayerMatchInformationDTO dto = new PlayerMatchInformationDTO();

        dto.setMatchId(match.getMatchId());
        dto.setHostId(match.getHostId());
        dto.setMatchPlayers(matchPlayers);
        dto.setAiPlayers(match.getAiPlayers());
        dto.setMatchGoal(match.getMatchGoal());
        dto.setGamePhase(GamePhase.PRESTART);
        dto.setSlot(matchPlayer.getSlot());

        List<PlayerCardDTO> playerCardDTOList = new ArrayList<>();

        if (match.getGames().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No games found for this match");
        }

        Game latestGame = match.getGames().get(match.getGames().size() - 1);

        List<GameStats> hand = gameStatsRepository.findByGameAndCardHolderAndPlayedBy(latestGame, matchPlayer.getSlot(),
                0);
        for (GameStats gs : hand) {
            PlayerCardDTO dtoCard = new PlayerCardDTO();
            dtoCard.setCard(gs.getRankSuit());
            dtoCard.setGameId(latestGame.getGameId());
            dtoCard.setGameNumber(latestGame.getGameNumber());
            dtoCard.setPlayerId(user.getId());
            playerCardDTOList.add(dtoCard);
        }

        dto.setPlayerCards(playerCardDTOList);
        System.out.println("My slot: " + matchPlayer.getSlot() + ", current slot: " + latestGame.getCurrentSlot());

        dto.setMyTurn(matchPlayer.getSlot() == latestGame.getCurrentSlot());
        dto.setGamePhase(latestGame.getPhase());
        dto.setMatchPhase(match.getPhase());

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

        dto.setAvatarUrls(avatarUrls);

        List<PlayerCardDTO> playableCardDTOList = new ArrayList<>();

        // Only show playable cards if it's this player's turn
        if (matchPlayer.getSlot() == latestGame.getCurrentSlot()) {
            List<Card> playableCards = getPlayableCardsForPlayer(match, latestGame, user);

            playableCardDTOList = playableCards.stream().map(card -> {
                PlayerCardDTO dtoCard = new PlayerCardDTO();
                dtoCard.setCard(card.getCode());
                dtoCard.setGameId(latestGame.getGameId());
                dtoCard.setGameNumber(latestGame.getGameNumber());
                dtoCard.setPlayerId(user.getId());
                return dtoCard;
            }).toList();
        }

        dto.setPlayableCards(playableCardDTOList);

        // Only use cards that have actually been played
        List<GameStats> allPlays = gameStatsRepository.findByGameAndPlayedByGreaterThan(latestGame,
                0);
        int numberOfPlayedCards = allPlays.size();
        int trickSize = numberOfPlayedCards % 4;

        // Trick winner & points if just finished
        if (numberOfPlayedCards >= 4 && trickSize == 0) {
            List<GameStats> lastTrick = allPlays.subList(numberOfPlayedCards - 4, numberOfPlayedCards);
            int winnerSlot = determineTrickWinner(lastTrick);
            for (GameStats card : lastTrick) {
                card.setPointsBilledTo(winnerSlot);
            }
            gameStatsRepository.saveAll(lastTrick);

            int trickPoints = calculateTrickPoints(lastTrick);

            if (winnerSlot < 1 || winnerSlot > 4) {
                throw new IllegalStateException(
                        "Invalid slot [3849]: " + winnerSlot + "/" + trickSize + "/" + numberOfPlayedCards);
            }

            dto.setLastTrickWinnerSlot(winnerSlot);
            dto.setLastTrickPoints(trickPoints);
            if (latestGame.getPhase() == GamePhase.FIRSTROUND) {
                latestGame.setPhase(GamePhase.NORMALROUND);
            }
            // Transition to LASTROUND just before final trick
            if (numberOfPlayedCards == 48) {
                latestGame.setPhase(GamePhase.FINALROUND);
            }
            latestGame.setCurrentSlot(winnerSlot); // Next to lead

            gameRepository.save(latestGame);
        }

        // Hearts broken?
        dto.setHeartsBroken(latestGame.getHeartsBroken());

        // Cards-in-hand counts
        Map<Integer, Integer> handCounts = new HashMap<>();
        for (MatchPlayer mp : match.getMatchPlayers()) {
            int count = gameStatsRepository
                    .findByGameAndCardHolderAndPlayedBy(latestGame, mp.getSlot(), 0)
                    .size();
            handCounts.put(mp.getSlot(), count);
        }
        dto.setCardsInHandPerPlayer(handCounts);

        // Points per player
        Map<Integer, Integer> points = new HashMap<>();
        for (MatchPlayer mp : match.getMatchPlayers()) {
            points.put(mp.getSlot(), mp.getScore());
        }
        dto.setPlayerPoints(points);

        // Only return a current trick if it's in progress (1–3 cards played in this
        // trick)
        int cardsInTrick = numberOfPlayedCards % 4;
        boolean trickInProgress = cardsInTrick > 0;
        boolean trickJustFinished = cardsInTrick == 0 && numberOfPlayedCards > 0;

        dto.setTrickInProgress(trickInProgress);

        if ((latestGame.getPhase() == GamePhase.FIRSTROUND ||
                latestGame.getPhase() == GamePhase.NORMALROUND ||
                latestGame.getPhase() == GamePhase.FINALROUND ||
                latestGame.getPhase() == GamePhase.FINISHED)
                && (trickInProgress || trickJustFinished)) {

            List<GameStats> currentTrickStats;

            if (trickJustFinished) {
                // Show the last completed trick (4 cards)
                currentTrickStats = allPlays.subList(
                        allPlays.size() - 4,
                        allPlays.size());
                dto.setTrickLeaderSlot(currentTrickStats.get(0).getPlayedBy());
            } else {
                // Show the new in-progress trick (1–3 cards)
                currentTrickStats = allPlays.subList(
                        allPlays.size() - cardsInTrick,
                        allPlays.size());
                dto.setTrickLeaderSlot(currentTrickStats.get(0).getPlayedBy());
            }

            dto.setCurrentTrick(currentTrickStats.stream()
                    .map(CardUtils::fromGameStats)
                    .toList());

            // Last card played, regardless of whether trick just started or finished
            GameStats last = allPlays.get(allPlays.size() - 1);
            dto.setLastPlayedCard(CardUtils.fromGameStats(last));
        }

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
                Rank rank = Rank.fromSymbol(code.substring(0, code.length() - 1));
                Suit suit = Suit.fromSymbol(code.substring(code.length() - 1));

                GameStats stats = gameStatsRepository.findByRankAndSuitAndGame(rank, suit, game);
                if (stats != null) {
                    stats.setCardHolder(player.getSlot());
                    gameStatsRepository.save(stats);
                }

                // Optionally update current player if 2C is found
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

    public void playCard(String token, Long matchId, PlayedCardDTO playedCardDTO) {
        User player = userService.getUserByToken(token);

        // Force refresh of the Game to get latest state from DB
        Game game = gameRepository.findById(getActiveGameByMatchId(matchId).getGameId())
                .orElseThrow(() -> new EntityNotFoundException("Game not found"));

        Match match = game.getMatch();

        MatchPlayer matchPlayer = matchPlayerRepository.findByUserAndMatch(player, match);
        int playerSlot = matchPlayer.getSlot();

        // Validate it's their turn
        if (playerSlot != game.getCurrentSlot()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "It is not your turn. You are slot " + playerSlot + ", but current slot is "
                            + game.getCurrentSlot());
        }

        String playedCardCode = playedCardDTO.getCard();

        // Validate card exists in player's hand
        // System.out.println("Checking played card: '" + playedCardCode + "'");
        // System.out.println("Cards in hand:");
        for (MatchPlayerCards card : matchPlayer.getCardsInHand()) {
            // System.out.println(" - '" + card.getCard() + "'");
        }

        boolean hasCard = matchPlayer.getCardsInHand().stream()
                .anyMatch(card -> card.getCard().equals(playedCardCode));

        if (!hasCard) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Card not in hand.");
        }

        boolean isFirstCardOfGame = gameStatsRepository.countByGameAndPlayedByGreaterThan(game, 0) == 0;
        boolean isFirstTrick = gameStatsRepository.countByGameAndPlayedByGreaterThan(game, 0) < 4;

        // Enforce "2C" must be the first card played
        if (isFirstCardOfGame && !playedCardCode.equals("2C")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "First card of the game must be the 2♣.");
        }

        if (isFirstTrick) {
            Card card = new Card();
            card.setCode(playedCardCode);

            boolean isHeart = card.getSuit().equals("H");
            boolean isQueenOfSpades = playedCardCode.equals("QS");

            if (isHeart || isQueenOfSpades) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot play Hearts or Queen of Spades during the first trick.");
            }
        }

        // Is it okay to play this card?
        validateCardPlay(matchPlayer, game, playedCardCode);

        // Remove card from hand
        matchPlayer.getCardsInHand().removeIf(card -> card.getCard().equals(playedCardCode));
        matchPlayerRepository.save(matchPlayer);

        // Fetch existing GameStats if present
        GameStats gameStats = gameStatsRepository.findByGameAndRankSuit(game, playedCardCode);

        if (gameStats == null) {
            gameStats = new GameStats();
            gameStats.setCardFromString(playedCardCode);
            gameStats.setGame(game);
            gameStats.setMatch(match);
        }

        // Count how many cards have been played before this one
        long playedCount = gameStatsRepository.countByGameAndPlayedByGreaterThan(game, 0);

        // Set/update play-related fields
        gameStats.setPlayOrder((int) playedCount + 1);
        gameStats.setPlayedBy(playerSlot);
        gameStats.setCardHolder(playerSlot);

        gameStatsRepository.saveAndFlush(gameStats);
        // Update turn
        int nextSlot = (playerSlot % 4) + 1;
        game.setCurrentSlot(nextSlot);

        // Check if trick just completed
        List<GameStats> allPlays = gameStatsRepository.findByGameAndPlayedByGreaterThan(game, 0);
        int numberOfPlayedCards = allPlays.size();
        int trickSize = numberOfPlayedCards % 4;

        if (trickSize == 0 && numberOfPlayedCards > 0) {
            List<GameStats> lastTrick = allPlays.subList(numberOfPlayedCards - 4, numberOfPlayedCards);
            int winnerSlot = determineTrickWinner(lastTrick);

            // Set who gets the points for these 4 cards
            for (GameStats card : lastTrick) {
                card.setPointsBilledTo(winnerSlot);
            }
            gameStatsRepository.saveAll(lastTrick);

            int trickPoints = calculateTrickPoints(lastTrick);

            // Add points to the winning player's MatchPlayer score
            MatchPlayer winner = match.getMatchPlayers().stream()
                    .filter(mp -> mp.getSlot() == winnerSlot)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Winner slot not found in match players"));

            winner.setScore(winner.getScore() + trickPoints);
            matchPlayerRepository.save(winner); // Save score update

            // Update game phase if needed
            if (game.getPhase() == GamePhase.FIRSTROUND) {
                game.setPhase(GamePhase.NORMALROUND);
            }
            if (numberOfPlayedCards == 48) {
                game.setPhase(GamePhase.FINALROUND);
            }

            // Set next leader (winner of this trick)
            game.setCurrentSlot(winnerSlot);
        }

        // Check game end
        if (isGameFinished(game)) {
            game.setPhase(GamePhase.FINISHED);
        }

        // Check if the card played is a Heart, and if hearts weren't broken yet
        if (gameStats.getSuit() == Suit.H && !game.getHeartsBroken()) {
            game.setHeartsBroken(true);
        }
        gameRepository.save(game);

        if (!isGameFinished(game)) {
            playAiTurns(game.getMatch().getMatchId());
        }
    }

    private void validateCardPlay(MatchPlayer player, Game game, String playedCardCode) {
        boolean isFirstCardOfGame = gameStatsRepository.countByGameAndPlayedByGreaterThan(game, 0) == 0;
        boolean isFirstTrick = gameStatsRepository.countByGameAndPlayedByGreaterThan(game, 0) < 4;

        Card attemptedCard = CardUtils.fromCode(playedCardCode);
        List<Card> hand = player.getCardsInHand().stream()
                .map(c -> CardUtils.fromCode(c.getCard()))
                .toList();

        List<GameStats> currentTrick = getCurrentTrick(game);
        boolean isLeadingTrick = currentTrick.isEmpty();

        // 1. First card of the game must be 2♣
        if (isFirstCardOfGame && !playedCardCode.equals("2C")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "First card must be 2♣.");
        }

        // 2. Must follow suit if possible
        boolean hasLeadSuit = false;
        if (!currentTrick.isEmpty()) {
            String leadSuit = CardUtils.fromGameStats(currentTrick.get(0)).getSuit();
            hasLeadSuit = hand.stream().anyMatch(c -> c.getSuit().equals(leadSuit));

            if (hasLeadSuit && !attemptedCard.getSuit().equals(leadSuit)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You must follow suit.");
            }
        }

        // 3. No hearts/QS during first trick unless unable to follow suit
        boolean isHeart = attemptedCard.getSuit().equals("H");
        boolean isQueenOfSpades = attemptedCard.getCode().equals("QS");

        if (isFirstTrick && (isHeart || isQueenOfSpades)) {
            if (hasLeadSuit) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot play Hearts or Queen of Spades during the first trick unless you have no cards in the lead suit.");
            }
            // Otherwise allowed
        }

        // 4. Cannot lead with hearts unless broken or only hearts remain
        if (isLeadingTrick && isHeart && !game.getHeartsBroken()) {
            boolean onlyHeartsLeft = hand.stream().allMatch(c -> c.getSuit().equals("H"));

            if (!onlyHeartsLeft) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "You cannot lead with Hearts until they are broken, unless you only have Hearts.");
            }
        }
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
        Long playerId = passingDTO.getPlayerId();
        List<String> cardsToPass = passingDTO.getCards();

        if (cardsToPass == null || cardsToPass.size() != 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exactly 3 cards must be passed.");
        }

        User user = userService.getUserByToken(token);
        if (!user.getId().equals(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only pass cards for yourself.");
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
        Collections.shuffle(hand);
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
            System.out.printf("Passed cards: 2C holder is slot %d%n", twoOfClubs.getCardHolder());
        } else {
            throw new IllegalStateException("2♣ not found after passing.");
        }

        managedGame.setPhase(GamePhase.FIRSTROUND);
        gameRepository.save(managedGame);
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

    public List<Card> getPlayableCardsForPlayer(Match match, Game game, User player) {
        MatchPlayer matchPlayer = matchPlayerRepository.findByUserAndMatch(player, match);
        if (matchPlayer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not in this match.");
        }

        // Get player's hand from GameStats by slot
        int slot = matchPlayer.getSlot();
        List<GameStats> handStats = gameStatsRepository.findByGameAndCardHolder(game, slot);

        // Convert to Card objects
        List<Card> hand = handStats.stream()
                .map(CardUtils::fromGameStats)
                .collect(Collectors.toList());

        List<Card> sortedHand = CardUtils.sortCardsByCardOrder(hand);

        List<Card> playable = new ArrayList<>();

        boolean isFirstRound = game.getGameNumber() == 1;
        boolean heartsBroken = game.getHeartsBroken() != null && game.getHeartsBroken();
        boolean isFirstCardOfGame = gameStatsRepository.countByGameAndPlayedByGreaterThan(game, 0) == 0;

        // Current trick: last 1-4 cards
        List<GameStats> allPlays = gameStatsRepository.findByGameAndPlayOrderGreaterThanOrderByPlayOrderAsc(game, 0);
        List<GameStats> currentTrick = allPlays.subList(
                Math.max(0, allPlays.size() - (allPlays.size() % 4 == 0 ? 4 : allPlays.size() % 4)),
                allPlays.size());

        boolean isLeading = currentTrick.size() % 4 == 0 || currentTrick.isEmpty();

        final String trickSuitLocal;
        if (!isLeading && !currentTrick.isEmpty()) {
            Card leadingCard = CardUtils.fromGameStats(currentTrick.get(0));
            trickSuitLocal = leadingCard.getSuit(); // e.g. "H"
        } else {
            trickSuitLocal = null;
        }

        for (Card card : sortedHand) {
            String code = card.getCode();
            String suit = card.getSuit();

            if (isFirstCardOfGame) {
                if (code.equals("2C")) {
                    playable.add(card);
                }
                continue;
            }

            if (isFirstRound && (suit.equals("H") || code.equals("QS"))) {
                continue;
            }

            if (isLeading) {
                if (suit.equals("H") && !heartsBroken) {
                    boolean onlyHearts = sortedHand.stream().allMatch(c -> c.getSuit().equals("H"));
                    if (!onlyHearts) {
                        continue;
                    }
                }
                playable.add(card);
            } else {
                boolean hasSuit = sortedHand.stream().anyMatch(c -> c.getSuit().equals(trickSuitLocal));
                if (hasSuit) {
                    if (suit.equals(trickSuitLocal)) {
                        playable.add(card);
                    }
                } else {
                    if (isFirstRound && (suit.equals("H") || code.equals("QS"))) {
                        continue;
                    }
                    playable.add(card);
                }
            }
        }

        List<Card> sortedPlayable = CardUtils.sortCardsByCardOrder(playable);

        return sortedPlayable;
    }

    @Transactional
    public void playAiTurns(Long matchId) {
        Game currentGame = getActiveGameByMatchId(matchId);
        int aiTurnLimit = 4; // max 4 AI turns (e.g. full round)

        while (aiTurnLimit-- > 0 && !isGameFinished(currentGame)) {
            Match match = currentGame.getMatch();
            int currentSlot = currentGame.getCurrentSlot();
            User aiPlayer = match.getUserBySlot(currentSlot);

            if (aiPlayer == null || !Boolean.TRUE.equals(aiPlayer.getIsAiPlayer())) {
                break;
            }

            List<Card> playableCards = getPlayableCardsForPlayer(match, currentGame, aiPlayer);
            if (playableCards.isEmpty()) {
                break;
            }

            PlayedCardDTO dto = new PlayedCardDTO();
            dto.setCard(playableCards.get(0).getCode());

            playCard(aiPlayer.getToken(), matchId, dto);

            // Re-fetch the game to update state
            currentGame = getActiveGameByMatchId(matchId);
        }
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

}
