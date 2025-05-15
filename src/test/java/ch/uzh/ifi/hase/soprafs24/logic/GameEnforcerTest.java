package ch.uzh.ifi.hase.soprafs24.logic;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GameEnforcerTest {

    private Match match;
    private GameRepository mockRepo;

    @BeforeEach
    public void setup() {
        match = new Match();
        match.setMatchId(1L);
        mockRepo = mock(GameRepository.class);
    }

    private Game createGame(GamePhase phase) {
        Game game = new Game();
        game.setPhase(phase);
        return game;
    }

    // In-memory tests

    @Test
    public void test_getOnlyActiveGameOrNull_returnsNullWhenNoActive() {
        match.setGames(List.of(
                createGame(GamePhase.FINISHED),
                createGame(GamePhase.ABORTED)));
        assertNull(GameEnforcer.getOnlyActiveGameOrNull(match));
    }

    @Test
    public void test_getOnlyActiveGameOrNull_returnsSingleActive() {
        Game active = createGame(GamePhase.PASSING);
        match.setGames(List.of(
                createGame(GamePhase.FINISHED),
                active));
        assertEquals(active, GameEnforcer.getOnlyActiveGameOrNull(match));
    }

    @Test
    public void test_getOnlyActiveGameOrNull_throwsWhenMultipleActive() {
        match.setGames(List.of(
                createGame(GamePhase.PASSING),
                createGame(GamePhase.NORMALTRICK)));
        assertThrows(IllegalStateException.class, () -> GameEnforcer.getOnlyActiveGameOrNull(match));
    }

    @Test
    public void test_requireExactlyOneActiveGame_throwsWhenNone() {
        match.setGames(new ArrayList<>());
        assertThrows(IllegalStateException.class, () -> GameEnforcer.requireExactlyOneActiveGame(match));
    }

    @Test
    public void test_assertNoActiveGames_passesIfNone() {
        match.setGames(List.of(createGame(GamePhase.FINISHED)));
        assertDoesNotThrow(() -> GameEnforcer.assertNoActiveGames(match));
    }

    @Test
    public void test_assertNoActiveGames_throwsIfOneActive() {
        match.setGames(List.of(createGame(GamePhase.PASSING)));
        assertThrows(ResponseStatusException.class, () -> GameEnforcer.assertNoActiveGames(match));
    }

    // Repository-based tests

    @Test
    public void test_getOnlyActiveGameOrNull_repo_returnsNull() {
        when(mockRepo.findActiveGamesByMatchId(1L)).thenReturn(List.of());
        assertNull(GameEnforcer.getOnlyActiveGameOrNull(1L, mockRepo));
    }

    @Test
    public void test_getOnlyActiveGameOrNull_repo_returnsOne() {
        Game game = new Game();
        when(mockRepo.findActiveGamesByMatchId(1L)).thenReturn(List.of(game));
        assertEquals(game, GameEnforcer.getOnlyActiveGameOrNull(1L, mockRepo));
    }

    @Test
    public void test_getOnlyActiveGameOrNull_repo_throwsMultiple() {
        when(mockRepo.findActiveGamesByMatchId(1L))
                .thenReturn(List.of(new Game(), new Game()));
        assertThrows(IllegalStateException.class,
                () -> GameEnforcer.getOnlyActiveGameOrNull(1L, mockRepo));
    }

    @Test
    public void test_requireExactlyOneActiveGame_repo_throwsWhenNone() {
        when(mockRepo.findActiveGamesByMatchId(1L)).thenReturn(List.of());
        assertThrows(IllegalStateException.class,
                () -> GameEnforcer.requireExactlyOneActiveGame(1L, mockRepo));
    }

    @Test
    public void test_assertNoActiveGames_repo_passesIfNone() {
        when(mockRepo.findActiveGamesByMatchId(1L)).thenReturn(List.of());
        assertDoesNotThrow(() -> GameEnforcer.assertNoActiveGames(1L, mockRepo));
    }

    @Test
    public void test_assertNoActiveGames_repo_throwsIfActiveExists() {
        when(mockRepo.findActiveGamesByMatchId(1L))
                .thenReturn(List.of(createGame(GamePhase.NORMALTRICK)));
        assertThrows(ResponseStatusException.class,
                () -> GameEnforcer.assertNoActiveGames(1L, mockRepo));
    }
}
