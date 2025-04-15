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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

public class GameServiceTest {
    @Mock
    private MatchRepository matchRepository = Mockito.mock(MatchRepository.class);

    @Mock
    private UserRepository userRepository = Mockito.mock(UserRepository.class);

    @Mock
    private GameRepository gameRepository = Mockito.mock(GameRepository.class);

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
    private GameService gameService = new GameService(
            matchRepository,
            userRepository,
            matchPlayerRepository,
            matchStatsRepository,
            gameStatsRepository,
            gameRepository,
            externalApiClientService,
            userService);

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

        Card card1 = new Card();
        card1.setCode("3H");
        Card card2 = new Card();
        card2.setCode("0H");
        Card card3 = new Card();
        card3.setCode("0S");
        Card card4 = new Card();
        card4.setCode("0D");
        Card card5 = new Card();
        card5.setCode("0C");

        List<Card> cards = new ArrayList<>();
        cards.add(card1);
        cards.add(card2);
        cards.add(card3);
        cards.add(card4);
        cards.add(card5);

        DrawCardResponse drawCardResponse = new DrawCardResponse();
        drawCardResponse.setDeck_id("9876");
        drawCardResponse.setSuccess(true);
        drawCardResponse.setRemaining(0);
        drawCardResponse.setCards(cards);

        Mono<DrawCardResponse> drawCardMono = Mono.just(drawCardResponse);

        Mono<NewDeckResponse> newDeckMono = Mono.just(newDeckResponse);

        given(userRepository.findUserByToken(Mockito.any())).willReturn(user);
        given(matchRepository.findMatchByMatchId(Mockito.any())).willReturn(match);

        given(matchRepository.save(Mockito.any())).willReturn(match);
        given(matchPlayerRepository.save(Mockito.any())).willReturn(matchPlayer);

        given(externalApiClientService.createNewDeck()).willReturn(newDeckMono);

        given(gameStatsRepository.findByRankSuit(Mockito.any())).willReturn(new GameStats());

        given(gameStatsRepository.save(Mockito.any())).willReturn(new GameStats());

        given(externalApiClientService.drawCard("9876", 52)).willReturn(drawCardMono);

        given(matchPlayerRepository.save(Mockito.any())).willReturn(matchPlayer);

        match.setHost(user.getUsername()); // so host can start the match
        match.setPlayer1(user);

        Map<Integer, Long> invites = new HashMap<>();
        invites.put(1, user.getId());
        match.setInvites(invites);

        Map<Long, String> joinRequests = new HashMap<>();
        joinRequests.put(user.getId(), "accepted");
        match.setJoinRequests(joinRequests);

        List<Integer> aiPlayers = new ArrayList<>();
        aiPlayers.add(1);
        aiPlayers.add(2);
        aiPlayers.add(3);
        match.setAiPlayers(aiPlayers);

        gameService.startMatch(1L, "1234");

        verify(matchRepository, Mockito.times(2)).save(Mockito.any());
        verify(matchPlayerRepository).save(Mockito.any());
        verify(externalApiClientService).createNewDeck();
        verify(gameStatsRepository, Mockito.times(57)).save(Mockito.any());
        verify(externalApiClientService).drawCard("9876", 52);
    }
}
