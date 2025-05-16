package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.MatchSummary;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class MatchServiceExpandedTest {

    @Mock
    private GameRepository gameRepository;
    @Mock
    private GameService gameService;
    @Mock
    private GameSimulationService gameSimulationService;
    @Mock
    private GameSetupService gameSetupService;
    @Mock
    private MatchSummaryService matchSummaryService;
    @Mock
    private MatchPlayerRepository matchPlayerRepository;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private PollingService pollingService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MatchSetupService matchSetupService;

    @MockBean
    private UserService userService;

    @InjectMocks
    private MatchService matchService;

    private Match match;
    private Game game;
    private MatchPlayer mp;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        match = new Match();
        match.setMatchId(1L);
        match.setMatchGoal(50);
        match.setPhase(MatchPhase.BETWEEN_GAMES);
        match.setMatchSummary(new MatchSummary());

        // Create user and attach to MatchPlayer
        User user = new User();
        user.setId(42L);
        user.setUsername("testuser");

        mp = new MatchPlayer();
        mp.setMatchPlayerSlot(1);
        mp.setMatchScore(40);
        mp.setUser(user);

        List<MatchPlayer> players = new ArrayList<>();
        players.add(mp);
        match.setMatchPlayers(players);

        game = new Game();
        game.setGameId(1L);
        game.setPhase(GamePhase.NORMALTRICK);
        game.setMatch(match);

        match.setGames(new ArrayList<>(List.of(game)));

        // Clear mutable structures
        match.getMessages().clear();
        match.getInvites().clear();
        match.getJoinRequests().clear();
        match.getAiPlayers().clear();
    }

    @Test
    public void testAbortMatch_setsAbortedAndCallsCleanup() {
        matchService.abortMatch(match);
        assertEquals(MatchPhase.ABORTED, match.getPhase());
    }

    @Test
    public void testAutoPlayToMatchSummary_delegatesCorrectly() {
        when(matchRepository.findMatchByMatchId(1L)).thenReturn(match);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        matchService.autoPlayToMatchSummary(1L, 0);

        verify(gameSimulationService).autoPlayToMatchSummary(match, game);
    }

    @Test
    public void testAutoPlayToGameSummary_delegatesCorrectly() {
        when(matchRepository.findMatchByMatchId(1L)).thenReturn(match);

        matchService.autoPlayToGameSummary(1L, 0);

        verify(gameSimulationService).autoPlayToGameSummary(match, game);
    }

    @Test
    public void testAutoPlayToLastTrickOfMatch_delegatesAndSaves() {
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(matchRepository.findMatchByMatchId(1L)).thenReturn(match);

        matchService.autoPlayToLastTrickOfMatch(1L);

        verify(gameSimulationService).autoPlayToLastTrickOfMatch(match, game);
        verify(matchPlayerRepository).saveAll(match.getMatchPlayers());
    }

    @Test
    public void testCheckGameAndStartNextIfNeeded_createsGame() {
        match.setPhase(MatchPhase.BETWEEN_GAMES);

        Game finishedGame = new Game();
        finishedGame.setGameId(999L);
        finishedGame.setPhase(GamePhase.FINISHED);
        finishedGame.setMatch(match);

        match.setGames(new ArrayList<>(List.of(finishedGame)));

        when(gameSetupService.createAndStartGameForMatch(eq(match), any(), any(), isNull()))
                .thenReturn(game);

        matchService.checkGameAndStartNextIfNeeded(match);

        verify(gameSetupService).createAndStartGameForMatch(eq(match), any(), any(), isNull());
    }

    @Test
    public void testEstablishRankingOfMatchPlayersInMatch_correctRanksAssigned() {
        MatchPlayer mp1 = new MatchPlayer();
        mp1.setGameScore(10);
        MatchPlayer mp2 = new MatchPlayer();
        mp2.setGameScore(5);

        List<MatchPlayer> players = new ArrayList<>();
        players.add(mp1);
        players.add(mp2);

        match.setMatchPlayers(players);

        matchService.establishRankingOfMatchPlayersInMatch(match);

        verify(matchPlayerRepository, times(2)).save(any());
        assertEquals(1, mp2.getRankingInMatch());
        assertEquals(2, mp1.getRankingInMatch());
    }
}
