package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.model.Card;
import ch.uzh.ifi.hase.soprafs24.model.DrawCardResponse;
import ch.uzh.ifi.hase.soprafs24.model.NewDeckResponse;
import ch.uzh.ifi.hase.soprafs24.repository.*;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayerMatchInformationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Game Service
 * This class is the "worker" and responsible for all functionality related to
 * currently ongoing games, e.g. updating the player's scores, requesting information
 * from the deck of cards API, etc.
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class GameService {
    private final MatchRepository matchRepository;
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
            ExternalApiClientService externalApiClientService,
            UserService userService) {
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.matchPlayerRepository = matchPlayerRepository;
        this.matchStatsRepository = matchStatsRepository;
        this.gameStatsRepository = gameStatsRepository;
        this.externalApiClientService = externalApiClientService;
        this.userService = userService;
    }

    /**
     * Gets the necessary information for a player.
     * @param token The player's token
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
     * @param matchId The match's id
     * @param token The token of the player sending the request
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
     * Initializes the GAME_STATS relation with the necessary information for a new match.
     */
    public void initializeGameStatsNewMatch(Match match) {
        GameStats.Suit[] suits = GameStats.Suit.values();
        GameStats.Rank[] ranks = GameStats.Rank.values();

        for (GameStats.Suit suit : suits) {
            for (GameStats.Rank rank : ranks) {
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
                if (suit == GameStats.Suit.C && rank == GameStats.Rank._2) {
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
        for (MatchPlayer matchPlayer : match.getMatchPlayers()) {
            Mono<DrawCardResponse> drawCardResponseMono = externalApiClientService.drawCard(match.getDeckId(), 13);

            System.out.println("REQUESTED DRAW");

            // This code is executed when the response arrives.
            drawCardResponseMono.subscribe(response -> {
                System.out.println("DRAW RESPONSE");
                List<MatchPlayerCards> cards = new ArrayList<>();

                for (Card card : response.getCards()) {
                    String code = card.getCode();

                    MatchPlayerCards matchPlayerCards = new MatchPlayerCards();

                    // Converts the code from the deck of cards api to our implementation.
                    if (code.startsWith("0")) {
                        if (code.endsWith("H")) {
                            matchPlayerCards.setCard("10H");
                        } else if (code.endsWith("S")) {
                            matchPlayerCards.setCard("10S");
                        } else if (code.endsWith("D")) {
                            matchPlayerCards.setCard("10D");
                        } else if (code.endsWith("C")) {
                            matchPlayerCards.setCard("10C");
                        }
                    } else {
                        matchPlayerCards.setCard(code);
                    }

                    matchPlayerCards.setMatchPlayer(matchPlayer);

                    cards.add(matchPlayerCards);
                }

                matchPlayer.setCardsInHand(cards);
                matchPlayerRepository.save(matchPlayer);
                matchPlayerRepository.flush();
            });
        }
    }
}
