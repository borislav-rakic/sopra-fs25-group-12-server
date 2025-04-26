package ch.uzh.ifi.hase.soprafs24.service;

import java.util.List;

import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.model.Card;

@Service
public class AiPlayingService {

    private final CardRulesService cardRulesService;

    public AiPlayingService(CardRulesService cardRulesService) {
        this.cardRulesService = cardRulesService;
    }

    public Card selectCardToPlay(Game game, MatchPlayer matchPlayer) {
        List<Card> legalCards = cardRulesService.getLegalCardsAsCards(game, matchPlayer);

        if (legalCards.isEmpty()) {
            throw new IllegalStateException("AI player has no legal cards to play.");
        }

        // Pick the first legal card (simple AI)
        return legalCards.get(0);
    }
}
