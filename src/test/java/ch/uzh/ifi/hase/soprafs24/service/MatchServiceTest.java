package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class MatchServiceTest {

    @Mock
    private GameRepository gameRepository = Mockito.mock(GameRepository.class);

    @Mock
    private GameService gameService = Mockito.mock(GameService.class);

    @Mock
    private GameSimulationService gameSimulationService = Mockito.mock(GameSimulationService.class);

    @Mock
    private GameSetupService gameSetupService = Mockito.mock(GameSetupService.class);

    @Mock
    private HtmlSummaryService htmlSummaryService = Mockito.mock(HtmlSummaryService.class);

    @Mock
    private MatchPlayerRepository matchPlayerRepository = Mockito.mock(MatchPlayerRepository.class);

    @Mock
    private MatchRepository matchRepository = Mockito.mock(MatchRepository.class);

    @Mock
    private PollingService pollingService = Mockito.mock(PollingService.class);

    @Mock
    private UserRepository userRepository = Mockito.mock(UserRepository.class);

    @Mock
    private MatchSetupService matchSetupService = Mockito.mock(MatchSetupService.class);

    @MockBean
    private UserService userService = Mockito.mock(UserService.class);

    @InjectMocks
    private MatchService matchService = new MatchService(
            gameRepository,
            gameService,
            gameSetupService,
            gameSimulationService,
            htmlSummaryService,
            matchPlayerRepository,
            matchRepository,
            pollingService,
            userRepository,
            userService,
            matchSetupService
    // alphabetical order
    );

    private Match match;
    private User user;
    private User user2;
    private MatchPlayer matchPlayer;

    @BeforeEach
    public void setup() {
        user = new User();
        user.setUsername("username");
        user.setPassword("password");
        user.setId(11L); // Users up to id 10 are reserverd for AI Players.
        user.setToken("1234");
        user.setIsAiPlayer(false);
        user.setStatus(UserStatus.ONLINE);

        user2 = new User();
        user2.setUsername("username2");
        user2.setPassword("password2");
        user2.setId(12L); // Users up to id 10 are reserverd for AI Players.
        user2.setToken("12342");
        user2.setIsAiPlayer(false);
        user2.setStatus(UserStatus.ONLINE);

        match = new Match();
        match.setPhase(MatchPhase.SETUP);

        List<MatchPlayer> matchPlayers = new ArrayList<>();
        matchPlayers.add(new MatchPlayer());
        matchPlayers.get(0).setUser(user);
        matchPlayers.get(0).setMatch(match);

        match.setMatchPlayers(matchPlayers);
        match.setHostId(user.getId());
        match.setMatchGoal(100);
        match.setStarted(false);
        match.setPlayer1(user);

        matchPlayer = new MatchPlayer();
        matchPlayer.setMatch(match);
        matchPlayer.setUser(user);
        matchPlayer.setMatchPlayerId(1L);
    }

    @Test
    public void testGetMatchesInformation() {
        List<Match> matchList = new ArrayList<>();
        matchList.add(match);

        when(matchRepository.findAll()).thenReturn(matchList);

        List<Match> result = matchService.getMatchesInformation();

        assertEquals(matchList, result);
    }

    @Test
    public void testGetPollingError() {
        when(matchRepository.findMatchByMatchId(1L)).thenReturn(null);

        assertThrows(
                ResponseStatusException.class,
                () -> matchService.getPolling(1L),
                "Expected getPolling to throw an exception");
    }

    @Test
    public void testGetPollingSuccess() {
        when(matchRepository.findMatchByMatchId(1L)).thenReturn(match);

        Match result = matchService.getPolling(1L);

        assertEquals(match, result);
    }
}
