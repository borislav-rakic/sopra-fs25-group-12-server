package ch.uzh.ifi.hase.soprafs24.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.PassedCard;
import ch.uzh.ifi.hase.soprafs24.repository.PassedCardRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AiPassingService {
    private static final Logger log = LoggerFactory.getLogger(AiPassingService.class);

    private final MatchPlayerRepository matchPlayerRepository;
    private final PassedCardRepository passedCardRepository;

    @Autowired
    public AiPassingService(MatchPlayerRepository matchPlayerRepository, PassedCardRepository passedCardRepository) {
        this.matchPlayerRepository = matchPlayerRepository;
        this.passedCardRepository = passedCardRepository;
    }

    public List<String> selectCardsToPass(MatchPlayer matchPlayer) {
        String hand = matchPlayer.getHand();
        if (hand == null || hand.isBlank()) {
            throw new IllegalStateException("AI player has no cards to pass.");
        }

        String[] cardsArray = hand.split(",");
        if (cardsArray.length < 3) {
            throw new IllegalStateException("AI player does not have enough cards to pass.");
        }

        List<String> sortedHand = java.util.Arrays.stream(cardsArray)
                .sorted((card1, card2) -> {
                    if (card1.equals("QS"))
                        return -1;
                    if (card2.equals("QS"))
                        return 1;

                    boolean c1Heart = card1.endsWith("H");
                    boolean c2Heart = card2.endsWith("H");
                    if (c1Heart && !c2Heart)
                        return -1;
                    if (!c1Heart && c2Heart)
                        return 1;

                    return 0;
                })
                .collect(Collectors.toList());

        log.error("Location: AiPassingService.selectCardsToPass. Hand: " + String.join(",", sortedHand));

        return sortedHand.stream()
                .limit(3)
                .collect(Collectors.toList());
    }

    public void passForAllAiPlayers(Game game) {
        Match match = game.getMatch();
        if (match == null) {
            throw new IllegalStateException("Game does not belong to match.");
        }

        for (int slot = 1; slot <= 4; slot++) {
            MatchPlayer matchPlayer = match.getMatchPlayerBySlot(slot);
            if (matchPlayer == null) {
                throw new IllegalStateException("There is no match player in slot " + slot + ".");
            }
            if (Boolean.TRUE.equals(matchPlayer.getIsAiPlayer())) {
                List<String> cardsToPass = selectCardsToPass(matchPlayer);

                for (String cardCode : cardsToPass) {
                    if (!passedCardRepository.existsByGameAndRankSuit(game, cardCode)) {
                        PassedCard passedCard = new PassedCard(game, cardCode, slot, game.getGameNumber());
                        passedCardRepository.save(passedCard);

                        // Also remove the passed card from AI hand
                        matchPlayer.removeCardCodeFromHand(cardCode);
                        matchPlayerRepository.flush();
                    }
                }
            }
        }
    }
}
