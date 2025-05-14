package ch.uzh.ifi.hase.soprafs24.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameStatsRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;

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

    public void keeptThoseInjectionsUseful(Game game) {
        cardRulesService.determineTrickWinner(game);
        gameService.assignTwoOfClubsLeader(game);
    }

    @Transactional
    public void autoPlayToLastTrickOfMatch(Match match, Game game) {

        autoPlayToLastTrickOfGame(match, game, 0);

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
    public void autoPlayToMatchSummary(Match match, Game game) {

        autoPlayToLastTrickOfGame(match, game, 0);

        log.info("SIM. SimulateGameToLastTrick done");
        //// The original game is probably not need anymore
        // Game originalGame = game;

        int loopCounter = 1;
        List<MatchPlayer> matchPlayers = match.getMatchPlayers();

        while (getMaxScore(match, game) < match.getMatchGoal() - 2) {
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
    public void autoPlayToLastTrickOfMatchThree(Match match, Game game) {

        autoPlayToLastTrickOfGame(match, game, 0);

        log.info("SIM. SimulateGameToLastTrick done");
        Game originalGame = game;

        int loopCounter = 1;
        List<MatchPlayer> matchPlayers = match.getMatchPlayers();

        while (getMaxScore(match, game) < match.getMatchGoal() - 2) {
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

            if (game.getGameNumber() >= 3) {
                log.warn("Simulation reached game #3.");
                break;
            }
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
    public void autoPlayToGameSummary(Match match, Game game) {

        autoPlayToLastTrickOfGame(match, game, 0);

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
    public void autoPlayToLastTrickOfGame(Match match, Game game, Integer fakeShootingTheMoon) {
        try {

            game.setCurrentMatchPlayerSlot(0);
            gameRepository.saveAndFlush(game);
            MatchPlayer m1 = match.getMatchPlayersSortedBySlot().get(0);
            MatchPlayer m2 = match.getMatchPlayersSortedBySlot().get(1);
            MatchPlayer m3 = match.getMatchPlayersSortedBySlot().get(2);
            MatchPlayer m4 = match.getMatchPlayersSortedBySlot().get(3);

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
