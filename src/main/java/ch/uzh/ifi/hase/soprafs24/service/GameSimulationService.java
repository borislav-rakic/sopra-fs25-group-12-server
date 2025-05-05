package ch.uzh.ifi.hase.soprafs24.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.util.CardUtils;

import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Qualifier("gameSimulationService")
public class GameSimulationService {
    private static final Logger log = LoggerFactory.getLogger(GameSimulationService.class);

    private final CardRulesService cardRulesService;
    private final GameService gameService;

    private final Random random = new Random();

    @Autowired
    public GameSimulationService(CardRulesService cardRulesService, GameService gameService) {
        this.cardRulesService = cardRulesService;
        this.gameService = gameService;
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

        log.info("I am MatchPlayer {} in simulation and I decided to play {}.", matchPlayer.getUser().getUsername(),
                cardCode);

        gameService.executeValidatedCardPlay(game, matchPlayer, cardCode);
    }

}
