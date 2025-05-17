package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.model.DrawCardResponse;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GameSetupServiceTest {
    @Mock
    private ExternalApiClientService externalApiClientService = Mockito.mock(ExternalApiClientService.class);

    @Mock
    private GameStatsService gameStatsService = Mockito.mock(GameStatsService.class);

    @Mock
    private MatchPlayerRepository matchPlayerRepository = Mockito.mock(MatchPlayerRepository.class);

    @Mock
    private MatchRepository matchRepository = Mockito.mock(MatchRepository.class);

    @Mock
    private GameRepository gameRepository = Mockito.mock(GameRepository.class);

    @InjectMocks
    private GameSetupService gameSetupService = new GameSetupService(
            externalApiClientService,
            gameStatsService,
            matchPlayerRepository);

    Match match;
    Game game;

    @BeforeEach
    public void setup() {
        User user1 = new User();
        user1.setId(1L);
        User user2 = new User();
        user2.setId(2L);
        User user3 = new User();
        user3.setId(3L);
        User user4 = new User();
        user4.setId(4L);

        MatchPlayer matchPlayer1 = new MatchPlayer();
        matchPlayer1.setUser(user1);
        MatchPlayer matchPlayer2 = new MatchPlayer();
        matchPlayer2.setUser(user2);
        MatchPlayer matchPlayer3 = new MatchPlayer();
        matchPlayer3.setUser(user3);
        MatchPlayer matchPlayer4 = new MatchPlayer();
        matchPlayer4.setUser(user4);

        game = new Game();
        game.setGameId(1L);
        game.setDeckId("1234");

        match = new Match();
        match.setMatchId(1L);
        match.getGames().add(game);
        match.getMatchPlayers().add(matchPlayer1);
        match.getMatchPlayers().add(matchPlayer2);
        match.getMatchPlayers().add(matchPlayer3);
        match.getMatchPlayers().add(matchPlayer4);
    }

    @Test
    public void testDetermineNextGameNumber() {
        // Create mock Game objects
        Match match = mock(Match.class);
        Game game1 = mock(Game.class);
        when(game1.getGameNumber()).thenReturn(1);
        Game game2 = mock(Game.class);
        when(game2.getGameNumber()).thenReturn(2);

        // Mocking match.getGames() to return the list of mock games
        when(match.getGames()).thenReturn(Arrays.asList(game1, game2));

        // Calculate next game number
        int nextGameNumber = gameSetupService.determineNextGameNumber(match);

        assertEquals(3, nextGameNumber); // We expect the next game number to be 3
    }

    @Test
    void testCreateAndStartGameForMatch_Failure_ActiveGameExists() {
        // Arrange
        Match match = new Match();
        match.setPhase(MatchPhase.BEFORE_GAMES);
        match.setMatchId(42L);

        Game activeGame = new Game();
        activeGame.setPhase(GamePhase.NORMALTRICK); // Simulate an active game
        match.setGames(List.of(activeGame));

        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            gameSetupService.createAndStartGameForMatch(match, matchRepository, gameRepository, null);
        });

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    public void testFetchAndDistributeCardsAsync() {
        // Simulating a RuntimeException for API failure
        RuntimeException apiError = new RuntimeException("Simulated exception");

        // Mocking the external API client to return an error
        given(externalApiClientService.createNewDeck()).willReturn(Mono.error(apiError));

        // Mocking the necessary repository methods
        given(matchRepository.findMatchByMatchId(Mockito.anyLong())).willReturn(match);
        given(gameRepository.findWaitingGameByMatchid(Mockito.anyLong())).willReturn(match.getGames());

        // Simulate the fetch and distribute cards method
        gameSetupService.fetchDeckAndDistributeCardsAsync(matchRepository, gameRepository, 1L);
    }

    @Test
    public void testFetchDeckAndDistributeCardsAsync_Failure() {
        // Simulate API failure
        given(externalApiClientService.createNewDeck())
                .willReturn(Mono.error(new RuntimeException("Simulated API failure")));

        // Mock repository
        when(matchRepository.findMatchByMatchId(anyLong())).thenReturn(match);
        when(gameRepository.findWaitingGameByMatchid(anyLong())).thenReturn(Arrays.asList(game));

        // Test asynchronous method with API failure
        gameSetupService.fetchDeckAndDistributeCardsAsync(matchRepository, gameRepository, 1L);

        // Verify the error handling
        verify(externalApiClientService, times(1)).createNewDeck(); // API call should still happen
        assertNotNull(game.getDeckId()); // A fallback deck ID should be set
    }

    @Test
    void testDistributeCards() {
        // Create mock Game and Match objects
        Game game = mock(Game.class);
        Match match = mock(Match.class);

        // Mock the externalApiClientService to throw an exception
        Mono<DrawCardResponse> drawCardResponseMono = Mono.error(new RuntimeException("Simulated exception"));
        when(externalApiClientService.drawCard(anyString(), anyInt())).thenReturn(drawCardResponseMono);

        // Call the method under test and handle the error
        assertThrows(RuntimeException.class, () -> {
            gameSetupService.distributeCards(match, game, matchRepository, gameRepository, null);
        });
    }

}
