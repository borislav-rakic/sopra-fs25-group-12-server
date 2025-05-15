package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        Strategy effectiveStrategy = (strategy == null) ? Strategy.LEFTMOST : strategy;

        List<String> cards = new ArrayList<>(List.of(cardsArray));
        List<String> selectedCards = new ArrayList<>(List.of(cardsArray));

        boolean mayTryShootMoon = isPotentialMoonShooter(cards);

        switch (effectiveStrategy) {
            case LEFTMOST:
                selectedCards = cards.subList(0, 3);
                break;

            case RANDOM:
                Collections.shuffle(cards);
                selectedCards = cards.subList(0, 3);
                break;

            case DUMPHIGHESTFACEFIRST:
                selectedCards = dumpHighestScoringFaceFirst(cards);
                break;

            case GETRIDOFCLUBSTHENHEARTS:
                selectedCards = cards.stream()
                        .sorted(Comparator.comparingInt(card -> {
                            char suit = card.charAt(card.length() - 1);
                            return switch (suit) {
                                case 'C' -> 0; // Clubs first
                                case 'H' -> 1; // Then Hearts
                                case 'S' -> 2;
                                case 'D' -> 3;
                                default -> 4;
                            };
                        }))
                        .limit(3)
                        .collect(Collectors.toList());
                break;

            case PREFERBLACK:
                selectedCards = cards.stream()
                        .sorted(Comparator.comparingInt(card -> {
                            char suit = card.charAt(card.length() - 1);
                            return (suit == 'C' || suit == 'S') ? 0 : 1; // ♣♠ first
                        }))
                        .limit(3)
                        .collect(Collectors.toList());
                break;

            case VOIDSUIT:
                Map<Character, List<String>> suitMapVoidsuit = cards.stream()
                        .collect(Collectors.groupingBy(card -> card.charAt(card.length() - 1)));

                char suitToVoid = suitMapVoidsuit.entrySet().stream()
                        .min(Comparator.comparingInt(entry -> entry.getValue().size()))
                        .map(Map.Entry::getKey)
                        .orElse('C');

                List<String> voidCards = suitMapVoidsuit.get(suitToVoid);
                if (voidCards.size() >= 3) {
                    selectedCards = voidCards.subList(0, 3);
                } else {
                    selectedCards = new ArrayList<>(voidCards);
                    for (String card : cards) {
                        if (!selectedCards.contains(card) && selectedCards.size() < 3) {
                            selectedCards.add(card);
                        }
                    }
                }
                break;

            case HYPATIA:
                if (mayTryShootMoon) {
                    selectedCards = cards.stream()
                            .sorted(Comparator.comparingInt(CardUtils::calculateCardOrder))
                            .limit(3)
                            .collect(Collectors.toList());
                } else {
                    selectedCards = dumpHighestScoringFaceFirst(cards);
                }
                break;

            case GARY:
                if (mayTryShootMoon) {
                    // Keep high cards, pass lowest 3
                    Collections.sort(cards, new Comparator<String>() {
                        @Override
                        public int compare(String a, String b) {
                            return Integer.compare(CardUtils.calculateCardOrder(a), CardUtils.calculateCardOrder(b));
                        }
                    });
                    selectedCards = cards.subList(0, 3);
                } else {
                    // Group cards by suit
                    Map<Character, List<String>> suitMapGary = new java.util.HashMap<>();
                    for (String card : cards) {
                        char suit = card.charAt(card.length() - 1);
                        suitMapGary.computeIfAbsent(suit, k -> new ArrayList<>()).add(card);
                    }

                    // Try to void Clubs or Diamonds
                    char suitToVoidGary = 'C'; // default
                    int minSize = Integer.MAX_VALUE;
                    for (char suit : new char[] { 'C', 'D' }) {
                        List<String> suitCards = suitMapGary.get(suit);
                        if (suitCards != null && suitCards.size() < minSize) {
                            suitToVoid = suit;
                            minSize = suitCards.size();
                        }
                    }

                    List<String> voidable = suitMapGary.getOrDefault(suitToVoidGary, new ArrayList<>());

                    selectedCards = new ArrayList<>();
                    for (int i = 0; i < voidable.size() && selectedCards.size() < 3; i++) {
                        selectedCards.add(voidable.get(i));
                    }

                    // If we didn't get enough cards from voiding, add high-risk cards
                    List<String> remaining = new ArrayList<>();
                    for (String card : cards) {
                        if (!selectedCards.contains(card)) {
                            remaining.add(card);
                        }
                    }

                    Collections.sort(remaining, new Comparator<String>() {
                        @Override
                        public int compare(String a, String b) {
                            return Integer.compare(
                                    CardUtils.calculateHighestScoreOrder(b),
                                    CardUtils.calculateHighestScoreOrder(a));
                        }
                    });

                    for (int i = 0; i < remaining.size() && selectedCards.size() < 3; i++) {
                        selectedCards.add(remaining.get(i));
                    }
                }
                break;

            case ADA:
                if (mayTryShootMoon) {
                    // If she might shoot the moon, pass lowest-value junk cards
                    Collections.sort(cards, new Comparator<String>() {
                        @Override
                        public int compare(String a, String b) {
                            return Integer.compare(CardUtils.calculateCardOrder(a), CardUtils.calculateCardOrder(b));
                        }
                    });
                    selectedCards = cards.subList(0, 3);
                } else {
                    // Strategy: pass cards from suits with the fewest cards (odd ones out)
                    Map<Character, List<String>> suits = new java.util.HashMap<>();
                    for (String card : cards) {
                        char suit = card.charAt(card.length() - 1);
                        suits.computeIfAbsent(suit, k -> new ArrayList<>()).add(card);
                    }

                    // Find the suit(s) with the fewest cards
                    List<String> passCandidates = new ArrayList<>();
                    int minCount = Integer.MAX_VALUE;

                    for (Map.Entry<Character, List<String>> entry : suits.entrySet()) {
                        List<String> suitCards = entry.getValue();
                        if (suitCards.size() < minCount) {
                            minCount = suitCards.size();
                            passCandidates = new ArrayList<>(suitCards);
                        } else if (suitCards.size() == minCount) {
                            // Add these too, in case of tie
                            passCandidates.addAll(suitCards);
                        }
                    }

                    // Add up to 3 pass cards from the smallest suit group(s)
                    selectedCards = new ArrayList<>();
                    for (int i = 0; i < passCandidates.size() && selectedCards.size() < 3; i++) {
                        selectedCards.add(passCandidates.get(i));
                    }

                    // Fill with lowest non-selected cards if needed
                    if (selectedCards.size() < 3) {
                        List<String> remaining = new ArrayList<>();
                        for (String card : cards) {
                            if (!selectedCards.contains(card)) {
                                remaining.add(card);
                            }
                        }

                        Collections.sort(remaining, new Comparator<String>() {
                            @Override
                            public int compare(String a, String b) {
                                return Integer.compare(CardUtils.calculateCardOrder(a),
                                        CardUtils.calculateCardOrder(b));
                            }
                        });

                        for (int i = 0; i < remaining.size() && selectedCards.size() < 3; i++) {
                            selectedCards.add(remaining.get(i));
                        }
                    }
                }
                break;

            default:
                throw new IllegalArgumentException("Unsupported strategy: " + strategy);
        }
        log.info("User has decided on three Cards to pass.");
        return selectedCards;
    }

    public Strategy getStrategyForUserId(Long userId) {
        return switch (userId.intValue()) {
            case 1 -> Strategy.LEFTMOST;
            case 2 -> Strategy.RANDOM;
            case 3 -> Strategy.DUMPHIGHESTFACEFIRST;
            case 4 -> Strategy.GETRIDOFCLUBSTHENHEARTS;
            case 5 -> Strategy.PREFERBLACK;
            case 6 -> Strategy.VOIDSUIT;
            case 7 -> Strategy.HYPATIA;
            case 8 -> Strategy.GARY;
            case 9 -> Strategy.ADA;
            default -> Strategy.LEFTMOST; // Fallback
        };
    }

    public void passForAllAiPlayers(Game game) {
        Match match = game.getMatch();
        if (match == null) {
            throw new IllegalStateException("Game does not belong to match.");
        }
        for (int matchPlayerSlot = 1; matchPlayerSlot <= 4; matchPlayerSlot++) {
            MatchPlayer matchPlayer = match.requireMatchPlayerBySlot(matchPlayerSlot);
            if (matchPlayer == null) {
                int playerSlot = matchPlayerSlot - 1;
                throw new IllegalStateException(
                        String.format("There is no match player in playerSlot %d.", playerSlot));
            }
            if (Boolean.TRUE.equals(matchPlayer.getIsAiPlayer())) {
                Strategy strategy = getStrategyForUserId(matchPlayer.getUser().getId());

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

    public boolean isPotentialMoonShooter(List<String> hand) {
        long highHearts = hand.stream()
                .filter(card -> card.endsWith("H"))
                .map(card -> CardUtils.rankStrToInt(card.substring(0, card.length() - 1)))
                .filter(rank -> rank >= 10)
                .count();

        boolean hasQS = hand.contains("QS");
        long spades = hand.stream().filter(card -> card.endsWith("S")).count();
        long hearts = hand.stream().filter(card -> card.endsWith("H")).count();

        // Heuristic: at least 6 hearts (3 of them high), QS, and few low off-suits
        return hearts >= 6 && highHearts >= 3 && hasQS && spades >= 3;
    }

}
