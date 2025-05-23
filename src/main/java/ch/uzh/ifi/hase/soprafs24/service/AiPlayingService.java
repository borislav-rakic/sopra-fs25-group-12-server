package ch.uzh.ifi.hase.soprafs24.service;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.exceptions.GameplayException;
import ch.uzh.ifi.hase.soprafs24.repository.GameStatsRepository;
import ch.uzh.ifi.hase.soprafs24.util.CardUtils;
import ch.uzh.ifi.hase.soprafs24.constant.Strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Qualifier("aiPlayingService")
public class AiPlayingService {

    private static final Logger log = LoggerFactory.getLogger(AiPlayingService.class);

    private final CardRulesService cardRulesService;
    private final GameStatsRepository gameStatsRepository;

    public AiPlayingService(
            CardRulesService cardRulesService,
            GameStatsRepository gameStatsRepository) {
        this.cardRulesService = cardRulesService;
        this.gameStatsRepository = gameStatsRepository;
    }

    /**
     * Selects a single card for the AI player to play based on the given strategy
     * and current game state.
     * The selection logic considers legal cards, game phase, player slot, trick
     * order, and risk assessment
     * (e.g., avoiding dangerous cards or trying to shoot the moon).
     * 
     * Each strategy applies its own logic, ranging from simple heuristics (like
     * leftmost or random) to
     * advanced reasoning (like evaluating voiding suits or point-dumping
     * potential).
     * 
     * If no legal cards are available, an exception is thrown to indicate an
     * invalid game state.
     * 
     * @param game        the current game state
     * @param matchPlayer the AI player making the move
     * @param strategy    the strategy to guide card selection
     * @return the selected card code to be played (e.g., "QS", "7H")
     * @throws IllegalStateException if the player has no legal cards to play
     */
    public String selectCardToPlay(Game game, MatchPlayer matchPlayer, Strategy strategy) {
        String playableCardsString = cardRulesService.getPlayableCardsForMatchPlayerPolling(game, matchPlayer);

        // log.info ("I am MatchPlayer with hand: {}.", matchPlayer.getHand());
        // log.info ("I am MatchPlayer with playable hand: {}.", playableCardsString);

        if (playableCardsString == null || playableCardsString.isBlank()) {
            throw new GameplayException("No playable cards available for the player.");
        }

        Random random = new Random();

        String[] legalCards = playableCardsString.split(",");

        // log.info("Hi, I am an AI player, making a decision.");
        // log.info("My legal cards are: ", String.join(", ", legalCards));

        String cardCode;
        switch (strategy) {
            case LEFTMOST -> {
                cardCode = Arrays.stream(legalCards)
                        .sorted(Comparator.comparingInt(CardUtils::calculateCardOrder))
                        .findFirst()
                        .orElseThrow(() -> new GameplayException("No card to play"));
            }
            case RANDOM -> cardCode = legalCards[random.nextInt(legalCards.length)];
            case DUMPHIGHESTFACEFIRST -> {
                List<String> sorted = Arrays.stream(legalCards)
                        .sorted((a, b) -> Integer.compare(
                                CardUtils.calculateHighestScoreOrder(b),
                                CardUtils.calculateHighestScoreOrder(a)))
                        .toList();

                cardCode = sorted.get(0); // Dump most dangerous card first
            }
            case GETRIDOFCLUBSTHENHEARTS -> {
                List<String> clubs = Arrays.stream(legalCards)
                        .filter(card -> card.endsWith("C"))
                        .sorted(CardUtils::compareCards) // play low clubs first
                        .toList();

                List<String> hearts = Arrays.stream(legalCards)
                        .filter(card -> card.endsWith("H"))
                        .sorted(CardUtils::compareCards) // play low hearts next
                        .toList();

                if (!clubs.isEmpty()) {
                    cardCode = clubs.get(0); // Prefer dumping clubs
                } else if (!hearts.isEmpty()) {
                    cardCode = hearts.get(0); // Then hearts
                } else {
                    // Fallback to least valuable card
                    List<String> sorted = Arrays.stream(legalCards)
                            .sorted(CardUtils::compareCards)
                            .toList();
                    cardCode = sorted.get(0);
                }
            }
            case PREFERBLACK -> {
                List<String> blackCards = Arrays.stream(legalCards)
                        .filter(card -> card.endsWith("C") || card.endsWith("S"))
                        .filter(card -> !card.equals("QS")) // Avoid dumping Q♠ unless desperate
                        .sorted(CardUtils::compareCards)
                        .toList();

                // Filter out dangerously high Spades unless we’re last to play
                List<String> safeBlackCards = blackCards.stream()
                        .filter(card -> {
                            if (!card.endsWith("S"))
                                return true; // All Clubs are fine
                            return !card.startsWith("A") && !card.startsWith("K"); // Avoid AS, KS
                        })
                        .toList();

                if (!safeBlackCards.isEmpty()) {
                    cardCode = safeBlackCards.get(0);
                } else if (!blackCards.isEmpty()) {
                    // Only risky black cards remain — pick lowest one
                    cardCode = blackCards.get(0);
                } else {
                    // No black cards — fallback to safest red card
                    List<String> redCards = Arrays.stream(legalCards)
                            .filter(card -> card.endsWith("H") || card.endsWith("D"))
                            .sorted(CardUtils::compareCards)
                            .toList();

                    cardCode = redCards.isEmpty() ? legalCards[0] : redCards.get(0);
                }
            }
            case VOIDSUIT -> {
                Map<Character, List<String>> suitMap = Arrays.stream(legalCards)
                        .collect(Collectors.groupingBy(card -> card.charAt(card.length() - 1)));

                Map<Character, Long> suitCountsInHand = CardUtils.splitCardCodesAsListOfStrings(matchPlayer.getHand())
                        .stream()
                        .collect(Collectors.groupingBy(card -> card.charAt(card.length() - 1), Collectors.counting()));

                // Prioritize suits where we have fewer cards (but avoid Spades unless high
                // ones)
                List<Character> suitsByVoidPotential = suitCountsInHand.entrySet().stream()
                        .sorted(Comparator.comparingLong(Map.Entry::getValue)) // least cards first
                        .map(Map.Entry::getKey)
                        .toList();

                String selected = null;

                for (Character suit : suitsByVoidPotential) {
                    List<String> options = suitMap.get(suit);
                    if (options == null || options.isEmpty()) {
                        continue;
                    }

                    // Handle Spades specially: avoid voiding small Spades
                    if (suit == 'S') {
                        List<String> highSpades = options.stream()
                                .filter(card -> {
                                    String rank = card.substring(0, card.length() - 1);
                                    int value = CardUtils.rankStrToInt(rank);
                                    return value >= 12; // Q, K, A
                                })
                                .sorted(CardUtils::compareCards)
                                .toList();

                        if (!highSpades.isEmpty()) {
                            selected = highSpades.get(0); // allow voiding via Q/K/A of Spades
                            break;
                        }

                        continue; // skip voiding Spades with low cards
                    }

                    // Non-Spade suit: go for lowest card to void
                    selected = options.stream()
                            .sorted(CardUtils::compareCards)
                            .findFirst()
                            .orElse(null);
                    break;
                }

                // Fallback: play lowest overall if nothing else applies
                if (selected == null) {
                    selected = Arrays.stream(legalCards)
                            .sorted(CardUtils::compareCards)
                            .findFirst()
                            .orElse(legalCards[0]);
                }

                cardCode = selected;
            }
            case HYPATIA -> {
                int mySlot = matchPlayer.getMatchPlayerSlot();
                List<String> currentTrick = game.getCurrentTrick();
                String leadSuit = game.getSuitOfFirstCardInCurrentTrick();
                List<Integer> trickOrder = game.getTrickMatchPlayerSlotOrder();
                int myIndex = trickOrder.indexOf(mySlot);
                List<Integer> playersAfterMe = trickOrder.subList(myIndex + 1, trickOrder.size());

                // Simple heuristic: already taken point cards → might be moon-shooting
                boolean attemptingMoonShot = isAttemptingMoonShot(matchPlayer);

                String safestWinningCard = null;
                String aggressiveWinningCard = null;

                for (String cardCodeHypatia : legalCards) {
                    boolean followsSuit = currentTrick.isEmpty() || cardCodeHypatia.endsWith(leadSuit);

                    if (!followsSuit) {
                        continue;
                    }

                    boolean wouldWin = currentTrick.stream()
                            .filter(c -> c.endsWith(leadSuit))
                            .allMatch(playedCard -> CardUtils.compareCards(cardCodeHypatia, playedCard) > 0);

                    if (!wouldWin)
                        continue;

                    BitSet possibleHolders = getPossibleHolders(cardCodeHypatia, game, mySlot, gameStatsRepository);
                    boolean canBeBeaten = playersAfterMe.stream().anyMatch(slot -> possibleHolders.get(slot - 1));

                    if (!canBeBeaten) {
                        safestWinningCard = cardCodeHypatia;
                        break;
                    } else if (attemptingMoonShot) {
                        aggressiveWinningCard = cardCodeHypatia; // might risk it
                    }
                }

                if (safestWinningCard != null) {
                    cardCode = safestWinningCard;
                } else if (attemptingMoonShot && aggressiveWinningCard != null) {
                    cardCode = aggressiveWinningCard;
                } else {
                    cardCode = Arrays.stream(legalCards)
                            .sorted(CardUtils::compareCards)
                            .findFirst()
                            .orElse(legalCards[0]);
                }
            }
            case GARY -> {
                int mySlot = matchPlayer.getMatchPlayerSlot();
                List<String> currentTrick = game.getCurrentTrick();
                String leadSuit = game.getSuitOfFirstCardInCurrentTrick();
                List<Integer> trickOrder = game.getTrickMatchPlayerSlotOrder();
                int myIndex = trickOrder.indexOf(mySlot);
                List<Integer> playersAfterMe = trickOrder.subList(myIndex + 1, trickOrder.size());

                String safeLosingCard = null;
                String voidingCard = null;
                String safePointDump = null;
                String unavoidableWin = null;

                for (String card : legalCards) {
                    boolean followsSuit = currentTrick.isEmpty() || card.endsWith(leadSuit);

                    boolean wouldCurrentlyWin = currentTrick.stream()
                            .filter(c -> c.endsWith(leadSuit))
                            .allMatch(played -> CardUtils.compareCards(card, played) > 0);

                    if (!followsSuit) {
                        // Off-suit: great for voiding or dumping points
                        if (isPointCard(card) && safePointDump == null) {
                            safePointDump = card;
                        }
                        if (voidingCard == null) {
                            voidingCard = card;
                        }
                        continue;
                    }

                    if (!wouldCurrentlyWin) {
                        // Will not win even currently — safe!
                        if (safeLosingCard == null) {
                            safeLosingCard = card;
                        }
                        continue;
                    }

                    // Card would currently win — can it be beaten later?
                    BitSet holders = getPossibleHolders(card, game, mySlot, gameStatsRepository);
                    boolean canBeBeaten = playersAfterMe.stream().anyMatch(slot -> holders.get(slot - 1));

                    if (canBeBeaten) {
                        // Risky — might not win after all. Treat like safe to lose
                        if (safeLosingCard == null) {
                            safeLosingCard = card;
                        }
                    } else {
                        // Forced to win — choose least damaging
                        if (unavoidableWin == null || CardUtils.compareCards(card, unavoidableWin) < 0) {
                            unavoidableWin = card;
                        }
                    }
                }

                // Decision order: safe loss > void > safe dump > unavoidable win > fallback
                if (safeLosingCard != null) {
                    cardCode = safeLosingCard;
                } else if (voidingCard != null) {
                    cardCode = voidingCard;
                } else if (safePointDump != null) {
                    cardCode = safePointDump;
                } else {
                    cardCode = unavoidableWin != null ? unavoidableWin : legalCards[0];
                }
            }

            case ADA -> {
                int mySlot = matchPlayer.getMatchPlayerSlot();
                List<String> currentTrick = game.getCurrentTrick();
                String leadSuit = game.getSuitOfFirstCardInCurrentTrick();
                List<Integer> trickOrder = game.getTrickMatchPlayerSlotOrder();
                int myIndex = trickOrder.indexOf(mySlot);
                List<Integer> playersAfterMe = trickOrder.subList(myIndex + 1, trickOrder.size());

                boolean attemptingMoonShot = isAttemptingMoonShot(matchPlayer);

                String safePointDump = null;
                String voidingCard = null;

                // Always initialize a fallback in case everything else fails
                String fallbackCard = Arrays.stream(legalCards)
                        .sorted(CardUtils::compareCards)
                        .findFirst()
                        .orElse(legalCards[0]);

                for (String card : legalCards) {
                    boolean followsSuit = currentTrick.isEmpty() || card.endsWith(leadSuit);
                    boolean trickHasPoints = currentTrick.stream().anyMatch(this::isPointCard);
                    boolean isPointCard = isPointCard(card);

                    boolean wouldCurrentlyWin = currentTrick.stream()
                            .filter(c -> c.endsWith(leadSuit))
                            .allMatch(played -> CardUtils.compareCards(card, played) > 0);

                    // Check if others could beat this card
                    BitSet holders = getPossibleHolders(card, game, mySlot, gameStatsRepository);
                    boolean canBeBeaten = playersAfterMe.stream().anyMatch(slot -> holders.get(slot - 1));

                    if (wouldCurrentlyWin && !canBeBeaten && (!trickHasPoints || attemptingMoonShot)) {
                        cardCode = card;
                        break;
                    }

                    if (!wouldCurrentlyWin && isPointCard && safePointDump == null) {
                        safePointDump = card;
                    }

                    if (!followsSuit && voidingCard == null) {
                        voidingCard = card;
                    }

                }
                // Priority order: safe clean win > point dump > void > fallback
                if (safePointDump != null) {
                    cardCode = safePointDump;
                } else if (voidingCard != null) {
                    cardCode = voidingCard;
                } else {
                    cardCode = fallbackCard;
                }
            }
            default -> {
                log.warn("Unknown strategy: {}. Falling back to RANDOM.", strategy);
                cardCode = legalCards[random.nextInt(legalCards.length)];
            }

        }

        log.info("AI Player chooses: {}.", cardCode);
        return cardCode;
    }

    /**
     * Retrieves the set of possible player slots that may still hold the specified
     * card,
     * based on historical game stats and knowledge inferred from card passing.
     * If the requesting player originally passed this card, the knowledge is
     * refined
     * to indicate only the recipient as a possible holder.
     *
     * @param cardCode       the card to check (e.g., "QS", "7H")
     * @param game           the game instance the card belongs to
     * @param requestingSlot the match player slot requesting the information
     *                       (1-based)
     * @param repo           the repository used to fetch card statistics
     * @return a BitSet representing possible holders (0-based index for player
     *         slots)
     */
    public BitSet getPossibleHolders(String cardCode, Game game, int requestingSlot, GameStatsRepository repo) {
        // Retrieve the GameStats record for the card
        GameStats card = repo.findByRankSuitAndGame(cardCode, game);

        // Convert int bitmask to BitSet
        BitSet holders = intToBitSet(card.getPossibleHolders());

        // Apply asymmetric knowledge if this player passed the card
        if (card.getPassedBy() == requestingSlot) {
            holders.clear();
            holders.set(card.getPassedTo() - 1); // BitSet uses 0-based indexing
        }

        return holders;
    }

    /**
     * Converts a 4-bit integer bitmask into a BitSet representing player slots.
     * Each bit corresponds to a player (bit 0 = slot 1, bit 1 = slot 2, etc.).
     *
     * @param mask the integer bitmask where each bit represents a possible card
     *             holder
     * @return a BitSet with bits set for each player slot indicated in the mask
     */
    private BitSet intToBitSet(int mask) {
        BitSet bitSet = new BitSet(4);
        for (int i = 0; i < 4; i++) {
            if ((mask & (1 << i)) != 0) {
                bitSet.set(i);
            }
        }
        return bitSet;
    }

    /**
     * Determines whether the player appears to be attempting to shoot the moon,
     * based on the cards they have taken so far.
     * The heuristic checks if the player has taken the Queen of Spades and at least
     * four hearts.
     *
     * @param player the match player whose taken cards are being evaluated
     * @return true if the player is likely attempting to shoot the moon; false
     *         otherwise
     */
    private boolean isAttemptingMoonShot(MatchPlayer player) {
        String takenString = player.getTakenCards(); // e.g., "QH,AS,5H"
        if (takenString == null || takenString.isEmpty()) {
            return false;
        }
        List<String> taken = CardUtils.splitCardCodesAsListOfStrings(takenString);
        long heartCount = taken.stream().filter(card -> card.endsWith("H")).count();
        boolean hasQS = taken.contains("QS");

        // Simple condition: has QS and at least 4 hearts → maybe moon-shot
        return hasQS && heartCount >= 4;
    }

    /**
     * Determines whether the given card is a point card in the game of Hearts.
     * Point cards are all Hearts and the Queen of Spades.
     *
     * @param cardCode the card code to check (e.g., "7H", "QS")
     * @return true if the card is a point card; false otherwise
     */
    private boolean isPointCard(String cardCode) {
        return cardCode.endsWith("H") || "QS".equals(cardCode);
    }

}
