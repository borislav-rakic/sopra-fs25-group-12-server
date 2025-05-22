package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.GameConstants;
import ch.uzh.ifi.hase.soprafs24.constant.Strategy;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.PassedCard;
import ch.uzh.ifi.hase.soprafs24.exceptions.GameplayException;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameStatsRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.PassedCardRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePassingDTO;
import ch.uzh.ifi.hase.soprafs24.util.CardUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Qualifier("cardPassingService")
public class CardPassingService {

    private final AiPassingService aiPassingService;
    private final CardRulesService cardRulesService;
    private final GameRepository gameRepository;
    private final GameStatsRepository gameStatsRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final PassedCardRepository passedCardRepository;

    @Autowired
    public CardPassingService(
            AiPassingService aiPassingService,
            CardRulesService cardRulesService,
            GameRepository gameRepository,
            GameStatsRepository gameStatsRepository,
            MatchPlayerRepository matchPlayerRepository,
            PassedCardRepository passedCardRepository) {
        this.aiPassingService = aiPassingService;
        this.cardRulesService = cardRulesService;
        this.gameRepository = gameRepository;
        this.gameStatsRepository = gameStatsRepository;
        this.matchPlayerRepository = matchPlayerRepository;
        this.passedCardRepository = passedCardRepository;
    }

    private final Logger log = LoggerFactory.getLogger(CardPassingService.class);

    /**
     * """
     * 
     * """
     *
     **/

    /**
     * Collects and reassigns all passed cards in a game according to the current
     * round's passing direction.
     * Ensures that all players have submitted their passed cards before proceeding.
     * Once reassigned, the original passed card records are deleted from the
     * repository.
     *
     * @param game the game for which cards are being collected
     * @throws ResponseStatusException if the game or its match is not found,
     *                                 or if not all players have passed the
     *                                 required number of cards
     */
    public void collectPassedCards(Game game) {
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found");
        }
        Match match = game.getMatch();
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }
        validateAllCardsPassed(match, game);

        log.info("Checking cards distribution BEFORE passing.");
        cardRulesService.validateUniqueDeckAcrossPlayers(match);

        List<PassedCard> passedCards = passedCardRepository.findByGame(game);
        Map<Integer, List<PassedCard>> cardsByMatchPlayerSlot = mapPassedCardsByMatchPlayerSlot(passedCards);

        Map<Integer, Integer> passTo = cardRulesService.determinePassingDirection(game.getGameNumber());

        reassignPassedCards(game, cardsByMatchPlayerSlot, passTo);
        // Delete passed cards
        passedCardRepository.deleteAll(passedCards);
        passedCardRepository.flush();

    }

    /**
     * Validates that all players in the match have passed the required number of
     * cards.
     * For a 4-player match, this should be exactly 12 passed cards (3 per player).
     *
     * @param match the match to validate
     * @param game  the game in which the card passing occurred
     * @throws MatchplayException if the total number of passed cards is not 12
     */
    private void validateAllCardsPassed(Match match, Game game) {
        List<PassedCard> passedCards = passedCardRepository.findByGame(game);

        if (passedCards.size() != 12) {
            throw new GameplayException("Not all players have finished passing cards.");
        }
    }

    /**
     * Groups the given list of passed cards by the slot of the player who passed
     * them.
     *
     * @param passedCards the list of passed cards to organize
     * @return a map where the key is the match player slot (1–4) and the value is
     *         the list of cards passed by that player
     */
    private Map<Integer, List<PassedCard>> mapPassedCardsByMatchPlayerSlot(List<PassedCard> passedCards) {
        Map<Integer, List<PassedCard>> cardsByMatchPlayerSlot = new HashMap<>();
        for (PassedCard passedCard : passedCards) {
            cardsByMatchPlayerSlot.computeIfAbsent(passedCard.getFromMatchPlayerSlot(), k -> new ArrayList<>())
                    .add(passedCard);
        }
        return cardsByMatchPlayerSlot;
    }

    /**
     * Reassigns passed cards to their target players based on the current game's
     * passing direction.
     * Updates each player's hand accordingly, adjusts game statistics (who passed
     * what to whom),
     * and persists the changes to the database.
     *
     * @param game                   the game in which cards are being reassigned
     * @param cardsByMatchPlayerSlot map of cards grouped by the player slot that
     *                               passed them
     * @param passTo                 a map defining the passing direction: from each
     *                               player slot to a target slot
     * @throws IllegalStateException if a passing direction is invalid
     * @throws GamepplayException    if a GameStat
     *                               entry is missing
     */
    private void reassignPassedCards(Game game, Map<Integer, List<PassedCard>> cardsByMatchPlayerSlot,
            Map<Integer, Integer> passTo) {
        List<GameStats> updatedGameStats = new ArrayList<>(); // Collect for batch update

        for (Map.Entry<Integer, List<PassedCard>> entry : cardsByMatchPlayerSlot.entrySet()) {
            int fromMatchPlayerSlot = entry.getKey();
            Integer toMatchPlayerSlot = passTo.get(fromMatchPlayerSlot);

            if (toMatchPlayerSlot == null || toMatchPlayerSlot < 1 || toMatchPlayerSlot > 4) {
                int fromPlayerSlot = (int) fromMatchPlayerSlot;
                throw new IllegalStateException("Invalid passing target playerSlot for playerSlot: " + fromPlayerSlot);
            }

            MatchPlayer sender = findMatchPlayer(game, fromMatchPlayerSlot);
            MatchPlayer receiver = findMatchPlayer(game, toMatchPlayerSlot);

            for (PassedCard card : entry.getValue()) {
                String cardCode = card.getRankSuit();

                // Remove the card from sender
                sender.removeCardCodeFromHand(cardCode);

                // Add the card to receiver
                receiver.addCardCodeToHand(cardCode);
                receiver.setHand(CardUtils.normalizeCardCodeString(receiver.getHand()));
                // Update GameStats: passedBy and passedTo
                GameStats gameStat = gameStatsRepository.findByRankSuitAndGameAndCardHolder(cardCode, game,
                        fromMatchPlayerSlot);
                if (gameStat != null) {
                    gameStat.setPassedBy(fromMatchPlayerSlot);
                    gameStat.setPassedTo(toMatchPlayerSlot);
                    updatedGameStats.add(gameStat); // Collect to batch-save later
                } else {
                    throw new GameplayException("Card passing failed: no tracking data for " + cardCode);
                }
            }

            receiver.setHand(CardUtils.normalizeCardCodeString(receiver.getHand()));
            sender.setHand(CardUtils.normalizeCardCodeString(sender.getHand()));

            // Save sender and receiver after their hand changes
            matchPlayerRepository.save(sender);
            matchPlayerRepository.save(receiver);
        }

        // Save all updated GameStats in one batch at the end
        gameStatsRepository.saveAll(updatedGameStats);
        gameStatsRepository.flush(); // Optional but ensures immediate write
        gameRepository.flush();
    }

    /**
     * Finds the MatchPlayer in the game by their match player slot (1-based,
     * server-side convention).
     * This method maps frontend slot indices (0–3) to internal match player slots
     * (1–4).
     *
     * @param game            the game containing the match and its players
     * @param matchPlayerSlot the match player slot (1–4)
     * @return the MatchPlayer corresponding to the given slot
     * @throws IllegalArgumentException if the slot is not between 1 and 4
     * @throws IllegalStateException    if no MatchPlayer is found for the given
     *                                  slot
     */
    private MatchPlayer findMatchPlayer(Game game, int matchPlayerSlot) {
        if (matchPlayerSlot < 1 || matchPlayerSlot > 4) {
            int playerSlot = matchPlayerSlot - 1;
            throw new IllegalArgumentException("PlayerSlot must be between 0 and 3. Got: " + playerSlot);
        }
        int playerSlot = matchPlayerSlot - 1;
        return game.getMatch().getMatchPlayers().stream()
                .filter(mp -> mp.getMatchPlayerSlot() == matchPlayerSlot)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No MatchPlayer found for playerSlot " + playerSlot));
    }

    /**
     * Handles the logic for a player passing three cards during the passing phase
     * of a game.
     * Validates the input (card format, ownership, duplicates, etc.), persists the
     * passed cards,
     * and triggers AI players to pass their cards if all human players have
     * completed their pass.
     *
     * @param game         the current game where the passing is occurring
     * @param matchPlayer  the player attempting to pass cards
     * @param passingDTO   the DTO containing the selected cards to pass
     * @param pickRandomly if true, selects cards automatically using a predefined
     *                     AI strategy
     * @return the total number of cards passed so far in the game (should reach 12
     *         when all passes are complete)
     *
     * @throws ResponseStatusException if:
     *                                 - fewer or more than 3 cards are passed,
     *                                 - duplicate cards are selected,
     *                                 - an invalid card format is used,
     *                                 - the player attempts to pass cards they
     *                                 don't own,
     *                                 - the same card is passed multiple times
     */
    public int passingAcceptCards(Game game, MatchPlayer matchPlayer, GamePassingDTO passingDTO,
            Boolean pickRandomly) {
        List<String> cardsToPass;
        if (pickRandomly) {
            cardsToPass = aiPassingService.selectCardsToPass(matchPlayer, Strategy.LEFTMOST);
        } else {
            cardsToPass = passingDTO.getCards();
        }

        Match match = matchPlayer.getMatch();

        int matchPlayerSlot = matchPlayer.getMatchPlayerSlot();
        int playerSlot = matchPlayerSlot - 1; // client logic.

        int alreadyPassed = passedCardRepository
                .countByGameAndFromMatchPlayerSlotAndGameNumber(game, matchPlayerSlot, game.getGameNumber());

        if (alreadyPassed > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already passed cards this round.");
        }

        if (cardsToPass == null || cardsToPass.size() != 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exactly 3 cards must be passed.");
        }

        // Check for duplicate cards in the selection
        long distinctCount = cardsToPass.stream().distinct().count();
        if (distinctCount != cardsToPass.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Duplicate cards selected for passing are not allowed.");
        }

        for (String cardCode : cardsToPass) {
            if (!isValidCardFormat(cardCode)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid card format: " + cardCode);
            }

            if (!matchPlayer.hasCardCodeInHand(cardCode)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Card " + cardCode + " is not owned by player in playerSlot " + playerSlot);
            }
            if (passedCardRepository.existsByGameAndFromMatchPlayerSlotAndRankSuit(game, matchPlayerSlot, cardCode)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Card " + cardCode + " has already been passed by playerSlot " + playerSlot);
            }

            if (passedCardRepository.existsByGameAndRankSuit(game, cardCode)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Card " + cardCode + " has already been passed by another player.");
            }
        }

        // Save all passed cards in a batch
        List<PassedCard> passedCards = cardsToPass.stream()
                .map(cardCode -> new PassedCard(game, cardCode, matchPlayerSlot, game.getGameNumber()))
                .collect(Collectors.toList());
        passedCardRepository.saveAll(passedCards);
        passedCardRepository.flush();
        log.info("° MatchPlayer {} is passing cards {} from his hand {}. {}", matchPlayer.getInfo(),
                passingDTO.getCardsAsString(), matchPlayer.getHand(),
                cardRulesService.describePassingDirection(game.getGameNumber(), matchPlayer.getMatchPlayerSlot()));
        // Count how many cards have been passed in total
        int passedCount = passedCardRepository.countByGame(game);

        // If not all 12 passed yet, check if AI players need to pass
        if (passedCount < 12) {
            int expectedHumanPasses = (int) match.getMatchPlayers().stream()
                    .filter(mp -> mp.getMatchPlayerSlot() >= 1 && mp.getMatchPlayerSlot() <= 4)
                    .filter(mp -> mp.getUser() != null && Boolean.FALSE.equals(mp.getUser().getIsAiPlayer()))
                    .map(mp -> mp.getMatchPlayerSlot()) // map by matchPlayer
                    .distinct() // unique matchPlayer only
                    .count() * 3;
            if (passedCount == expectedHumanPasses) {
                aiPassingService.passForAllAiPlayers(game);
                passedCount = passedCardRepository.countByGame(game);
            }
        }
        return passedCount;

    }

    /**
     * If the game is in the PASSING phase and fewer than 12 cards have been passed,
     * and all human players have completed their passing,
     * this method triggers AI players to pass their cards.
     */
    public void maybeTriggerAiPassing(Game game) {
        if (game == null || game.getMatch() == null) {
            log.warn("maybeTriggerAiPassing: game or match is null");
            return;
        }

        Match match = game.getMatch();
        int totalPassed = passedCardRepository.countByGame(game);

        if (totalPassed >= 12) {
            return; // Passing already complete
        }

        int expectedHumanPasses = (int) match.getMatchPlayers().stream()
                .filter(mp -> mp.getUser() != null && !Boolean.TRUE.equals(mp.getUser().getIsAiPlayer()))
                .count() * 3;

        if (totalPassed == expectedHumanPasses) {
            log.info("All human passes complete. Triggering AI passing.");
            aiPassingService.passForAllAiPlayers(game);
        } else {
            log.info("maybeTriggerAiPassing: Not all human players have passed yet.");
        }
    }

    /**
     * Validates whether the given card code matches the expected card format.
     * Uses a regular expression defined in {@code GameConstants.CARD_CODE_REGEX}.
     *
     * @param cardCode the card code to validate (e.g., "7H", "QS")
     * @return true if the card code is non-null and matches the expected format;
     *         false otherwise
     */
    private boolean isValidCardFormat(String cardCode) {
        return cardCode != null && cardCode.matches(GameConstants.CARD_CODE_REGEX);
    }

    /**
     * Converts a 0-based player slot index (used by the frontend) to a 1-based
     * match player slot index (used server-side).
     *
     * @param playerSlot the 0-based player slot index (range: 0–3)
     * @return the corresponding 1-based match player slot (range: 1–4)
     * @throws IllegalArgumentException if the input is not between 0 and 3
     */
    public int playerSlotToMatchPlayerSlot(int playerSlot) {
        if (playerSlot < 0 || playerSlot > 3) {
            throw new IllegalArgumentException("Invalid playerSlot: " + playerSlot + ". Expected 0–3.");
        }
        return playerSlot + 1;
    }

    /**
     * Converts a 1-based match player slot index (used server-side) to a 0-based
     * player slot index (used by the frontend).
     *
     * @param matchPlayerSlot the 1-based match player slot (range: 1–4)
     * @return the corresponding 0-based player slot index (range: 0–3)
     * @throws IllegalArgumentException if the input is not between 1 and 4
     */
    public int matchPlayerSlotToPlayerSlot(int matchPlayerSlot) {
        if (matchPlayerSlot < 1 || matchPlayerSlot > 4) {
            throw new IllegalArgumentException("Invalid matchPlayerSlot: " + matchPlayerSlot + ". Expected 1–4.");
        }
        return matchPlayerSlot - 1;
    }

}
