package ch.uzh.ifi.hase.soprafs24.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayerCards;

@Service
public class AiPassingService {

    public List<String> selectCardsToPass(MatchPlayer matchPlayer) {
        List<MatchPlayerCards> hand = matchPlayer.getCardsInHand();
        if (hand == null || hand.size() < 3) {
            throw new IllegalStateException("AI player does not have enough cards to pass.");
        }

        // First, sort hand:
        List<MatchPlayerCards> sortedHand = hand.stream()
                .sorted((card1, card2) -> {
                    // Prioritize Queen of Spades
                    if (card1.getCard().equals("QS"))
                        return -1;
                    if (card2.getCard().equals("QS"))
                        return 1;

                    // Then prioritize hearts
                    boolean c1Heart = card1.getCard().endsWith("H");
                    boolean c2Heart = card2.getCard().endsWith("H");
                    if (c1Heart && !c2Heart)
                        return -1;
                    if (!c1Heart && c2Heart)
                        return 1;

                    // Otherwise no priority, leave natural order
                    return 0;
                })
                .collect(Collectors.toList());

        return sortedHand.stream()
                .limit(3)
                .map(MatchPlayerCards::getCard)
                .collect(Collectors.toList());
    }

}
