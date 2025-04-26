package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.PassedCard;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.PassedCardRepository;

@Service
public class CardPassingService {

    private final PassedCardRepository passedCardRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final CardRulesService cardRulesService;

    @Autowired
    public CardPassingService(PassedCardRepository passedCardRepository,
            GameStatsService gameStatsService,
            MatchPlayerRepository matchPlayerRepository,
            CardRulesService cardRulesService,
            AiPassingService aiPassingService,
            GameRepository gameRepository) {
        this.passedCardRepository = passedCardRepository;
        this.matchPlayerRepository = matchPlayerRepository;
        this.cardRulesService = cardRulesService;
    }

    public void collectPassedCards(Game game) {
        validateAllCardsPassed(game);

        List<PassedCard> passedCards = passedCardRepository.findByGame(game);
        Map<Integer, List<PassedCard>> cardsBySlot = mapPassedCardsBySlot(passedCards);

        Map<Integer, Integer> passTo = cardRulesService.determinePassingDirection(game.getGameNumber());
        reassignPassedCards(game, cardsBySlot, passTo);

        passedCardRepository.deleteAll(passedCards);
    }

    private void validateAllCardsPassed(Game game) {
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
        for (Map.Entry<Integer, List<PassedCard>> entry : cardsBySlot.entrySet()) {
            int fromSlot = entry.getKey();
            int toSlot = passTo.get(fromSlot);

            for (PassedCard card : entry.getValue()) {
                MatchPlayer receiver = findMatchPlayer(game, toSlot);
                receiver.addCardToHand(card.getRankSuit());
                matchPlayerRepository.save(receiver);
            }
        }
    }

    private MatchPlayer findMatchPlayer(Game game, int slot) {
        return game.getMatch().getMatchPlayers().stream()
                .filter(mp -> mp.getSlot() == slot)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No MatchPlayer found for slot " + slot));
    }

}
