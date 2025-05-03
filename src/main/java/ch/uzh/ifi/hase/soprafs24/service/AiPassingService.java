package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs24.constant.Strategy;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.PassedCard;
import ch.uzh.ifi.hase.soprafs24.repository.PassedCardRepository;
import ch.uzh.ifi.hase.soprafs24.util.CardUtils;
import ch.uzh.ifi.hase.soprafs24.util.StrategyRegistry;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Available Strategies
// LEFTMOST, // Always take the leftmost card.
// RANDOM, // Take any playable card.
// DUMPHIGHESTFACEFIRST, // Try to get rid of QS,AH,KH,QH,JH,AS,AD,AC,KS, ... in that order.
// GETRIDOFCLUBSTHENHEARTS, // Try to get rid of Clubs as quickly as possible, then hearts.
// PREFERBLACK, // Try to keep black cards and discard red cards
// VOIDSUIT, // Get rid of the suit with the fewest items first.
// HYPATIA, // Think like Hypatia
// GARY, // Think like Gary
// ADA; // Think like Ada

@Service
@Qualifier("aiPassingService")
public class AiPassingService {
    private static final Logger log = LoggerFactory.getLogger(AiPassingService.class);

    private final MatchPlayerRepository matchPlayerRepository;
    private final PassedCardRepository passedCardRepository;

    @Autowired
    public AiPassingService(MatchPlayerRepository matchPlayerRepository, PassedCardRepository passedCardRepository) {
        this.matchPlayerRepository = matchPlayerRepository;
        this.passedCardRepository = passedCardRepository;
    }

    public List<String> selectCardsToPass(MatchPlayer matchPlayer, Strategy strategy) {
        String hand = matchPlayer.getHand();
        if (hand == null || hand.isBlank()) {
            throw new IllegalStateException("AI player has no cards to pass.");
        }

        String[] cardsArray = hand.split(",");
        if (cardsArray.length < 3) {
            throw new IllegalStateException("Player does not have enough cards to pass.");
        }
        Strategy effectiveStrategy = strategy;

        List<String> cards = new ArrayList<>(List.of(cardsArray));
        List<String> selectedCards = new ArrayList<>(List.of(cardsArray));

        switch (effectiveStrategy) {
            case LEFTMOST:
                selectedCards = cards.subList(0, 3);
                break;

            case RANDOM:
                Collections.shuffle(cards);
                selectedCards = cards.subList(0, 3);
                break;

            case DUMPHIGHESTFACEFIRST:
                Collections.shuffle(cards);
                selectedCards = cards.subList(0, 3);
                break;

            case GETRIDOFCLUBSTHENHEARTS:
                Collections.shuffle(cards);
                selectedCards = cards.subList(0, 3);
                break;

            case PREFERBLACK:
                Collections.shuffle(cards);
                selectedCards = cards.subList(0, 3);
                break;

            case VOIDSUIT:
                Collections.shuffle(cards);
                selectedCards = cards.subList(0, 3);
                break;

            case HYPATIA:
                Collections.shuffle(cards);
                selectedCards = cards.subList(0, 3);
                break;

            case GARY:
                Collections.shuffle(cards);
                selectedCards = cards.subList(0, 3);
                break;

            case ADA:
                Collections.shuffle(cards);
                selectedCards = cards.subList(0, 3);
                break;

            default:
                throw new IllegalArgumentException("Unsupported strategy: " + strategy);
        }
        log.info("User has decided on three Cards to pass.");
        return selectedCards;
    }

    public void passForAllAiPlayers(Game game) {
        Match match = game.getMatch();
        if (match == null) {
            throw new IllegalStateException("Game does not belong to match.");
        }
        Strategy strategy = Strategy.RANDOM;
        for (int matchPlayerSlot = 1; matchPlayerSlot <= 4; matchPlayerSlot++) {
            MatchPlayer matchPlayer = match.requireMatchPlayerBySlot(matchPlayerSlot);
            if (matchPlayer == null) {
                int playerSlot = matchPlayerSlot - 1;
                throw new IllegalStateException(
                        String.format("There is no match player in playerSlot %d.", playerSlot));
            }
            if (Boolean.TRUE.equals(matchPlayer.getIsAiPlayer())) {
                strategy = StrategyRegistry.getStrategyForUserId(matchPlayer.getUser().getId());
                // For now we keep it predictability at LEFTMOST
                strategy = Strategy.LEFTMOST;
                List<String> cardsToPass = selectCardsToPass(matchPlayer, strategy);
                for (String cardCode : cardsToPass) {
                    if (!passedCardRepository.existsByGameAndRankSuit(game, cardCode)) {
                        PassedCard passedCard = new PassedCard(game, cardCode, matchPlayerSlot, game.getGameNumber());
                        passedCard.setGame(game);
                        passedCardRepository.save(passedCard);

                        // Also remove the passed card from AI hand
                        matchPlayer.removeCardCodeFromHand(cardCode);
                        matchPlayerRepository.flush();
                    }
                }
            }
        }
    }

    public List<String> dumpHighestScoringFaceFirst(List<String> hand) {
        return hand.stream()
                .sorted(Comparator.comparingInt(CardUtils::calculateHighestScoreOrder).reversed()) // highest first
                .limit(3)
                .collect(Collectors.toList());
    }
}
