package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchStatsRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayerMatchInformationDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

public class GameServiceTest {
    @Mock
    private MatchRepository matchRepository = Mockito.mock(MatchRepository.class);

    @Mock
    private UserRepository userRepository = Mockito.mock(UserRepository.class);

    @MockBean
    private UserService userService = Mockito.mock(UserService.class);

    @Mock
    private MatchPlayerRepository matchPlayerRepository = Mockito.mock(MatchPlayerRepository.class);

    @Mock
    private MatchStatsRepository matchStatsRepository = Mockito.mock(MatchStatsRepository.class);

    @Mock
    private WebClient externalApiClient = Mockito.mock(WebClient.class);

    @InjectMocks
    private GameService gameService = new GameService(matchRepository, userService, userRepository, matchPlayerRepository, matchStatsRepository, externalApiClient);

    private Match match;
    private User user;
    private MatchPlayer matchPlayer;

    @BeforeEach
    public void setup() {
        user = new User();
        user.setUsername("username");
        user.setPassword("password");
        user.setId(1L);
        user.setToken("1234");

        match = new Match();

        List<MatchPlayer> matchPlayers = new ArrayList<>();
        matchPlayers.add(new MatchPlayer());
        matchPlayers.get(0).setPlayerId(user);
        matchPlayers.get(0).setMatch(match);

        match.setMatchPlayers(matchPlayers);
        match.setHost(user.getUsername());
        match.setLength(100);
        match.setStarted(false);
        match.setPlayer1(user);

        matchPlayer = new MatchPlayer();
        matchPlayer.setMatch(match);
        matchPlayer.setPlayerId(user);
        matchPlayer.setMatchPlayerId(1L);
    }

    @Test
    public void testGetPlayerMatchInformationSuccess() {
        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);
        given(userRepository.findUserByToken(Mockito.any())).willReturn(user);
        given(matchPlayerRepository.findMatchPlayerByUser(user)).willReturn(matchPlayer);

        List<String> matchPlayers = new ArrayList<>();
        matchPlayers.add(user.getUsername());
        matchPlayers.add(null);
        matchPlayers.add(null);
        matchPlayers.add(null);

        PlayerMatchInformationDTO playerMatchInformationDTO = new PlayerMatchInformationDTO();
        playerMatchInformationDTO.setMatchId(match.getMatchId());
        playerMatchInformationDTO.setHost(match.getHost());
        playerMatchInformationDTO.setAiPlayers(new ArrayList<>());
        playerMatchInformationDTO.setMatchPlayers(matchPlayers);
        playerMatchInformationDTO.setLength(match.getLength());
        playerMatchInformationDTO.setStarted(true);

        PlayerMatchInformationDTO result = gameService.getPlayerMatchInformation("1234", 1L);

        assertEquals(playerMatchInformationDTO.getMatchId(), result.getMatchId());
        assertEquals(playerMatchInformationDTO.getHost(), result.getHost());
        assertEquals(playerMatchInformationDTO.getMatchPlayers(), result.getMatchPlayers());
        assertEquals(playerMatchInformationDTO.getAiPlayers(), result.getAiPlayers());
        assertEquals(playerMatchInformationDTO.getLength(), result.getLength());
        assertEquals(playerMatchInformationDTO.getStarted(), result.getStarted());
    }
}
