package ch.uzh.ifi.hase.soprafs24.service;

import java.util.Random;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.constant.Strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Qualifier("aiPlayingService")
public class AiPlayingService {

    private static final Logger log = LoggerFactory.getLogger(AiPlayingService.class);

    private final CardRulesService cardRulesService;

    public AiPlayingService(CardRulesService cardRulesService) {
        this.cardRulesService = cardRulesService;
    }

    public String selectCardToPlay(Game game, MatchPlayer matchPlayer, Strategy strategy) {
        String playableCardsString = cardRulesService.getPlayableCardsForMatchPlayerPolling(game, matchPlayer);

        // log.info ("I am MatchPlayer with hand: {}.", matchPlayer.getHand());
        // log.info ("I am MatchPlayer with playable hand: {}.", playableCardsString);

        if (playableCardsString == null || playableCardsString.isBlank()) {
            throw new IllegalStateException(
                    "AI player has no legal cards to play: " + matchPlayer.getHand());
        }

        Random random = new Random();

        String[] legalCards = playableCardsString.split(",");

        // log.info("Hi, I am an AI player, making a decision.");
        // log.info("My legal cards are: ", String.join(", ", legalCards));

        String cardCode;
        switch (strategy) {
            case LEFTMOST -> cardCode = legalCards[0];
            case RANDOM -> cardCode = legalCards[random.nextInt(legalCards.length)];
            case DUMPHIGHESTFACEFIRST -> cardCode = legalCards[random.nextInt(legalCards.length)];
            case GETRIDOFCLUBSTHENHEARTS -> cardCode = legalCards[random.nextInt(legalCards.length)];
            case PREFERBLACK -> cardCode = legalCards[random.nextInt(legalCards.length)];
            case VOIDSUIT -> cardCode = legalCards[random.nextInt(legalCards.length)];
            case HYPATIA -> cardCode = legalCards[random.nextInt(legalCards.length)];
            case GARY -> cardCode = legalCards[random.nextInt(legalCards.length)];
            case ADA -> cardCode = legalCards[random.nextInt(legalCards.length)];
            default -> cardCode = legalCards[0]; // fallback => RANDOM
        }

        log.info("AI Player chooses: {}.", cardCode);
        return cardCode;
    }

}
