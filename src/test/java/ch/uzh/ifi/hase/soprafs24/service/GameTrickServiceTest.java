package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.TrickPhase;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.repository.GameStatsRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameTrickServiceTest {

    private MatchPlayerRepository matchPlayerRepository;
    private CardRulesService cardRulesService;
    private GameStatsRepository gameStatsRepository;
    private GameTrickService gameTrickService;

    private Game game;
    private Match match;
    private MatchPlayer player;

    @BeforeEach
    void setup() {
        matchPlayerRepository = mock(MatchPlayerRepository.class);
        cardRulesService = mock(CardRulesService.class);
        gameStatsRepository = mock(GameStatsRepository.class);

        gameTrickService = new GameTrickService(matchPlayerRepository, cardRulesService, gameStatsRepository);

        game = new Game();
        game.setCurrentPlayOrder(0);
        game.setCurrentTrick(new ArrayList<>());
        game.setPhase(GamePhase.PASSING); // initial phase
        match = new Match();
        player = new MatchPlayer();
        player.setMatchPlayerSlot(1);
    }

    @Test
    void addCardToTrick_updatesTrickAndPlayOrder() {
        gameTrickService.addCardToTrick(match, game, player, "QS");

        assertEquals(1, game.getCurrentPlayOrder());
        assertEquals(1, game.getCurrentTrick().size());
        assertEquals("QS", game.getCurrentTrick().get(0));
    }

    @Test
    void updateGamePhaseBasedOnPlayOrder_setsCorrectPhase_firstTrick() {
        game.setCurrentPlayOrder(1);
        game.setPhase(GamePhase.PASSING);

        gameTrickService.updateGamePhaseBasedOnPlayOrder(game);

        assertEquals(GamePhase.FIRSTTRICK, game.getPhase());
        assertEquals(TrickPhase.READYFORFIRSTCARD, game.getTrickPhase());
    }

    @Test
    void updateGamePhaseBasedOnPlayOrder_setsCorrectPhase_finalTrick() {
        game.setCurrentPlayOrder(49);
        game.setPhase(GamePhase.NORMALTRICK);

        gameTrickService.updateGamePhaseBasedOnPlayOrder(game);

        assertEquals(GamePhase.FINALTRICK, game.getPhase());
    }

    @Test
    void updateGamePhaseBasedOnPlayOrder_setsResultPhase() {
        game.setCurrentPlayOrder(53);
        game.setPhase(GamePhase.FINALTRICK);

        gameTrickService.updateGamePhaseBasedOnPlayOrder(game);

        assertEquals(GamePhase.RESULT, game.getPhase());
    }
}
