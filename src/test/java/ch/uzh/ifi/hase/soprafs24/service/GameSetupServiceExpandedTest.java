package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GameSetupServiceExpandedTest {

    private GameSetupService gameSetupService;
    private ExternalApiClientService externalApiClientService;
    private GameStatsService gameStatsService;
    private MatchPlayerRepository matchPlayerRepository;
    private MatchRepository matchRepository;
    private GameRepository gameRepository;

    @BeforeEach
    void setup() {
        externalApiClientService = mock(ExternalApiClientService.class);
        gameStatsService = mock(GameStatsService.class);
        matchPlayerRepository = mock(MatchPlayerRepository.class);
        matchRepository = mock(MatchRepository.class);
        gameRepository = mock(GameRepository.class);

        gameSetupService = new GameSetupService(
                externalApiClientService,
                gameStatsService,
                matchPlayerRepository);
    }

    @Test
    void testCreateAndStartGameForMatch_success_internalSeed() {
        Match match = new Match();
        match.setPhase(MatchPhase.BEFORE_GAMES);
        match.setMatchId(1L);
        match.setGames(new ArrayList<>());

        List<MatchPlayer> players = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            MatchPlayer player = new MatchPlayer();
            player.setUser(new User());
            player.setMatchPlayerSlot(i);
            players.add(player);
        }
        match.setMatchPlayers(players);

        Game game = new Game();
        game.setPhase(GamePhase.PRESTART);
        game.setGameNumber(1);
        game.setGameId(42L);

        when(gameRepository.save(any(Game.class))).thenReturn(game);
        doNothing().when(gameRepository).flush();

        Game result = gameSetupService.createAndStartGameForMatch(match, matchRepository, gameRepository, 1234569247L);

        assertNotNull(result);
        assertTrue(
                result.getPhase() == GamePhase.PASSING || result.getPhase() == GamePhase.SKIP_PASSING,
                "Expected game to be in PASSING or SKIP_PASSING phase");
        verify(gameRepository, atLeastOnce()).save(any(Game.class));
    }

    @Test
    void testAssignTwoOfClubsLeader_success() {
        Match match = new Match();
        Game game = new Game();
        game.setMatch(match);

        MatchPlayer playerWithTwoC = new MatchPlayer();
        playerWithTwoC.setMatchPlayerSlot(2);
        playerWithTwoC.setHand("2C,5D,7H");
        playerWithTwoC.setUser(new ch.uzh.ifi.hase.soprafs24.entity.User());

        match.setMatchPlayers(List.of(playerWithTwoC));

        gameSetupService.assignTwoOfClubsLeader(game);

        assertEquals(2, game.getCurrentMatchPlayerSlot());
        assertEquals(2, game.getTrickLeaderMatchPlayerSlot());
    }

    @Test
    void testAssignTwoOfClubsLeader_failure_noPlayerHasCard() {
        Match match = new Match();
        Game game = new Game();
        game.setMatch(match);

        MatchPlayer player = new MatchPlayer();
        player.setMatchPlayerSlot(0);
        player.setHand("5D,7H,9S");

        match.setMatchPlayers(List.of(player));

        assertThrows(IllegalStateException.class, () -> {
            gameSetupService.assignTwoOfClubsLeader(game);
        });
    }

    @Test
    void testDistributeFullDeckToPlayers_invalidCardCount_throws() {
        Match match = new Match();
        match.setMatchPlayers(generateValidPlayers(4));

        Game game = new Game();

        List<CardResponse> shortDeck = generateFakeDeck(40); // not 52

        assertThrows(IllegalArgumentException.class, () -> {
            invokePrivateDistribute(match, game, shortDeck);
        });
    }

    @Test
    void testDistributeFullDeckToPlayers_invalidPlayerCount_throws() {
        Match match = new Match();
        match.setMatchPlayers(generateValidPlayers(3)); // not 4

        Game game = new Game();

        List<CardResponse> fullDeck = generateValidDeck();

        assertThrows(IllegalStateException.class, () -> {
            invokePrivateDistribute(match, game, fullDeck);
        });
    }

    @Test
    void testDistributeFullDeckToPlayers_setsPhaseCorrectly() {
        Match match = new Match();
        match.setMatchPlayers(generateValidPlayers(4));

        Game game = new Game();
        game.setGameNumber(1); // any game number

        List<CardResponse> fullDeck = generateValidDeck();

        invokePrivateDistribute(match, game, fullDeck);

        assertEquals(MatchPhase.IN_PROGRESS, match.getPhase());
        assertTrue(
                game.getPhase() == GamePhase.PASSING || game.getPhase() == GamePhase.SKIP_PASSING,
                "Expected PASSING or SKIP_PASSING but got: " + game.getPhase());
    }

    // --- Helpers ---

    private List<MatchPlayer> generateValidPlayers(int count) {
        List<MatchPlayer> players = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            MatchPlayer p = new MatchPlayer();
            User user = new User();
            user.setId((long) i + 1);
            p.setUser(user);
            p.setMatchPlayerSlot(i);
            players.add(p);
        }
        return players;
    }

    private List<CardResponse> generateValidDeck() {
        String[] suits = { "C", "D", "H", "S" };
        String[] ranks = { "2", "3", "4", "5", "6", "7", "8", "9", "0", "J", "Q", "K", "A" };

        List<CardResponse> deck = new ArrayList<>();
        for (String suit : suits) {
            for (String rank : ranks) {
                CardResponse card = new CardResponse();
                card.setCode(rank + suit); // e.g., "2C"
                deck.add(card);
            }
        }

        return deck;
    }

    private List<CardResponse> generateFakeDeck(int size) {
        return IntStream.range(0, size).mapToObj(i -> {
            CardResponse c = new CardResponse();
            c.setCode("C" + i);
            return c;
        }).collect(Collectors.toList());
    }

    private void invokePrivateDistribute(Match match, Game game, List<CardResponse> deck) {
        try {
            var method = GameSetupService.class
                    .getDeclaredMethod("distributeFullDeckToPlayers", Match.class, Game.class,
                            MatchRepository.class, GameRepository.class, List.class);
            method.setAccessible(true);
            method.invoke(gameSetupService, match, game, matchRepository, gameRepository, deck);
        } catch (Exception e) {
            // Unwrap reflection exceptions
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            } else {
                fail("Unexpected exception: " + e.getMessage());
            }
        }
    }
}
