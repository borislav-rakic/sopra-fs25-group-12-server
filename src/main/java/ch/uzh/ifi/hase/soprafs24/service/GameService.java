package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.model.Card;
import ch.uzh.ifi.hase.soprafs24.model.CardResponse;
import ch.uzh.ifi.hase.soprafs24.model.DrawCardResponse;
import ch.uzh.ifi.hase.soprafs24.model.NewDeckResponse;
import ch.uzh.ifi.hase.soprafs24.constant.Rank;
import ch.uzh.ifi.hase.soprafs24.constant.Suit;
import ch.uzh.ifi.hase.soprafs24.repository.*;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePassingDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayedCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayerCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayerMatchInformationDTO;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import ch.uzh.ifi.hase.soprafs24.util.CardMapper;
import ch.uzh.ifi.hase.soprafs24.util.CardUtils;
import java.util.stream.Collectors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityNotFoundException;

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
    private final MatchStatsRepository matchStatsRepository;
    private final GameStatsRepository gameStatsRepository;
    private final ExternalApiClientService externalApiClientService;
    private final UserService userService;
    private final PassedCardRepository passedCardRepository;
    private final MatchPlayerCardsRepository matchPlayerCardsRepository;

    

    // or via constructor injection

    @Autowired
    public GameService(
            @Qualifier("matchRepository") MatchRepository matchRepository,
            @Qualifier("userRepository") UserRepository userRepository,
            @Qualifier("matchPlayerRepository") MatchPlayerRepository matchPlayerRepository,
            @Qualifier("matchStatsRepository") MatchStatsRepository matchStatsRepository,
            @Qualifier("gameStatsRepository") GameStatsRepository gameStatsRepository,
            @Qualifier("gameRepository") GameRepository gameRepository,
            @Qualifier("matchPlayerCardsRepository") MatchPlayerCardsRepository matchPlayerCardsRepository,
            PassedCardRepository passedCardRepository,
            ExternalApiClientService externalApiClientService,
            UserService userService) {
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.matchPlayerRepository = matchPlayerRepository;
        this.matchStatsRepository = matchStatsRepository;
        this.gameStatsRepository = gameStatsRepository;
        this.externalApiClientService = externalApiClientService;
        this.userService = userService;
        this.gameRepository = gameRepository;
        this.passedCardRepository = passedCardRepository;
        this.matchPlayerCardsRepository = matchPlayerCardsRepository;
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

        // Variable to save the MatchPlayer entry from the requesting user from the Match entity.
        MatchPlayer requestingMatchPlayer = null;

        for (MatchPlayer player : match.getMatchPlayers()) {
            if (player.getPlayerId().getId().equals(user.getId())) {
                requestingMatchPlayer = player;
                break;
            }
        }

        System.out.println(matchPlayers);

        PlayerMatchInformationDTO dto = new PlayerMatchInformationDTO();
        dto.setMatchId(match.getMatchId());
        dto.setHost(match.getHost());
        dto.setMatchPlayers(matchPlayers);
        dto.setAiPlayers(match.getAiPlayers());
        dto.setLength(match.getLength());
        dto.setGamePhase(GamePhase.PRESTART);

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
        dto.setMyTurn(match.getCurrentPlayer() == user);
        dto.setGamePhase(latestGame.getPhase());
        dto.setMatchPhase(match.getPhase());

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

        return dto;
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

        if (!givenUser.getUsername().equals(givenMatch.getHost())) {
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
            MatchStats matchStats = new MatchStats();
            matchStats.setMatch(givenMatch);
            matchStats.setPlayer(matchPlayer.getPlayerId());
            matchStats.setMalusPoints(0);
            matchStats.setPerfectGames(0);
            matchStats.setShotTheMoonCount(0);

            matchStatsRepository.save(matchStats);
            matchStatsRepository.flush();
        }

        Mono<NewDeckResponse> newDeckResponseMono = externalApiClientService.createNewDeck();

        newDeckResponseMono.subscribe(response -> {
            System.out.println("Deck id: " + response.getDeck_id());

            Game savedGame = gameRepository.findById(savedGameId)
                .orElseThrow(() -> new EntityNotFoundException("Game not found with id: " + savedGameId));
            
            savedGame.setDeckId(response.getDeck_id());

            Match savedMatch = matchRepository.findMatchByMatchId(givenMatch.getMatchId());
            savedMatch.setDeckId(response.getDeck_id());

            matchRepository.save(savedMatch);
            matchRepository.flush();

            gameRepository.save(savedGame);
            gameRepository.flush();

            initializeGameStatsNewMatch(savedMatch, savedGame);

            distributeCards(savedMatch, savedGame);
        });
    }


    /**
     * Initializes the GAME_STATS relation with the necessary information for a new
     * match.
     */
    public void initializeGameStatsNewMatch(Match match, Game game) {
        Suit[] suits = Suit.values();
        Rank[] ranks = Rank.values();

        for (Suit suit : suits) {
            for (Rank rank : ranks) {
                GameStats gameStats = new GameStats();
                gameStats.setGame(game);  // <- add this line
                gameStats.setSuit(suit);
                gameStats.setRank(rank);
                gameStats.setMatch(match);
                gameStats.setPointsBilledTo(0);
                gameStats.setCardHolder(0); // default, updated later
                gameStats.setPlayedBy(0);
                gameStats.setPlayOrder(0);
                gameStats.setPossibleHolders(0);
                gameStats.setAllowedToPlay(suit == Suit.C && rank == Rank._2);
                
                gameStatsRepository.save(gameStats);
            }
        }
        gameStatsRepository.flush();
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
                        match.setCurrentPlayer(matchPlayer.getPlayerId());
                        game.setCurrentPlayer(matchPlayer.getPlayerId());
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
        });
    }




    private User determineNextPlayer(Game game) {
        List<User> players = game.getMatch().getMatchPlayers()
                .stream()
                .map(MatchPlayer::getPlayerId)
                .toList();

        int currentIndex = players.indexOf(game.getCurrentPlayer());
        int nextIndex = (currentIndex + 1) % players.size();
        return players.get(nextIndex);
    }

    private boolean isGameFinished(Game game) {
        // Example: all cards played, one player empty hand, etc.
        return game.getPlayedCards().size() >= EXPECTED_CARD_COUNT;
    }

    public void playCard(String token, Long gameId, PlayedCardDTO playedCardDTO) {
        User player = userService.getUserByToken(token);
        Game game = getGameByGameId(gameId);

        if (!player.equals(game.getCurrentPlayer())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "It is not your turn.");
        }

        String playedCardCode = playedCardDTO.getCard();

        // Validate card exists in player's hand (matchPlayer entity)
        Match match = game.getMatch();
        MatchPlayer matchPlayer = matchPlayerRepository.findByUserAndMatch(player, match);

        boolean hasCard = matchPlayer.getCardsInHand().stream()
                .anyMatch(card -> card.getCard().equals(playedCardCode));

        if (!hasCard) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Card not in hand.");
        }

        // Remove card from hand
        matchPlayer.getCardsInHand().removeIf(card -> card.getCard().equals(playedCardCode));
        matchPlayerRepository.save(matchPlayer);

        // Log or record the card play (e.g. GameStats)
        GameStats gameStats = new GameStats();
        gameStats.setCardFromString(playedCardCode);
        gameStats.setGame(game);
        gameStats.setMatch(match);
        gameStats.setPlayedBy(matchPlayer.getSlot()); // or some player identifier
        gameStats.setPlayOrder(game.getPlayedCards().size() + 1);
        gameStats.setAllowedToPlay(false); // turn it off now that it was played

        gameStatsRepository.save(gameStats);
        game.getPlayedCards().add(gameStats);

        // Next turn
        User nextPlayer = determineNextPlayer(game);
        game.setCurrentPlayer(nextPlayer);

        if (isGameFinished(game)) {
            game.setPhase(GamePhase.FINISHED);
        }

        gameRepository.save(game);
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
            System.out.println("All cards were passed — time to collect and redistribute!");
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
            .map(CardMapper::fromGameStats)
            .collect(Collectors.toList());

        // Sort the hand
        List<Card> sortedHand = CardUtils.sortCardsByOrder(
            hand.stream().map(Card::getCode).toList()
        ).stream().map(code -> {
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
            Card leadingCard = CardMapper.fromGameStats(currentTrick.get(0));
            trickSuitLocal = leadingCard.getSuit();
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

            if (isFirstRound && (suit.equals("Hearts") || code.equals("QS"))) {
                continue;
            }

            if (isLeading) {
                if (suit.equals("Hearts") && !heartsBroken) {
                    boolean onlyHearts = sortedHand.stream().allMatch(c -> c.getSuit().equals("Hearts"));
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
                    if (isFirstRound && (suit.equals("Hearts") || code.equals("QS"))) {
                        continue;
                    }
                    playable.add(card);
                }
            }
        }

        return playable;
    }



}
