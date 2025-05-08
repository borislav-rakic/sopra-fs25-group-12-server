package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameConstants;
import ch.uzh.ifi.hase.soprafs24.constant.TrickPhase;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GameTrickService {

    private static final Logger log = LoggerFactory.getLogger(GameTrickService.class);

    private final MatchPlayerRepository matchPlayerRepository;
    private final CardRulesService cardRulesService;

    @Autowired
    public GameTrickService(MatchPlayerRepository matchPlayerRepository, CardRulesService cardRulesService) {
        this.matchPlayerRepository = matchPlayerRepository;
        this.cardRulesService = cardRulesService;
    }

    public void addCardToTrick(Match match, Game game, MatchPlayer matchPlayer, String cardCode) {
        game.addCardCodeToCurrentTrick(cardCode);
        game.setCurrentPlayOrder(game.getCurrentPlayOrder() + 1);
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

    public void clearTrick(Match match, Game game) {
        game.setPreviousTrick(game.getCurrentTrick());
        game.clearCurrentTrick();
        game.setCurrentTrickNumber(game.getCurrentTrickNumber() + 1);
    }

    public TrickDTO prepareTrickDTO(Match match, Game game, MatchPlayer pollingMatchPlayer) {
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

        Integer winnerAbsolute = game.getTrickPhase().inPause()
                ? game.getPreviousTrickWinnerMatchPlayerSlot()
                : null;
        Integer winnerRelative = winnerAbsolute != null ? (winnerAbsolute - pollingSlot + 4) % 4 : null;

        return new TrickDTO(cards, leaderRelative, winnerRelative);
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

        Integer winnerAbsolute = game.getTrickPhase().inPause()
                ? game.getPreviousTrickWinnerMatchPlayerSlot()
                : null;
        Integer winnerRelative = winnerAbsolute != null ? (winnerAbsolute - pollingSlot + 4) % 4 : null;

        return new TrickDTO(cards, leaderRelative, winnerRelative);
    }

    public void afterCardPlayed(Game game) {
        game.setCurrentPlayOrder(game.getCurrentPlayOrder() + 1);

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
    private void handlePotentialTrickCompletion(Match match, Game game) {
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

        // Step A3: Archive the trick
        // move current trick to previous, but do not clear it just yet.
        game.setPreviousTrick(game.getCurrentTrick());
        game.setPreviousTrickWinnerMatchPlayerSlot(winnerMatchPlayerSlot);
        game.setPreviousTrickPoints(points);

        // Step A4: Make everything ready. All that remains is clearing the trick.
        game.setTrickLeaderMatchPlayerSlot(game.getPreviousTrickWinnerMatchPlayerSlot());
        game.setCurrentMatchPlayerSlot(game.getPreviousTrickWinnerMatchPlayerSlot());

        // Step A5:
        game.setTrickJustCompletedTime(Instant.now());
        game.setTrickPhase(TrickPhase.TRICKJUSTCOMPLETED);
        log.info(" & TrickPhase set to JUSTCOMPLETED at {}", game.getTrickJustCompletedTime());

        // STOP HERE AND WAIT FOR A POLLING BY THE MATCH OWNER TO PICK UP WHERE YOU
        // LEFT.
    }

}
