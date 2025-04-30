package ch.uzh.ifi.hase.soprafs24.service;

import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;

@Service
public class AiPlayingService {

    private final CardRulesService cardRulesService;

    public AiPlayingService(CardRulesService cardRulesService) {
        this.cardRulesService = cardRulesService;
    }

    public String selectCardToPlay(Game game, MatchPlayer matchPlayer) {
        String playableCardsString = cardRulesService.getPlayableCardsForMatchPlayerPolling(game, matchPlayer);

        System.out.println("I am MatchPlayer with hand: " + matchPlayer.getHand());
        System.out.println("I am MatchPlayer with playable hand: " + playableCardsString);

        if (playableCardsString == null || playableCardsString.isBlank()) {
            throw new IllegalStateException(
                    "AI player has no legal cards to play: " + matchPlayer.getHand());
        }

        String[] legalCards = playableCardsString.split(",");

        System.out.println("Hi, I am an AI player, making a decision.");
        System.out.println("My legal cards are: " + String.join(", ", legalCards));

        String cardCode = legalCards[0]; // pick the first one
        System.out.println("I choose: " + cardCode);
        return cardCode;
    }

}
