package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameConstants;
import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.TrickPhase;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.repository.GameStatsRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.TrickDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.TrickDTO.TrickCard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

@Service
public class GameTrickService {

    private static final Logger log = LoggerFactory.getLogger(GameTrickService.class);

    private final MatchPlayerRepository matchPlayerRepository;
    private final CardRulesService cardRulesService;
    private final GameStatsRepository gameStatsRepository;

    @Autowired
    public GameTrickService(
            MatchPlayerRepository matchPlayerRepository,
            CardRulesService cardRulesService,
            GameStatsRepository gameStatsRepository) {
        this.matchPlayerRepository = matchPlayerRepository;
        this.cardRulesService = cardRulesService;
        this.gameStatsRepository = gameStatsRepository;
    }

    public void addCardToTrick(Match match, Game game, MatchPlayer matchPlayer, String cardCode) {
        game.addCardCodeToCurrentTrick(cardCode);
        game.setCurrentPlayOrder(game.getCurrentPlayOrder() + 1);
        log.info("addCardToTrick, cardCode={}, new playOrder={}, gamePhase (before updating) is {}.",
                cardCode,
                game.getCurrentPlayOrder(),
                game.getPhase());
    }

    public void updateGamePhaseBasedOnPlayOrder(Game game) {
        int playOrder = game.getCurrentPlayOrder();
        GamePhase currentPhase = game.getPhase();
        GamePhase newPhase;

        if (playOrder == 0) {
            newPhase = GamePhase.FIRSTTRICK;
            game.setTrickPhase(TrickPhase.READYFORFIRSTCARD);
        } else if (playOrder <= 4) {
            newPhase = GamePhase.FIRSTTRICK;
        } else if (playOrder <= 48) {
            newPhase = GamePhase.NORMALTRICK;
        } else if (playOrder <= GameConstants.FULL_DECK_CARD_COUNT) {
            newPhase = GamePhase.FINALTRICK;
        } else {
            newPhase = GamePhase.RESULT;
        }

        if (newPhase != currentPhase) {
            game.setPhase(newPhase);
            log.info("💄 GamePhase transitioned from {} to {} (playOrder = {}).", currentPhase, newPhase, playOrder);
        } else {
            log.info("💄 No GamePhase transition in GamePhase={} (playOrder = {}).", currentPhase, playOrder);
        }
    }

    public void determineWinnerOfTrick(Match match, Game game) {
        int winnerSlot = cardRulesService.determineTrickWinner(game);
        game.setPreviousTrickWinnerMatchPlayerSlot(winnerSlot);
    }

    public void determinePointsOfTrick(Match match, Game game) {
        int winnerSlot = game.getPreviousTrickWinnerMatchPlayerSlot();
        int points = cardRulesService.calculateTrickPoints(game, winnerSlot);
        game.setPreviousTrickPoints(points);

        MatchPlayer winner = matchPlayerRepository.findByMatchAndMatchPlayerSlot(match, winnerSlot);
        winner.setGameScore(winner.getGameScore() + points);
        matchPlayerRepository.save(winner);
    }

    public TrickDTO prepareTrickDTO(Match match, Game game, MatchPlayer pollingMatchPlayer) {
        if (game.getTrickPhase().inTransition()) {
            return preparePreviousTrickDTO(match, game, pollingMatchPlayer);
        }

        List<String> currentTrick = game.getCurrentTrick();
        List<Integer> absoluteOrder = game.getTrickMatchPlayerSlotOrder();

        int pollingSlot = pollingMatchPlayer.getMatchPlayerSlot();

        List<TrickCard> cards = new ArrayList<>();
        for (int i = 0; i < currentTrick.size(); i++) {
            String code = currentTrick.get(i);
            int absoluteSlot = absoluteOrder.get(i);
            int relativeSlot = (absoluteSlot - pollingSlot + 4) % 4;
            cards.add(new TrickCard(code, relativeSlot, i));
        }

        Integer leaderAbsolute = game.getTrickLeaderMatchPlayerSlot();
        int leaderRelative = (leaderAbsolute - pollingSlot + 4) % 4;

        return new TrickDTO(cards, leaderRelative, null);
    }

    public TrickDTO preparePreviousTrickDTO(Match match, Game game, MatchPlayer pollingMatchPlayer) {
        List<String> previousTrick = game.getPreviousTrick();
        List<Integer> absoluteOrder = game.getPreviousTrickMatchPlayerSlotOrder();

        int pollingSlot = pollingMatchPlayer.getMatchPlayerSlot();

        List<TrickCard> cards = new ArrayList<>();
        for (int i = 0; i < previousTrick.size(); i++) {
            String code = previousTrick.get(i);
            int absoluteSlot = absoluteOrder.get(i);
            int relativeSlot = (absoluteSlot - pollingSlot + 4) % 4;
            cards.add(new TrickCard(code, relativeSlot, i));
        }

        Integer leaderAbsolute = game.getPreviousTrickLeaderMatchPlayerSlot();
        int leaderRelative = (leaderAbsolute - pollingSlot + 4) % 4;

        Integer winnerAbsolute = game.getTrickPhase().inTransition()
                ? game.getPreviousTrickWinnerMatchPlayerSlot()
                : null;
        Integer winnerRelative = winnerAbsolute != null ? (winnerAbsolute - pollingSlot + 4) % 4 : null;

        return new TrickDTO(cards, leaderRelative, winnerRelative);
    }

    public void afterCardPlayed(Game game) {

        if (game.getCurrentTrickSize() == 1) {
            game.setTrickPhase(TrickPhase.RUNNINGTRICK);
        }

        if (game.getCurrentTrickSize() < GameConstants.MAX_TRICK_SIZE) {
            int nextSlot = (game.getCurrentMatchPlayerSlot() % GameConstants.MAX_TRICK_SIZE) + 1;
            game.setCurrentMatchPlayerSlot(nextSlot);
        } else {
            handlePotentialTrickCompletion(game.getMatch(), game);
        }
    }

    @Transactional
    public void handlePotentialTrickCompletion(Match match, Game game) {
        log.info(" (No trick completion yet.)");
        if (game.getCurrentTrickSize() != GameConstants.MAX_TRICK_SIZE) {
            return; // Trick is not complete yet
        }
        log.info(" &&& TRICK COMPLETION: {}. &&&", game.getCurrentTrick());

        // Step A1: Determine winner and points based on current trick
        int winnerMatchPlayerSlot = cardRulesService.determineTrickWinner(game);
        int points = cardRulesService.calculateTrickPoints(game, winnerMatchPlayerSlot);

        // Step A2: Adds the points to the correct entry in the MatchPlayer relation
        MatchPlayer winnerMatchPlayer = matchPlayerRepository.findByMatchAndMatchPlayerSlot(game.getMatch(),
                winnerMatchPlayerSlot);
        winnerMatchPlayer.setGameScore(winnerMatchPlayer.getGameScore() + points);

        matchPlayerRepository.save(winnerMatchPlayer);
        matchPlayerRepository.flush();

        log.info(" & Trick winnerMatchPlayerSlot {} ({} points)", winnerMatchPlayerSlot, points);

        // Step A3: Collect cards played in the current trick
        // List<String> currentTrickCardCodes = game.getCurrentTrick();
        List<GameStats> currentTrickStats = gameStatsRepository.findByGameAndTrickNumber(game,
                game.getCurrentTrickNumber());

        List<String> takenCardsThisTrick = currentTrickStats.stream()
                .map(GameStats::getRankSuit)
                .toList();

        if (winnerMatchPlayer.getTakenCards() == null) {
            winnerMatchPlayer.setTakenCards(new ArrayList<>());
        }
        winnerMatchPlayer.getTakenCards().addAll(takenCardsThisTrick);

        // Step A4: Archive the trick
        // move current trick to previous, but do not clear it just yet.
        game.setPreviousTrick(game.getCurrentTrick());
        game.setPreviousTrickLeaderMatchPlayerSlot(game.getTrickLeaderMatchPlayerSlot());
        game.setPreviousTrickWinnerMatchPlayerSlot(winnerMatchPlayerSlot);
        game.setPreviousTrickPoints(points);

        // Step A5: Make everything ready. All that remains is clearing the trick.
        game.setTrickLeaderMatchPlayerSlot(game.getPreviousTrickWinnerMatchPlayerSlot());
        game.setCurrentMatchPlayerSlot(game.getPreviousTrickWinnerMatchPlayerSlot());

        log.info("Previous leader MatchPlayerSlot: {}, new leader (absolute): {}",
                game.getPreviousTrickLeaderMatchPlayerSlot(),
                game.getTrickLeaderMatchPlayerSlot());

        // Step A6:
        game.setTrickJustCompletedTime(Instant.now());
        if (match.getFastForwardMode()) {
            clearTrick(match, game);

            log.info("Fast-forwarded trick to READY phase");
            return; // Don't stop — let upper layer (MatchService) continue
        }
        game.setTrickPhase(TrickPhase.TRICKJUSTCOMPLETED);
        log.info(" & TrickPhase set to JUSTCOMPLETED at {}", game.getTrickJustCompletedTime());

        // STOP HERE AND WAIT FOR A POLLING BY THE MATCH OWNER TO PICK UP WHERE YOU
        // LEFT.
    }

    public void clearTrick(Match match, Game game) {
        game.setPreviousTrick(game.getCurrentTrick());
        game.clearCurrentTrick();
        game.setTrickPhase(TrickPhase.READYFORFIRSTCARD);
        game.setCurrentTrickNumber(game.getCurrentTrickNumber() + 1);
        log.info(
                "Trick was just cleared in GameTrickService. GamePhase={}, TrickPhase={}, PlayOrder={}, currentTrickNumber={}.",
                game.getPhase(),
                game.getTrickPhase(),
                game.getCurrentPlayOrder(),
                game.getCurrentTrickNumber());
    }

}
