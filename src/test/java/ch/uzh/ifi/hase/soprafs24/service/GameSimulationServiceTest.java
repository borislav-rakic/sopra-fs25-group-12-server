package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameStatsRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GameSimulationServiceTest {

    @Mock
    private CardRulesService cardRulesService;

    @Mock
    private GameService gameService;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameStatsRepository gameStatsRepository;

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @InjectMocks
    private GameSimulationService gameSimulationService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    private Match createMockMatchWithPlayers() {
        Match match = new Match();
        List<MatchPlayer> players = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            MatchPlayer mp = new MatchPlayer();
            mp.setMatchPlayerSlot(i + 1);
            mp.setMatchScore(0);
            players.add(mp);
        }
        match.setMatchPlayers(players);
        match.setMatchGoal(100);
        return match;
    }

    @Test
    public void testAutoPlayToLastTrickOfGame_setsFinalTrickState() {
        Game game = new Game();
        Match match = createMockMatchWithPlayers();

        when(gameRepository.saveAndFlush(any())).thenReturn(game);
        when(gameRepository.save(any())).thenReturn(game);

        gameSimulationService.autoPlayToLastTrickOfGame(match, game, 0);

        assertEquals(GamePhase.FINALTRICK, game.getPhase());
        assertEquals(13, game.getCurrentTrickNumber());
        assertEquals(1, game.getCurrentMatchPlayerSlot());
        assertEquals(1, game.getTrickLeaderMatchPlayerSlot());
        assertEquals(48, game.getCurrentPlayOrder());
        assertTrue(game.getHeartsBroken());
        assertNotNull(game.getCurrentTrick());
        assertEquals(0, game.getCurrentTrick().size());

        // Verify players were saved
        verify(matchPlayerRepository, times(4)).save(any(MatchPlayer.class));
        verify(gameRepository, atLeastOnce()).saveAndFlush(game);
    }
}
