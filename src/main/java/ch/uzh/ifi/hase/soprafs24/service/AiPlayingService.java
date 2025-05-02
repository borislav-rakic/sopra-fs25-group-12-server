package ch.uzh.ifi.hase.soprafs24.service;

import java.util.Random;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.constant.Strategy;

@Service
@Qualifier("aiPlayingService")
public class AiPlayingService {

    private final CardRulesService cardRulesService;

    public AiPlayingService(CardRulesService cardRulesService) {
        this.cardRulesService = cardRulesService;
    }

    public String selectCardToPlay(Game game, MatchPlayer matchPlayer, Strategy strategy) {
        String playableCardsString = cardRulesService.getPlayableCardsForMatchPlayerPolling(game, matchPlayer);

        System.out.println("I am MatchPlayer with hand: " + matchPlayer.getHand());
        System.out.println("I am MatchPlayer with playable hand: " + playableCardsString);

        if (playableCardsString == null || playableCardsString.isBlank()) {
            throw new IllegalStateException(
                    "AI player has no legal cards to play: " + matchPlayer.getHand());
        }

        Random random = new Random();

        // Handle WAVERING by picking a non-WAVERING strategy at random
        if (strategy == Strategy.WAVERING) {
            do {
                strategy = Strategy.values()[random.nextInt(Strategy.values().length)];
            } while (strategy == Strategy.WAVERING);
            System.out.println("WAVERING resolved to: " + strategy);
        }

        String[] legalCards = playableCardsString.split(",");

        System.out.println("Hi, I am an AI player, making a decision.");
        System.out.println("My legal cards are: " + String.join(", ", legalCards));

        String cardCode;
        switch (strategy) {
            case LEFTMOST -> cardCode = legalCards[0];
            case RIGHTMOST -> cardCode = legalCards[legalCards.length - 1];
            case RANDOM -> cardCode = legalCards[random.nextInt(legalCards.length)];
            default -> cardCode = legalCards[0]; // fallback
        }

        System.out.println("I choose: " + cardCode);
        return cardCode;
    }

}
