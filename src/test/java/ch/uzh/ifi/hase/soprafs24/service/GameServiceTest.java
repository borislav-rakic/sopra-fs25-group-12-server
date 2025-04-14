package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.model.Card;
import ch.uzh.ifi.hase.soprafs24.model.DrawCardResponse;
import ch.uzh.ifi.hase.soprafs24.model.NewDeckResponse;
import ch.uzh.ifi.hase.soprafs24.repository.*;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayerMatchInformationDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

public class GameServiceTest {
    @Mock
    private MatchRepository matchRepository = Mockito.mock(MatchRepository.class);

    @Mock
    private UserRepository userRepository = Mockito.mock(UserRepository.class);

    @Mock
    private MatchPlayerRepository matchPlayerRepository = Mockito.mock(MatchPlayerRepository.class);

    @Mock
    private MatchStatsRepository matchStatsRepository = Mockito.mock(MatchStatsRepository.class);

    @Mock
    private GameStatsRepository gameStatsRepository = Mockito.mock(GameStatsRepository.class);

    @Mock
    private ExternalApiClientService externalApiClientService = Mockito.mock(ExternalApiClientService.class);

    @MockBean
    private UserService userService = Mockito.mock(UserService.class);

    @InjectMocks
    private GameService gameService = new GameService(matchRepository, userRepository, matchPlayerRepository, matchStatsRepository, gameStatsRepository, externalApiClientService, userService);

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

    @Test
    public void testStartMatchSuccess() {
        NewDeckResponse newDeckResponse = new NewDeckResponse();
        newDeckResponse.setDeck_id("9876");
        newDeckResponse.setSuccess(true);
        newDeckResponse.setRemaining(52);
        newDeckResponse.setShuffled(true);

        Card card = new Card();
        card.setCode("3H");

        List<Card> cards = new ArrayList<>();
        cards.add(card);

        DrawCardResponse drawCardResponse = new DrawCardResponse();
        drawCardResponse.setDeck_id("9876");
        drawCardResponse.setSuccess(true);
        drawCardResponse.setRemaining(39);
        drawCardResponse.setCards(cards);

        Mono<DrawCardResponse> drawCardMono = Mono.just(drawCardResponse);

        Mono<NewDeckResponse> newDeckMono = Mono.just(newDeckResponse);

        given(userRepository.findUserByToken(Mockito.any())).willReturn(user);
        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);

        given(matchRepository.save(Mockito.any())).willReturn(match);
        given(matchPlayerRepository.save(Mockito.any())).willReturn(matchPlayer);

        given(externalApiClientService.createNewDeck()).willReturn(newDeckMono);

        given(gameStatsRepository.save(Mockito.any())).willReturn(new GameStats());

        given(externalApiClientService.drawCard("9876", 13)).willReturn(drawCardMono);

        given(matchPlayerRepository.save(Mockito.any())).willReturn(matchPlayer);

        gameService.startMatch(1L, "1234");

        verify(matchRepository, Mockito.times(2)).save(Mockito.any());
        verify(matchPlayerRepository).save(Mockito.any());
        verify(externalApiClientService).createNewDeck();
        verify(gameStatsRepository, Mockito.times(52)).save(Mockito.any());
        verify(externalApiClientService).drawCard("9876", 13);
    }
}
