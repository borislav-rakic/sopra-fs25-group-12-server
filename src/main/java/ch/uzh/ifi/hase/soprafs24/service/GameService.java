package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.model.Card;
import ch.uzh.ifi.hase.soprafs24.model.DrawCardResponse;
import ch.uzh.ifi.hase.soprafs24.model.NewDeckResponse;
import ch.uzh.ifi.hase.soprafs24.constant.Rank;
import ch.uzh.ifi.hase.soprafs24.constant.Suit;
import ch.uzh.ifi.hase.soprafs24.repository.*;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayedCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayerMatchInformationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

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

    @Autowired
    public GameService(
            @Qualifier("matchRepository") MatchRepository matchRepository,
            @Qualifier("userRepository") UserRepository userRepository,
            @Qualifier("matchPlayerRepository") MatchPlayerRepository matchPlayerRepository,
            @Qualifier("matchStatsRepository") MatchStatsRepository matchStatsRepository,
            @Qualifier("gameStatsRepository") GameStatsRepository gameStatsRepository,
            @Qualifier("gameRepository") GameRepository gameRepository,
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

        MatchPlayer matchPlayer = matchPlayerRepository.findMatchPlayerByUser(user);

        if (matchPlayer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }

        List<String> matchPlayers = new ArrayList<>();
        matchPlayers.add(null);
        matchPlayers.add(null);
        matchPlayers.add(null);
        matchPlayers.add(null);

        List<MatchPlayer> matchPlayerList = match.getMatchPlayers();

        List<User> usersInMatch = new ArrayList<>();
        usersInMatch.add(match.getPlayer1());
        usersInMatch.add(match.getPlayer2());
        usersInMatch.add(match.getPlayer3());
        usersInMatch.add(match.getPlayer4());

        for (MatchPlayer player : matchPlayerList) {
            User matchPlayerUser = player.getPlayerId();

            int slot = 0;

            for (User userInMatch : usersInMatch) {
                if (userInMatch == matchPlayerUser) {
                    break;
                }
                slot++;
            }

            matchPlayers.set(slot, matchPlayerUser.getUsername());
        }

        System.out.println(matchPlayers);

        PlayerMatchInformationDTO dto = new PlayerMatchInformationDTO();
        dto.setMatchId(match.getMatchId());
        dto.setHost(match.getHost());
        dto.setMatchPlayers(matchPlayers);
        dto.setAiPlayers(match.getAiPlayers());
        dto.setLength(match.getLength());
        dto.setStarted(true);

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

        // Get invites (human players)
        Map<Integer, Long> invites = givenMatch.getInvites();
        if (invites == null) {
            invites = new HashMap<>();
        }

        // Get accepted invites
        Map<Long, String> joinRequests = givenMatch.getJoinRequests();
        if (joinRequests == null) {
            joinRequests = new HashMap<>();
        }

        // 1. Ensure all invited users accepted
        for (Long invitedUserId : invites.values()) {
            String status = joinRequests.get(invitedUserId);
            if (!"accepted".equalsIgnoreCase(status)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot start match: not all invited users have accepted the invitation.");
            }
        }

        // 2. Ensure remaining slots are filled with AI players
        // int totalSlots = 4;
        // int filledHumanSlots = invites.size(); // accepted humans
        // int filledAiSlots = givenMatch.getAiPlayers() != null ?
        // givenMatch.getAiPlayers().size() : 0;
        //
        // if ((filledHumanSlots + filledAiSlots) < totalSlots) {
        // throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
        // "Cannot start match: not all player slots are filled.");
        // }
        if (givenMatch.getPlayer1() == null || givenMatch.getPlayer2() == null || givenMatch.getPlayer3() == null
                || givenMatch.getPlayer4() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot start match: not all player slots are filled");
        }

        givenMatch.setStarted(true);
        matchRepository.save(givenMatch);
        matchRepository.flush();

        List<MatchPlayer> matchPlayers = givenMatch.getMatchPlayers();

        // Initializes the MATCH_STATS relation
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

        // Is executed, when the response from the deck of cards API arrives
        newDeckResponseMono.subscribe(response -> {
            System.out.println("Deck id: " + response.getDeck_id());
            givenMatch.setDeckId(response.getDeck_id());
            matchRepository.save(givenMatch);
            matchRepository.flush();

            initializeGameStatsNewMatch(givenMatch);

            distributeCards(givenMatch);
        });
    }

    /**
     * Initializes the GAME_STATS relation with the necessary information for a new
     * match.
     */
    public void initializeGameStatsNewMatch(Match match) {
        Suit[] suits = Suit.values();
        Rank[] ranks = Rank.values();

        for (Suit suit : suits) {
            for (Rank rank : ranks) {
                GameStats gameStats = new GameStats();
                gameStats.setSuit(suit);
                gameStats.setRank(rank);
                gameStats.setMatch(match);
                gameStats.setPointsBilledTo(0);
                gameStats.setCardHolder(0);
                gameStats.setPlayedBy(0);
                gameStats.setPlayOrder(0);
                gameStats.setPossibleHolders(0);

                gameStats.setAllowedToPlay(false);

                // If the card is the two of clubs, it is allowed to be played.
                if (suit == Suit.C && rank == Rank._2) {
                    gameStats.setAllowedToPlay(true);
                }

                gameStatsRepository.save(gameStats);
                gameStatsRepository.flush();
            }
        }
    }

    /**
     * Distributes 13 cards to each player
     */
    public void distributeCards(Match match) {
        Mono<DrawCardResponse> drawCardResponseMono = externalApiClientService.drawCard(match.getDeckId(), 52);

        System.out.println("REQUESTED DRAW");

        // This code is executed when the response arrives.
        drawCardResponseMono.subscribe(response -> {
            System.out.println("DRAW RESPONSE");

            List<Card> responseCards = response.getCards();

            for (MatchPlayer matchPlayer : match.getMatchPlayers()) {
                List<MatchPlayerCards> cards = new ArrayList<>();

                int counter = 0;

                while (counter < 13 && !responseCards.isEmpty()) {
                    String code = responseCards.get(0).getCode();

                    MatchPlayerCards matchPlayerCards = new MatchPlayerCards();

                    matchPlayerCards.setCard(code);
                    GameStats gameStats = gameStatsRepository.findByRankSuit(code);
                    gameStats.setCardHolder(matchPlayer.getSlot());
                    gameStatsRepository.save(gameStats);

                    matchPlayerCards.setMatchPlayer(matchPlayer);

                    cards.add(matchPlayerCards);

                    counter++;
                    responseCards.remove(0);
                }

                matchPlayer.setCardsInHand(cards);
                matchPlayerRepository.save(matchPlayer);
                matchPlayerRepository.flush();
            }
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
        MatchPlayer matchPlayer = matchPlayerRepository.findMatchPlayerByUser(player);

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
            game.setFinished(true);
        }

        gameRepository.save(game);
    }

    public Game getGameByGameId(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new EntityNotFoundException("Game not found with id: " + gameId));
    }
}
