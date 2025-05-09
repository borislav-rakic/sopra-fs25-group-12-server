package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.model.CardResponse;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.util.*;

import static org.mockito.BDDMockito.given;

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
            matchPlayerRepository
    );

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
    public void testFetchAndDistributeCardsAsync() {
        RuntimeException apiError = new RuntimeException("Simulated exception");
        given(externalApiClientService.createNewDeck()).willReturn(Mono.error(apiError));

        given(matchRepository.findMatchByMatchId(Mockito.anyLong())).willReturn(match);

        given(gameRepository.findWaitingGameByMatchid(Mockito.anyLong())).willReturn(match.getGames());

        given(matchPlayerRepository.saveAndFlush(Mockito.any())).willReturn(match.getMatchPlayers().get(0));

        gameSetupService.fetchDeckAndDistributeCardsAsync(matchRepository, gameRepository, 1L);
    }

    @Test
    public void testDistributeCards() {
        RuntimeException apiError = new RuntimeException("Simulated exception");
        given(externalApiClientService.drawCard(Mockito.any(), Mockito.anyInt())).willReturn(Mono.error(apiError));

        given(matchRepository.findMatchByMatchId(Mockito.anyLong())).willReturn(match);

        given(gameRepository.findWaitingGameByMatchid(Mockito.anyLong())).willReturn(match.getGames());

        given(matchPlayerRepository.saveAndFlush(Mockito.any())).willReturn(match.getMatchPlayers().get(0));

        gameSetupService.distributeCards(match, game, matchRepository, gameRepository, null);
    }

    private List<CardResponse> generateDeckHelper(int deckSize, Long seed) {
        List<CardResponse> deck = new ArrayList<>();
        String[] suits = { "C", "D", "H", "S" };
        String[] ranks = { "2", "3", "4", "5", "6", "7", "8", "9", "0", "J", "Q", "K", "A" };

        for (String suit : suits) {
            for (String rank : ranks) {
                CardResponse card = new CardResponse();
                card.setCode(rank + suit); // e.g., "2C"
                deck.add(card);
            }
        }

        Random rng = new Random(seed);
        for (int i = deckSize - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            CardResponse temp = deck.get(i);
            deck.set(i, deck.get(j));
            deck.set(j, temp);
        }

        return deck;
    }
}
