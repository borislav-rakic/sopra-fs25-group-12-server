package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            if (player.getPlayerId().getId().equals(user.getId())) {
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

        for (MatchPlayerCards matchPlayerCard : requestingMatchPlayer.getCardsInHand()) {
            PlayerCardDTO playerCardDTO = new PlayerCardDTO();
            playerCardDTO.setPlayerId(user.getId());
            playerCardDTO.setGameId(latestGame.getGameId());
            playerCardDTO.setGameNumber(latestGame.getGameNumber());
            playerCardDTO.setCard(matchPlayerCard.getCard());
            playerCardDTOList.add(playerCardDTO);
        }

        dto.setPlayerCards(playerCardDTOList);
        dto.setMyTurn(matchPlayer.getSlot() == match.getCurrentSlot());
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

        List<Card> playableCards = getPlayableCardsForPlayer(match, latestGame, user);

        List<PlayerCardDTO> playableCardDTOList = playableCards.stream().map(card -> {
            PlayerCardDTO dtoCard = new PlayerCardDTO();
            dtoCard.setCard(card.getCode());
            dtoCard.setGameId(latestGame.getGameId());
            dtoCard.setGameNumber(latestGame.getGameNumber());
            dtoCard.setPlayerId(user.getId());
            return dtoCard;
        }).toList();

        dto.setPlayableCards(playableCardDTOList);

        List<GameStats> allPlays = latestGame.getPlayedCards();
        int trickSize = allPlays.size() % 4;

        // Trick winner & points if just finished
        if (trickSize == 0 && allPlays.size() >= 4) {
            List<GameStats> lastTrick = allPlays.subList(allPlays.size() - 4, allPlays.size());
            int winnerSlot = determineTrickWinner(lastTrick);
            int trickPoints = calculateTrickPoints(lastTrick);
            dto.setLastTrickWinnerSlot(winnerSlot);
            dto.setLastTrickPoints(trickPoints);
            latestGame.setCurrentSlot(winnerSlot); // Next to lead
        }

        // Hearts broken?
        dto.setHeartsBroken(latestGame.getHeartsBroken());

        // Cards-in-hand counts
        Map<Integer, Integer> handCounts = new HashMap<>();
        for (MatchPlayer mp : match.getMatchPlayers()) {
            handCounts.put(mp.getSlot(), mp.getCardsInHand().size());
        }
        dto.setCardsInHandPerPlayer(handCounts);

        // Points per player
        Map<Integer, Integer> points = new HashMap<>();
        for (MatchPlayer mp : match.getMatchPlayers()) {
            points.put(mp.getSlot(), mp.getScore());
        }
        dto.setPlayerPoints(points);

        // Return a current trick only if cards have actually been played
        if ((latestGame.getPhase() == GamePhase.FIRSTROUND ||
                latestGame.getPhase() == GamePhase.NORMALROUND ||
                latestGame.getPhase() == GamePhase.FINALROUND ||
                latestGame.getPhase() == GamePhase.RESULT ||
                latestGame.getPhase() == GamePhase.FINISHED)
                && !allPlays.isEmpty()) {
            List<GameStats> currentTrickStats = allPlays.subList(
                    Math.max(0, allPlays.size() - (trickSize == 0 ? 4 : trickSize)),
                    allPlays.size());

            dto.setCurrentTrick(currentTrickStats.stream()
                    .map(CardUtils::fromGameStats)
                    .toList());

            dto.setTrickLeaderSlot(currentTrickStats.get(0).getPlayedBy());

            GameStats last = allPlays.get(allPlays.size() - 1);
            dto.setLastPlayedCard(CardUtils.fromGameStats(last));

            if (trickSize == 0 && allPlays.size() >= 4) {
                List<GameStats> lastTrick = allPlays.subList(allPlays.size() - 4, allPlays.size());
                int winnerSlot = determineTrickWinner(lastTrick);
                int trickPoints = calculateTrickPoints(lastTrick);
                dto.setLastTrickWinnerSlot(winnerSlot);
                dto.setLastTrickPoints(trickPoints);
            }
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
    public void startMatch(Long matchId, String token) {
        User givenUser = userRepository.findUserByToken(token);
        Match givenMatch = matchRepository.findMatchByMatchId(matchId);

        if (givenUser == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        if (givenMatch == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
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

        Game game = new Game();
        game.setMatch(givenMatch);
        game.setGameNumber(1);
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

            distributeCards(savedMatch, savedGame);
        });
    }

    /**
     * Distributes 13 cards to each player
     */
    public void distributeCards(Match match, Game game) {
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
                        match.setCurrentSlot(slot);
                        game.setCurrentSlot(slot);
                        matchRepository.save(match);
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
        });
    }

    private User determineNextPlayer(Game game) {
        Match match = game.getMatch();

        // Get current slot
        int currentSlot = game.getCurrentSlot();

        // Calculate next slot (1–4 in circular order)
        int nextSlot = (currentSlot % 4) + 1;

        // Look up the User assigned to that slot
        return match.getUserBySlot(nextSlot);
    }

    private boolean isGameFinished(Game game) {
        // Example: all cards played, one player empty hand, etc.
        return game.getPlayedCards().size() >= EXPECTED_CARD_COUNT;
    }

    public void playCard(String token, Long gameId, PlayedCardDTO playedCardDTO) {
        User player = userService.getUserByToken(token);
        Game game = getGameByGameId(gameId);
        Match match = game.getMatch();

        MatchPlayer matchPlayer = matchPlayerRepository.findByUserAndMatch(player, match);
        int playerSlot = matchPlayer.getSlot();

        // Validate it's their turn
        if (playerSlot != game.getCurrentSlot()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "It is not your turn.");
        }

        String playedCardCode = playedCardDTO.getCard();

        // Validate card exists in player's hand
        boolean hasCard = matchPlayer.getCardsInHand().stream()
                .anyMatch(card -> card.getCard().equals(playedCardCode));

        if (!hasCard) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Card not in hand.");
        }

        boolean isFirstCardOfGame = game.getPlayedCards().isEmpty();
        boolean isFirstRound = game.getGameNumber() == 1;

        // Enforce "2C" must be the first card played
        if (isFirstCardOfGame && !playedCardCode.equals("2C")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "First card of the game must be the 2♣.");
        }

        // Enforce no hearts or QS in first round, unless it's the 2C opening
        if (isFirstRound && !isFirstCardOfGame) {
            Card card = new Card();
            card.setCode(playedCardCode);

            boolean isHeart = card.getSuit().equals("H");
            boolean isQueenOfSpades = playedCardCode.equals("QS");

            if (isHeart || isQueenOfSpades) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot play Hearts or Queen of Spades in the first round.");
            }
        }

        // Is it okay to play this card?
        validateCardPlay(matchPlayer, game, playedCardCode);

        // Remove card from hand
        matchPlayer.getCardsInHand().removeIf(card -> card.getCard().equals(playedCardCode));
        matchPlayerRepository.save(matchPlayer);

        // Record the card play
        GameStats gameStats = new GameStats();
        gameStats.setCardFromString(playedCardCode);
        gameStats.setGame(game);
        gameStats.setMatch(match);
        gameStats.setPlayedBy(playerSlot);
        gameStats.setPlayOrder(game.getPlayedCards().size() + 1);

        gameStatsRepository.save(gameStats);
        game.getPlayedCards().add(gameStats);

        // Update turn
        int nextSlot = (playerSlot % 4) + 1;
        game.setCurrentSlot(nextSlot);

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
            playAiTurns(game);
        }
    }

    private void validateCardPlay(MatchPlayer player, Game game, String playedCardCode) {
        boolean isFirstCardOfGame = game.getPlayedCards().isEmpty();
        boolean isFirstRound = game.getGameNumber() == 1;

        Card attemptedCard = CardUtils.fromCode(playedCardCode);

        // First card of game must be 2C
        if (isFirstCardOfGame && !playedCardCode.equals("2C")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "First card must be 2♣.");
        }

        // Hearts / QS not allowed in first round (except for 2C starter)
        if (isFirstRound && !isFirstCardOfGame) {
            boolean isHeart = attemptedCard.getSuit().equals("H");
            boolean isQueenOfSpades = attemptedCard.getCode().equals("QS");

            if (isHeart || isQueenOfSpades) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot play Hearts or QS in the first round.");
            }
        }

        // Must follow suit if possible
        List<Card> hand = player.getCardsInHand().stream()
                .map(c -> CardUtils.fromCode(c.getCard()))
                .toList();

        List<GameStats> currentTrick = getCurrentTrick(game); // helper: last 0–3 cards in round
        if (!currentTrick.isEmpty()) {
            String leadSuit = currentTrick.get(0).getSuit().getSymbol(); // "H", "C", etc.
            boolean hasLeadSuit = hand.stream().anyMatch(c -> c.getSuit().equals(leadSuit));

            if (hasLeadSuit && !attemptedCard.getSuit().equals(leadSuit)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You must follow suit.");
            }
        }
    }

    private List<GameStats> getCurrentTrick(Game game) {
        List<GameStats> allPlays = game.getPlayedCards();
        int trickSize = allPlays.size() % 4;
        return allPlays.subList(Math.max(0, allPlays.size() - trickSize), allPlays.size());
    }

    public Game getGameByGameId(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new EntityNotFoundException("Game not found with id: " + gameId));
    }

    public Game getGameByMatchId(Long matchId) {
        Game game = gameRepository.findGameByMatch_MatchId(matchId);
        if (game == null) {
            throw new EntityNotFoundException("Game not found with Match ID: " + matchId);
        }
        return game;
    };

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

        Game game = getGameByMatchId(matchId);
        Match match = game.getMatch();

        if (!match.containsPlayer(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Player is not part of this match.");
        }

        int slot = match.getSlotByPlayerId(playerId);

        // Make sure the player owns each card and that it hasn't already been passed
        for (String cardCode : cardsToPass) {
            GameStats card = gameStatsRepository.findByRankSuitAndGameAndCardHolder(cardCode, game, slot);
            if (card == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Card " + cardCode + " is not owned by player.");
            }

            if (passedCardRepository.existsByGameAndRankSuit(game, cardCode)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Card " + cardCode + " has already been passed.");
            }

            // Save passed card
            PassedCard passedCard = new PassedCard();
            passedCard.setGame(game);
            passedCard.setFromPlayer(user);
            passedCard.setRankSuit(cardCode);
            passedCard.setGameNumber(game.getGameNumber());

            passedCardRepository.save(passedCard);
        }

        // Check if all 12 cards have been passed (3 per player × 4 players)
        long passedCount = passedCardRepository.countByGame(game);
        if (passedCount == 12) {
            collectPassedCards(game);
        }

        System.out.printf("Player %d in match %d passed cards: %s%n", playerId, matchId, cardsToPass);
    }

    public void collectPassedCards(Game game) {
        List<PassedCard> passedCards = passedCardRepository.findByGame(game);
        if (passedCards.size() != 12) {
            throw new IllegalStateException("Cannot collect cards: not all cards have been passed yet.");
        }

        Match match = game.getMatch();

        // Build a map from slot to cards passed
        Map<Integer, List<PassedCard>> cardsBySlot = new HashMap<>();
        for (PassedCard passed : passedCards) {
            int fromSlot = match.getSlotByPlayerId(passed.getFromPlayer().getId());
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
                    gameStat.setCardHolder(toSlot);
                    gameStatsRepository.save(gameStat);
                }
            }
        }

        // Optional: cleanup
        passedCardRepository.deleteAll(passedCards);
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

        // Sort the hand
        List<Card> sortedHand = CardUtils.sortCardsByOrder(
                hand.stream().map(Card::getCode).toList()).stream().map(code -> {
                    Card card = new Card();
                    card.setCode(code);
                    return card;
                }).toList();

        List<Card> playable = new ArrayList<>();

        boolean isFirstRound = game.getGameNumber() == 1;
        boolean heartsBroken = false; // 🔧 fallback — track this in future
        boolean isFirstCardOfGame = game.getPlayedCards().isEmpty();

        // Current trick: last 1-4 cards
        List<GameStats> currentTrick = game.getPlayedCards().stream()
                .skip(Math.max(0, game.getPlayedCards().size() - 4))
                .toList();

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

        return playable;
    }

    @Transactional
    public void playAiTurns(Game game) {
        Game currentGame = game;
        int aiTurnLimit = 4; // absolute max in 4-player game
        while (aiTurnLimit-- > 0 && !isGameFinished(currentGame)) {
            int currentSlot = currentGame.getCurrentSlot();
            User aiPlayer = currentGame.getMatch().getUserBySlot(currentSlot);

            if (aiPlayer == null || !aiPlayer.getIsAiPlayer()) {
                break; // Stop if not AI
            }

            List<Card> playableCards = getPlayableCardsForPlayer(currentGame.getMatch(), currentGame, aiPlayer);
            if (playableCards.isEmpty()) {
                break; // Fallback
            }

            PlayedCardDTO dto = new PlayedCardDTO();
            dto.setCard(playableCards.get(0).getCode());
            playCard(aiPlayer.getToken(), currentGame.getGameId(), dto);

            currentGame = getGameByGameId(currentGame.getGameId());
        }
    }

}
