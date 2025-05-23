package ch.uzh.ifi.hase.soprafs24.service;

import java.util.List;
import java.util.Random;

import javax.persistence.EntityNotFoundException;

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
import ch.uzh.ifi.hase.soprafs24.logic.GameEnforcer;
import ch.uzh.ifi.hase.soprafs24.model.CardResponse;
import ch.uzh.ifi.hase.soprafs24.model.DrawCardResponse;
import ch.uzh.ifi.hase.soprafs24.model.NewDeckResponse;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import ch.uzh.ifi.hase.soprafs24.util.CardUtils;
import ch.uzh.ifi.hase.soprafs24.util.MatchUtils;
import reactor.core.publisher.Mono;

@Service
public class GameSetupService {
    private final Logger log = LoggerFactory.getLogger(GameSetupService.class);

    private final ExternalApiClientService externalApiClientService;
    private final GameStatsService gameStatsService;
    private final MatchPlayerRepository matchPlayerRepository;

    @Autowired
    public GameSetupService(
            @Qualifier("externalApiClientService") ExternalApiClientService externalApiClientService,
            @Qualifier("gameStatsService") GameStatsService gameStatsService,
            @Qualifier("matchPlayerRepository") MatchPlayerRepository matchPlayerRepository) {
        this.externalApiClientService = externalApiClientService;
        this.gameStatsService = gameStatsService;
        this.matchPlayerRepository = matchPlayerRepository;

    }

    /**
     * Returns the next id of a game about to be started.
     * 
     * @param match The match to be started.
     * @return game The new Game instance.
     */
    public int determineNextGameNumber(Match match) {
        log.info("  🦑 GameSetupService determineNextGameNumber.");
        return match.getGames().stream()
                .mapToInt(Game::getGameNumber)
                .max()
                .orElse(0) + 1;
    }

    /**
     * Creates and starts a new game for a given match.
     * 
     * @param match           the match to create the game for
     * @param matchRepository used to save the match
     * @param seed            optional number to create a predictable card order
     * @return the new Game object
     * @throws ResponseStatusException if the match is in the wrong phase or game
     *                                 creation fails
     **/
    public Game createAndStartGameForMatch(Match match, MatchRepository matchRepository, GameRepository gameRepository,
            Long seed) {

        if (match.getPhase() != MatchPhase.BEFORE_GAMES && match.getPhase() != MatchPhase.BETWEEN_GAMES) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    String.format("Game cannot be created if match is in phase %s.", match.getPhase()));
        }

        // Enforce that there is no other active game
        GameEnforcer.assertNoActiveGames(match);

        Game game = createNewGameInMatch(match, matchRepository, gameRepository);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Game could not be created.");
        }

        game.setPhase(GamePhase.WAITING_FOR_EXTERNAL_API);
        // log.info("💄 🦑 GameSetupService: GamePhase is set to
        // WAITING_FOR_EXTERNAL_API.");
        gameRepository.save(game);
        gameRepository.flush(); // immediate DB write necessary, else the asynch might not find the game again.

        if (seed != null && seed != 0) {
            // log.info(" 🦑 GameSetupService: Cards are determined internally.");
            game.setDeckId(ExternalApiClientService.buildSeedString(seed));
            distributeCards(match, game, matchRepository, gameRepository, seed);
        } else {
            // log.info(" 🦑 GameSetupService: Cards are fetched from remote API.");
            fetchDeckAndDistributeCardsAsync(matchRepository, gameRepository, match.getMatchId());
        }

        return game;
    }

    /**
     * Asynchronously fetches a new deck from the external API and distributes
     * cards.
     * 
     * @param matchRepository used to retrieve and save the match
     * @param matchId         the ID of the match to fetch the deck for
     *                        This method is non-blocking and will execute the
     *                        distribution when the API responds
     *                        If the match is not found or multiple games are in the
     *                        wrong phase, it throws an error
     **/
    public void fetchDeckAndDistributeCardsAsync(MatchRepository matchRepository, GameRepository gameRepository,
            Long matchId) {
        // log.info(" 🦑 GameSetupService fetchDeckAndDistributeCardsAsync");
        Mono<NewDeckResponse> newDeckResponseMono = externalApiClientService.createNewDeck();

        newDeckResponseMono.subscribe(response -> {
            Match match = matchRepository.findMatchByMatchId(matchId);
            if (match == null) {
                throw new EntityNotFoundException("Match not found");
            }
            List<Game> games = gameRepository.findWaitingGameByMatchid(matchId);
            if (games.size() != 1) {
                throw new IllegalStateException(
                        "Expected one game in WAITING_FOR_EXTERNAL_API, found: " + games.size());
            }

            Game game = games.get(0);
            // log.info(" 🦑 GameSetupService: Deck ID was set to {}.",
            // response.getDeck_id());
            game.setDeckId(response.getDeck_id());
            gameRepository.save(game);
            matchRepository.save(match);

            distributeCards(match, game, matchRepository, gameRepository, null);
        }, error -> {
            log.error("Failed to fetch deck from external API", error);

            Match match = matchRepository.findMatchByMatchId(matchId);
            if (match == null) {
                throw new EntityNotFoundException("Match not found");
            }
            List<Game> games = gameRepository.findWaitingGameByMatchid(matchId);
            if (games.size() != 1) {
                throw new IllegalStateException(
                        "Expected one game in WAITING_FOR_EXTERNAL_API, found: " + games.size());
            }

            // log.info(" 🦑 GameSetupService: Cards are determined internally.");

            Game game = games.get(0);

            Random random = new Random();
            long randomValue = Math.abs(random.nextInt());

            long randomSeed = randomValue * 10000L + 9247L;

            game.setDeckId(ExternalApiClientService.buildSeedString(randomSeed));
            distributeCards(match, game, matchRepository, gameRepository, randomSeed);
        });
    }

    /**
     * Distributes 13 cards to each player in the match
     * 
     * @param match           the match to distribute cards for
     * @param game            the game to distribute cards in
     * @param matchRepository used to refresh and save the match
     * @param seed            optional seed for deterministic deck generation
     *                        If a seed ending in 9247 is given, a deterministic
     *                        deck is used
     *                        Otherwise, draws a full deck from the external API
     *                        asynchronously
     **/

    public void distributeCards(Match match, Game game, MatchRepository matchRepository, GameRepository gameRepository,
            Long seed) {
        // log.info(" 🦑 GameSetupService distributeCards (seed=`{}´)", seed);
        if (seed != null && seed % 10000 == 9247) {
            List<CardResponse> deterministicDeck = ExternalApiClientService
                    .generateDeterministicDeck(GameConstants.FULL_DECK_CARD_COUNT, seed);
            // log.info(" 🦑 GameSetupService distributeCards. Drew Cards internally");
            distributeFullDeckToPlayers(match, game, matchRepository, gameRepository, deterministicDeck);
            return;
        }
        Long gameId = game.getGameId();
        Long matchId = match.getMatchId();

        Mono<DrawCardResponse> drawCardResponseMono = externalApiClientService.drawCard(game.getDeckId(),
                GameConstants.FULL_DECK_CARD_COUNT);
        drawCardResponseMono.subscribe(response -> {
            if (!CardUtils.validateDrawnCards(response.getCards())) {
                throw new IllegalStateException("Invalid cards in deck");
            }

            // Manually draw fresh game object from DB.
            Game refreshedGame = gameRepository.findById(gameId)
                    .orElseThrow(() -> new EntityNotFoundException("Game not found with id: " + gameId));
            // log.info(" 🦑 GameSetupService: Drew refreshedGame from gameRepository inside
            // subscribe block.");
            // Manually draw fresh match object from DB:
            Match refreshedMatch = matchRepository.findById(matchId)
                    .orElseThrow(() -> new EntityNotFoundException("Match not found with id: " + matchId));
            List<CardResponse> responseCards = response.getCards();
            // log.info(" 🦑 GameSetupService: About to distributeFullDeckToPlayers");
            distributeFullDeckToPlayers(refreshedMatch, refreshedGame, matchRepository, gameRepository, responseCards);
        }, error -> {
            log.error("Failed to fetch deck from external API", error);

            Match refreshedMatch = matchRepository.findMatchByMatchId(matchId);
            if (refreshedMatch == null) {
                throw new EntityNotFoundException("Match not found");
            }
            List<Game> games = gameRepository.findWaitingGameByMatchid(matchId);
            if (games.size() != 1) {
                throw new IllegalStateException(
                        "Expected one game in WAITING_FOR_EXTERNAL_API, found: " + games.size());
            }

            // log.info(" 🦑 GameSetupService: Cards are determined internally.");

            // Game refreshedGame = games.get(0);

            Random random = new Random();
            long randomValue = Math.abs(random.nextInt());

            long randomSeed = randomValue * 10000L + 9247L;

            game.setDeckId(ExternalApiClientService.buildSeedString(randomSeed));
            distributeCards(match, game, matchRepository, gameRepository, randomSeed);
        });
    }

    /**
     * Assigns 13 cards to each of the 4 players and updates game and match phases
     * 
     * @param match           the match to update
     * @param game            the game to update
     * @param matchRepository used to save the match
     * @param responseCards   the list of 52 card objects to distribute
     *                        Throws if number of cards is not 52 or if player count
     *                        is not 4
     **/
    private void distributeFullDeckToPlayers(Match match, Game game, MatchRepository matchRepository,
            GameRepository gameRepository,
            List<CardResponse> responseCards) {
        if (responseCards == null || responseCards.size() != GameConstants.FULL_DECK_CARD_COUNT) {
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

                // Placement of 2C is irrelevant before passing.

                if (handBuilder.length() > 0) {
                    handBuilder.append(",");
                }
                handBuilder.append(code);

                cardIndex++;
            }
            String sortedHand = CardUtils.normalizeCardCodeString(handBuilder.toString());
            matchPlayer.setHand(sortedHand);
            log.info("  🦑 GameSetupService: Dealing hand [{}] to MatchPlayer.id={} in slot={}.",
                    matchPlayer.getHand(),
                    matchPlayer.getUser().getId(),
                    matchPlayer.getMatchPlayerSlot());
            matchPlayerRepository.saveAndFlush(matchPlayer);

        }

        match.setPhase(MatchPhase.IN_PROGRESS);
        // log.info("💄 MatchPhase is set to IN_PROGRESS");

        int currentGameNumber = game.getGameNumber(); // or however you track it

        // already assign the leading position to the MatchPlayer holding 2 of Clubs.

        if (currentGameNumber % 4 == 0) {
            // Skip passing and jump directly into the first trick.
            game.setPhase(GamePhase.SKIP_PASSING);
            MatchUtils.resetReadyStateForHumanPlayers(match, matchPlayerRepository);
        } else {
            game.setPhase(GamePhase.PASSING);
            log.info("💄 GamePhase is set to PASSING");
            log.info("  🦑 GameSetupService: °°° PASSING COMMENCES °°°");
        }

        // Save updated game + match
        gameRepository.save(game);
        matchRepository.save(match);
        gameStatsService.updateGameStatsFromPlayers(match, game);
    }

    /**
     * Creates and persists a new Game for the given Match,
     * assigning the next available game number and updating Match status
     * accordingly.
     *
     * @param match The Match entity to which the new Game will belong.
     * @return The newly created and saved Game entity.
     */
    private Game createNewGameInMatch(Match match, MatchRepository matchRepository, GameRepository gameRepository) {
        int nextGameNumber = determineNextGameNumber(match);
        // log.info(" 🦑 GameSetupService createNewGameInMatch nextGameNumber={}.",
        // nextGameNumber);

        Game game = new Game();
        game.setGameNumber(nextGameNumber);
        game.setPhase(GamePhase.PRESTART);
        game.setCurrentPlayOrder(0);
        game.setCurrentTrickNumber(1);

        match.addGame(game);

        gameRepository.saveAndFlush(game);

        match.setStarted(true);
        matchRepository.save(match);

        gameStatsService.initializeGameStats(match, game);

        log.info("  🦑 GameSetupService: Created new game {} (gamePhase={}) for match {} (matchPhase={}).",
                game.getGameId(), game.getPhase(), match.getMatchId(), match.getPhase());

        return game;
    }

    // Careful: this is an exact copy of a similar method in CardPassingService.
    public void assignTwoOfClubsLeader(Game game) {
        Match match = game.getMatch();
        String hand = "";
        for (MatchPlayer player : match.getMatchPlayers()) {
            hand = player.getHand();
            if (CardUtils.isCardCodeInHand(hand, GameConstants.TWO_OF_CLUBS)) {
                int slot = player.getMatchPlayerSlot();
                game.setCurrentMatchPlayerSlot(slot);
                game.setTrickLeaderMatchPlayerSlot(slot);
                log.info("TrickLeaderMatchPlayerSlot was set to {}.", slot);
                return;
            }
        }

        throw new IllegalStateException("No player has the 2♣ — invalid game state.");
    }
}
