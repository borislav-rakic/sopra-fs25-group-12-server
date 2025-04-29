package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.controller.MatchController;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.PassedCard;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameStatsRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.PassedCardRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePassingDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
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

    public void collectPassedCards(Game game) {
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found");
        }
        Match match = game.getMatch();
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }
        validateAllCardsPassed(match, game);

        List<PassedCard> passedCards = passedCardRepository.findByGame(game);
        Map<Integer, List<PassedCard>> cardsBySlot = mapPassedCardsBySlot(passedCards);

        Map<Integer, Integer> passTo = cardRulesService.determinePassingDirection(game.getGameNumber());

        reassignPassedCards(game, cardsBySlot, passTo);
        // Delete passed cards
        passedCardRepository.deleteAll(passedCards);
        passedCardRepository.flush();
    }

    private void validateAllCardsPassed(Match match, Game game) {
        List<PassedCard> passedCards = passedCardRepository.findByGame(game);

        if (passedCards.size() != 12) {
            throw new IllegalStateException("Cannot collect cards: not all cards have been passed yet.");
        }
    }

    private Map<Integer, List<PassedCard>> mapPassedCardsBySlot(List<PassedCard> passedCards) {
        Map<Integer, List<PassedCard>> cardsBySlot = new HashMap<>();
        for (PassedCard passed : passedCards) {
            cardsBySlot.computeIfAbsent(passed.getFromSlot(), k -> new ArrayList<>()).add(passed);
        }
        return cardsBySlot;
    }

    private void reassignPassedCards(Game game, Map<Integer, List<PassedCard>> cardsBySlot,
            Map<Integer, Integer> passTo) {
        Match match = game.getMatch();
        List<GameStats> updatedGameStats = new ArrayList<>(); // Collect for batch update

        for (Map.Entry<Integer, List<PassedCard>> entry : cardsBySlot.entrySet()) {
            int fromSlot = entry.getKey();
            Integer toSlot = passTo.get(fromSlot);

            if (toSlot == null || toSlot < 1 || toSlot > 4) {
                throw new IllegalStateException("Invalid passing target slot for slot: " + fromSlot);
            }

            MatchPlayer sender = findMatchPlayer(game, fromSlot);
            MatchPlayer receiver = findMatchPlayer(game, toSlot);

            for (PassedCard card : entry.getValue()) {
                String cardCode = card.getRankSuit();

                // Remove the card from sender
                sender.removeCardCodeFromHand(cardCode);

                // Add the card to receiver
                receiver.addCardCodeToHand(cardCode);

                // Update GameStats: passedBy and passedTo
                GameStats gameStat = gameStatsRepository.findByRankSuitAndGameAndCardHolder(cardCode, game, fromSlot);
                if (gameStat != null) {
                    gameStat.setPassedBy(fromSlot);
                    gameStat.setPassedTo(toSlot);
                    updatedGameStats.add(gameStat); // Collect to batch-save later
                } else {
                    throw new IllegalStateException("GameStat not found for card: " + cardCode + ", slot: " + fromSlot);
                }
            }

            // Save sender and receiver after their hand changes
            matchPlayerRepository.save(sender);
            matchPlayerRepository.save(receiver);
        }

        // Save all updated GameStats in one batch at the end
        gameStatsRepository.saveAll(updatedGameStats);
        gameStatsRepository.flush(); // Optional but ensures immediate write

        // Set the starting player (holding 2C)
        for (MatchPlayer player : match.getMatchPlayers()) {
            System.out.println(player.getSlot() + " " + player.getHand());
            if (player.hasCardCodeInHand("2C")) {
                game.setCurrentSlot(player.getSlot());
                game.setTrickLeaderSlot(player.getSlot());
                List<Integer> slots = game.getCurrentTrickSlots();
                String slotsAsString = slots.stream().map(String::valueOf).collect(Collectors.joining(","));
                log.info("=+=+=+=Order of Slots based on 2C at {}: {}.", player.getSlot(), slotsAsString);
                System.out.println("2C with " + player.getSlot() + " " + player.getHand());
                System.out.println("Reassigned starting slot to player holding 2C (slot:" + player.getSlot() + ").");
                break;
            }
        }
    }

    private MatchPlayer findMatchPlayer(Game game, int slot) {
        if (slot < 1 || slot > 4) {
            throw new IllegalArgumentException("Slot must be between 1 and 4. Got: " + slot);
        }
        return game.getMatch().getMatchPlayers().stream()
                .filter(mp -> mp.getSlot() == slot)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No MatchPlayer found for slot " + slot));
    }

    public void passingAcceptCards(Game game, MatchPlayer matchPlayer, GamePassingDTO passingDTO) {

        List<String> cardsToPass = passingDTO.getCards();
        Match match = matchPlayer.getMatch();

        int slot = matchPlayer.getSlot();

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
                        "Card " + cardCode + " is not owned by player in slot " + slot);
            }
            if (passedCardRepository.existsByGameAndFromSlotAndRankSuit(game, slot, cardCode)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Card " + cardCode + " has already been passed by slot " + slot);
            }

            if (passedCardRepository.existsByGameAndRankSuit(game, cardCode)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Card " + cardCode + " has already been passed by another player.");
            }
        }

        // Save all passed cards in a batch
        List<PassedCard> passedCards = cardsToPass.stream()
                .map(cardCode -> new PassedCard(game, cardCode, slot, game.getGameNumber()))
                .collect(Collectors.toList());
        passedCardRepository.saveAll(passedCards);
        passedCardRepository.flush();
        log.info("MatchPlayer {} is passing cards {} from his hand {}. {}", matchPlayer.getInfo(),
                passingDTO.getCardsAsString(), matchPlayer.getHand(),
                cardRulesService.describePassingDirection(game.getGameNumber(), matchPlayer.getSlot()));
        // Count how many cards have been passed in total
        long passedCount = passedCardRepository.countByGame(game);

        // If not all 12 passed yet, check if AI players need to pass
        if (passedCount < 12) {
            int expectedHumanPasses = (int) match.getMatchPlayers().stream()
                    .filter(mp -> mp.getSlot() >= 1 && mp.getSlot() <= 4)
                    .filter(mp -> mp.getUser() != null && Boolean.FALSE.equals(mp.getUser().getIsAiPlayer()))
                    .map(mp -> mp.getSlot()) // map by slot
                    .distinct() // unique slots only
                    .count() * 3;
            if (passedCount == expectedHumanPasses) {
                aiPassingService.passForAllAiPlayers(game);
                passedCount = passedCardRepository.countByGame(game);
            }
        }

        // If all 12 cards passed, proceed to collect
        if (passedCount == 12) {
            collectPassedCards(game);
            // Transition phase to FIRSTTRICK!
            game.setPhase(GamePhase.FIRSTTRICK);
            game.setCurrentTrickNumber(1);
            gameRepository.save(game);

        }

    }

    private boolean isValidCardFormat(String cardCode) {
        return cardCode != null && cardCode.matches("^[02-9JQKA][HDCS]$");
    }

}
