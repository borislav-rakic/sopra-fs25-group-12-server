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
        Map<Integer, List<PassedCard>> cardsByMatchPlayerSlot = mapPassedCardsByMatchPlayerSlot(passedCards);

        Map<Integer, Integer> passTo = cardRulesService.determinePassingDirection(game.getGameNumber());

        reassignPassedCards(game, cardsByMatchPlayerSlot, passTo);
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

    private Map<Integer, List<PassedCard>> mapPassedCardsByMatchPlayerSlot(List<PassedCard> passedCards) {
        Map<Integer, List<PassedCard>> cardsByMatchPlayerSlot = new HashMap<>();
        for (PassedCard passedCard : passedCards) {
            cardsByMatchPlayerSlot.computeIfAbsent(passedCard.getFromMatchPlayerSlot(), k -> new ArrayList<>())
                    .add(passedCard);
        }
        return cardsByMatchPlayerSlot;
    }

    private void reassignPassedCards(Game game, Map<Integer, List<PassedCard>> cardsByMatchPlayerSlot,
            Map<Integer, Integer> passTo) {
        Match match = game.getMatch();
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

                // Update GameStats: passedBy and passedTo
                GameStats gameStat = gameStatsRepository.findByRankSuitAndGameAndCardHolder(cardCode, game,
                        fromMatchPlayerSlot);
                if (gameStat != null) {
                    gameStat.setPassedBy(fromMatchPlayerSlot);
                    gameStat.setPassedTo(toMatchPlayerSlot);
                    updatedGameStats.add(gameStat); // Collect to batch-save later
                } else {
                    int fromPlayerSlot = fromMatchPlayerSlot - 1;
                    throw new IllegalStateException(
                            "GameStat not found for card: " + cardCode + ", playerSlot: " + fromPlayerSlot);
                }
            }

            // Save sender and receiver after their hand changes
            matchPlayerRepository.save(sender);
            matchPlayerRepository.save(receiver);
        }

        // Save all updated GameStats in one batch at the end
        gameStatsRepository.saveAll(updatedGameStats);
        gameStatsRepository.flush(); // Optional but ensures immediate write

        for (MatchPlayer player : match.getMatchPlayers()) {
            System.out.println(player.getMatchPlayerSlot() + " " + player.getHand());
            if (player.hasCardCodeInHand("2C")) {
                game.setCurrentMatchPlayerSlot(player.getMatchPlayerSlot());
                game.setTrickLeaderMatchPlayerSlot(player.getMatchPlayerSlot());
                log.info(
                        "° After passing the new trick lead (holding 2C) is matchPlayerSlot {}. New trickMatchPlayerSlotOrder: {}.",
                        player.getMatchPlayerSlot(), game.getTrickMatchPlayerSlotOrderAsString());
                System.out.println("° 2C with " + player.getMatchPlayerSlot() + " " + player.getHand());
                System.out.println("° Reassigned starting matchPlayerSlot to player holding 2C (matchPlayerSlot:"
                        + player.getMatchPlayerSlot() + ").");
                break;
            }
        }
        gameRepository.flush();
    }

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

    public void passingAcceptCards(Game game, MatchPlayer matchPlayer, GamePassingDTO passingDTO) {

        List<String> cardsToPass = passingDTO.getCards();
        Match match = matchPlayer.getMatch();

        int matchPlayerSlot = matchPlayer.getMatchPlayerSlot();

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

            int playerSlot = matchPlayerSlot - 1; // client logic.
            if (!matchPlayer.hasCardCodeInHand(cardCode)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Card " + cardCode + " is not owned by player in playerSlot " + playerSlot);
            }
            if (passedCardRepository.existsByGameAndFromMatchPlayerSlotAndRankSuit(game, playerSlot, cardCode)) {
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
        long passedCount = passedCardRepository.countByGame(game);

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

        // If all 12 cards passed, proceed to collect
        if (passedCount == 12) {
            collectPassedCards(game);
            log.info("°°° PASSING CONCLUDED °°°");
            // Transition phase to FIRSTTRICK!
            game.setPhase(GamePhase.FIRSTTRICK);
            game.setCurrentTrickNumber(1);
            gameRepository.save(game);
            log.info("/// READY TO PLAY FIRST TRICK ///");

        }

    }

    private boolean isValidCardFormat(String cardCode) {
        return cardCode != null && cardCode.matches("^[02-9JQKA][HDCS]$");
    }

    public int playerSlotToMatchPlayerSlot(int playerSlot) {
        if (playerSlot < 0 || playerSlot > 3) {
            throw new IllegalArgumentException("Invalid playerSlot: " + playerSlot + ". Expected 0–3.");
        }
        return playerSlot + 1;
    }

    public int matchPlayerSlotToPlayerSlot(int matchPlayerSlot) {
        if (matchPlayerSlot < 1 || matchPlayerSlot > 4) {
            throw new IllegalArgumentException("Invalid matchPlayerSlot: " + matchPlayerSlot + ". Expected 1–4.");
        }
        return matchPlayerSlot - 1;
    }

}
