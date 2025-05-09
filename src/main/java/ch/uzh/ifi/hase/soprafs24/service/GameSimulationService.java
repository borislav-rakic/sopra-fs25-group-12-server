package ch.uzh.ifi.hase.soprafs24.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameStatsRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.util.CardUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import javax.transaction.Transactional;

@Service
@Qualifier("gameSimulationService")
public class GameSimulationService {
    private static final Logger log = LoggerFactory.getLogger(GameSimulationService.class);

    private final CardRulesService cardRulesService;
    private final GameService gameService;
    private final GameRepository gameRepository;
    private final GameStatsRepository gameStatsRepository;
    private final MatchPlayerRepository matchPlayerRepository;

    private final Random random = new Random();

    @Autowired
    public GameSimulationService(
            GameRepository gameRepository,
            CardRulesService cardRulesService,
            GameService gameService,
            ch.uzh.ifi.hase.soprafs24.repository.GameStatsRepository gameStatsRepository,
            MatchPlayerRepository matchPlayerRepository) {
        this.gameRepository = gameRepository;
        this.cardRulesService = cardRulesService;
        this.gameService = gameService;
        this.gameStatsRepository = gameStatsRepository;
        this.matchPlayerRepository = matchPlayerRepository;
    }

    public void autoPlayToLastTrick(Match match, Game game) {
        if (!game.getPhase().inTrick()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game is not in PLAYING phase.");
        }

        while (game.getPhase() != GamePhase.FINALTRICK) { // stop before last trick begins
            try {
                simulateNextCardPlay(match, game);
            } catch (IllegalStateException e) {
                log.warn("Card play failed: {}", e.getMessage());
                break;
            }
        }

        log.info("Auto-played up to the final trick for game {} in match {}", game.getGameId(), match.getMatchId());
    }

    @Transactional
    public void simulateGameToLastTrick(Match match, Game game) {
        if (!game.getPhase().inTrick()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game is not in PLAYING phase.");
        }

        try {
            while (game.getPhase() != GamePhase.FINALTRICK) {
                simulateNextCardPlay(match, game); // May throw unchecked exception
            }
        } catch (IllegalStateException e) {
            log.warn("Card play failed: {}", e.getMessage());
            // Rethrow to allow Spring to rollback properly
            throw e;
        }

        log.info("Auto-played up to the final trick for game {} in match {}", game.getGameId(), match.getMatchId());
    }

    @Transactional
    private void simulateNextCardPlay(Match match, Game game) {
        MatchPlayer matchPlayer = match.getMatchPlayers().stream()
                .filter(mp -> mp.getMatchPlayerSlot() == game.getCurrentMatchPlayerSlot())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Current player not found."));

        String playableCardsString = cardRulesService.getPlayableCardsForMatchPlayerPolling(game, matchPlayer);

        log.info("I am MatchPlayer {} in simulation with hand: {}.", matchPlayer.getUser().getUsername(),
                matchPlayer.getHand());
        log.info("I am MatchPlayer {} in simulation with playable hand: {}.", matchPlayer.getUser().getUsername(),
                playableCardsString);

        if (playableCardsString == null || playableCardsString.isBlank()) {
            throw new IllegalStateException(
                    "In simulation, player has no legal cards to play: " + matchPlayer.getHand());
        }

        List<String> legalCards = CardUtils.requireSplitCardCodesAsListOfStrings(playableCardsString);
        String cardCode = legalCards.get(random.nextInt(legalCards.size()));

        log.info("I am MatchPlayer {} in simulation and I decided to play {} (playOrder={}).",
                matchPlayer.getUser().getUsername(),
                cardCode,
                game.getCurrentPlayOrder() + 1);

        gameService.executeValidatedCardPlay(game, matchPlayer, cardCode);
    }

    @Transactional
    public void simulateMatchToLastTrick(Match match, Game game) {

        simulateUpToFinalTrick(match, game, 0);

        log.info("SIM. SimulateGameToLastTrick done");
        Game originalGame = game;

        int loopCounter = 1;
        List<MatchPlayer> matchPlayers = match.getMatchPlayers();

        while (getMaxScore(match, game) < match.getMatchGoal() - 10) {
            log.info("     SIM. Loop #{}", loopCounter);

            Game newGame = new Game(game);
            match.addGame(newGame);

            game.setHeartsBroken(true);
            game.setCurrentPlayOrder(52);
            game.setPhase(GamePhase.FINISHED);

            newGame.setGameNumber(game.getGameNumber() + 1);
            newGame.setMatch(match);

            List<Integer> randomScores = generateRandomScores();
            log.info("     SIM. Generated scoreString: {}", randomScores);

            game.setGameScoresList(randomScores);
            gameRepository.save(game);
            gameRepository.save(newGame);

            for (int i = 0; i < matchPlayers.size(); i++) {
                MatchPlayer tmp = matchPlayers.get(i);
                tmp.addToMatchScore(randomScores.get(i));
                matchPlayerRepository.save(tmp);
            }

            game = newGame;

            if (loopCounter++ > 15) {
                log.warn("Simulation loop exited after reaching limit.");
                break;
            }
        }

        // Move stats to latest game
        List<GameStats> statsToUpdate = gameStatsRepository.findAllByGame(originalGame);
        for (GameStats stats : statsToUpdate) {
            stats.setGame(game);
            gameStatsRepository.save(stats);
        }
    }

    @Transactional
    public void simulateUpToMatchSummary(Match match, Game game) {

        simulateUpToFinalTrick(match, game, 0);

        log.info("SIM. SimulateGameToLastTrick done");
        Game originalGame = game;

        int loopCounter = 1;
        List<MatchPlayer> matchPlayers = match.getMatchPlayers();

        while (getMaxScore(match, game) < match.getMatchGoal() - 10) {
            log.info("     SIM. Loop #{}", loopCounter);

            Game newGame = new Game(game);
            match.addGame(newGame);

            game.setHeartsBroken(true);
            game.setCurrentPlayOrder(52);
            game.setPhase(GamePhase.FINISHED);

            newGame.setGameNumber(game.getGameNumber() + 1);
            newGame.setMatch(match);

            List<Integer> randomScores = generateRandomScores();
            log.info("     SIM. Generated scoreString: {}", randomScores);

            game.setGameScoresList(randomScores);
            gameRepository.save(game);
            gameRepository.save(newGame);

            for (int i = 0; i < matchPlayers.size(); i++) {
                MatchPlayer tmp = matchPlayers.get(i);
                tmp.addToMatchScore(randomScores.get(i));
                matchPlayerRepository.save(tmp);
            }

            game = newGame;

            if (loopCounter++ > 15) {
                log.warn("Simulation loop exited after reaching limit.");
                break;
            }
        }
    }

    @Transactional
    public void simulateUpToGameSummary(Match match, Game game) {

        simulateUpToFinalTrick(match, game, 0);

        log.info("SIM. SimulateGameToLastTrick done");
        Game originalGame = game;

        int loopCounter = 1;
        List<MatchPlayer> matchPlayers = match.getMatchPlayers();

        while (getMaxScore(match, game) < match.getMatchGoal() - 10) {
            log.info("     SIM. Loop #{}", loopCounter);

            Game newGame = new Game(game);
            match.addGame(newGame);

            game.setHeartsBroken(true);
            game.setCurrentPlayOrder(52);
            game.setPhase(GamePhase.FINISHED);

            newGame.setGameNumber(game.getGameNumber() + 1);
            newGame.setMatch(match);

            List<Integer> randomScores = generateRandomScores();
            log.info("     SIM. Generated scoreString: {}", randomScores);

            game.setGameScoresList(randomScores);
            gameRepository.save(game);
            gameRepository.save(newGame);

            for (int i = 0; i < matchPlayers.size(); i++) {
                MatchPlayer tmp = matchPlayers.get(i);
                tmp.addToMatchScore(randomScores.get(i));
                matchPlayerRepository.save(tmp);
            }

            game = newGame;

            if (loopCounter++ > 15) {
                log.warn("Simulation loop exited after reaching limit.");
                break;
            }
        }

        // Move stats to latest game
        List<GameStats> statsToUpdate = gameStatsRepository.findAllByGame(originalGame);
        for (GameStats stats : statsToUpdate) {
            stats.setGame(game);
            gameStatsRepository.save(stats);
        }
    }

    public static List<Integer> generateRandomScores() {
        final int TOTAL = 26;
        Random random = new Random();

        // Choose the index that will be 0
        int zeroIndex = random.nextInt(4);

        // Distribute TOTAL among 3 parts
        int[] values = new int[4];
        int sum = TOTAL;

        // Generate 2 random breakpoints to split the total
        int first = random.nextInt(sum + 1); // 0 to 26
        int second = random.nextInt(sum - first + 1); // 0 to (26 - first)

        // Sort parts to ensure valid distribution
        int[] parts = new int[] { first, second, sum - first - second };
        Arrays.sort(parts);

        // Assign values, inserting 0 at the chosen index
        int partIndex = 0;
        for (int i = 0; i < 4; i++) {
            if (i == zeroIndex) {
                values[i] = 0;
            } else {
                values[i] = parts[partIndex++];
            }
        }

        // Convert array to List<Integer>
        List<Integer> result = new ArrayList<>();
        for (int val : values) {
            result.add(val);
        }

        return result;
    }

    private int getMaxScore(Match match, Game game) {

        List<MatchPlayer> matchPlayers = match.getMatchPlayers();

        if (matchPlayers.isEmpty()) {
            return 0;
        }

        Integer highestScore = matchPlayers.stream()
                .map(MatchPlayer::getMatchScore)
                .max(Integer::compareTo)
                .get(); // Safe now because we checked list is not empty

        return Integer.valueOf(highestScore);
    }

    @Transactional
    public void simulateUpToFinalTrick(Match match, Game game, Integer fakeShootingTheMoon) {
        try {

            game.setCurrentMatchPlayerSlot(0);
            gameRepository.saveAndFlush(game);
            MatchPlayer m1 = match.getMatchPlayers().get(0);
            MatchPlayer m2 = match.getMatchPlayers().get(1);
            MatchPlayer m3 = match.getMatchPlayers().get(2);
            MatchPlayer m4 = match.getMatchPlayers().get(3);

            List<List<String>> a = Arrays.asList(
                    Arrays.asList("5C", "6C", "7C", "8C"),
                    Arrays.asList("5C", "6D", "7C", "8D"),
                    Arrays.asList("5S", "6S", "7C", "0D"),
                    Arrays.asList("4C", "6D", "7C", "KD"),
                    Arrays.asList("5S", "6D", "7D", "8C"));
            int i = random.nextInt(a.size());
            List<String> b = a.get(i);

            List<List<Integer>> aa = Arrays.asList(
                    Arrays.asList(5, 21, 0, 0),
                    Arrays.asList(9, 9, 4, 4),
                    Arrays.asList(20, 3, 3, 0),
                    Arrays.asList(0, 6, 5, 15),
                    Arrays.asList(0, 0, 0, 26));
            int ii = random.nextInt(aa.size());
            List<Integer> bb = aa.get(ii);

            List<List<Integer>> aaa = Arrays.asList(
                    Arrays.asList(0, 0, 0, 26),
                    Arrays.asList(0, 0, 26, 0),
                    Arrays.asList(0, 26, 0, 0),
                    Arrays.asList(26, 0, 0, 0));
            int iii = random.nextInt(aaa.size());
            List<Integer> bbb = aaa.get(iii);

            if (fakeShootingTheMoon > 0) {
                bb = bbb;
            }

            m1.setHand(b.get(0));
            m1.setGameScore(bb.get(0));

            m2.setHand(b.get(1));
            m2.setGameScore(bb.get(1));

            m3.setHand(b.get(2));
            m3.setGameScore(bb.get(2));

            m4.setHand(b.get(3));
            m4.setGameScore(bb.get(3));

            matchPlayerRepository.save(m1);
            matchPlayerRepository.save(m2);
            matchPlayerRepository.save(m3);
            matchPlayerRepository.save(m4);

            game.setCurrentTrick(new ArrayList<>());
            game.setHeartsBroken(true);
            game.setCurrentMatchPlayerSlot(1);
            game.setTrickLeaderMatchPlayerSlot(1);
            game.setCurrentPlayOrder(48);
            game.setCurrentTrickNumber(13);
            game.setPhase(GamePhase.FINALTRICK);
            gameRepository.saveAndFlush(game);

            log.info("Simulation stopped at FINALTRICK for game {}", game.getGameId());
        } catch (Exception e) {
            log.error("Exception during simulation: ", e); // Logs full stack trace
            throw e; // rethrow if needed
        }
    }

    public boolean isTrickComplete(Game game) {
        return game.getCurrentTrick().size() == 4;
    }

}
