package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.model.CardResponse;
import ch.uzh.ifi.hase.soprafs24.model.DrawCardResponse;
import ch.uzh.ifi.hase.soprafs24.model.NewDeckResponse;
import ch.uzh.ifi.hase.soprafs24.repository.*;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePassingDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PollingDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.constant.Rank;
import ch.uzh.ifi.hase.soprafs24.constant.Suit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

import java.util.Optional;

public class GameServiceTest {
    @Mock
    private AiPassingService aiPassingService = Mockito.mock(AiPassingService.class);

    @Mock
    private AiPlayingService aiPlayingService = Mockito.mock(AiPlayingService.class);

    @Mock
    private CardPassingService cardPassingService = Mockito.mock(CardPassingService.class);

    @Mock
    private CardRulesService cardRulesService = Mockito.mock(CardRulesService.class);

    @Mock
    private ExternalApiClientService externalApiClientService = Mockito.mock(ExternalApiClientService.class);

    @Mock
    private GameRepository gameRepository = Mockito.mock(GameRepository.class);

    @Mock
    private GameStatsRepository gameStatsRepository = Mockito.mock(GameStatsRepository.class);

    @Mock
    private GameStatsService gameStatsService = Mockito.mock(GameStatsService.class);

    @Mock
    private MatchPlayerRepository matchPlayerRepository = Mockito.mock(MatchPlayerRepository.class);

    @Mock
    private MatchRepository matchRepository = Mockito.mock(MatchRepository.class);

    @Mock
    private PassedCardRepository passedCardRepository = Mockito.mock(PassedCardRepository.class);

    @Mock
    private UserRepository userRepository = Mockito.mock(UserRepository.class);

    @MockBean
    private UserService userService = Mockito.mock(UserService.class);

    @InjectMocks
    private GameService gameService = new GameService(

            aiPlayingService,
            cardPassingService,
            cardRulesService,
            externalApiClientService,
            gameRepository,
            gameStatsRepository,
            gameStatsService,
            matchPlayerRepository,
            matchRepository,
            passedCardRepository,
            userRepository,
            userService);

    private GameStats createGameStat(Game game, Rank rank, Suit suit) {
        GameStats stat = new GameStats();
        stat.setGame(game);
        stat.setRank(rank);
        stat.setSuit(suit);
        stat.setCardHolder(1); // Simulate slot ownership!
        return stat;
    }

    private Match match;
    private User user;
    private MatchPlayer matchPlayer;
    private Game game;

    @BeforeEach
    public void setup() {
        user = new User();
        user.setUsername("username");
        user.setPassword("password");
        user.setId(4L);
        user.setToken("1234");

        match = new Match();

        matchPlayer = new MatchPlayer();
        matchPlayer.setUser(user);
        matchPlayer.setMatch(match);
        matchPlayer.setSlot(1);

        matchPlayer.addCardCodeToHand("5C");
        matchPlayer.addCardCodeToHand("KH");
        matchPlayer.addCardCodeToHand("7S");

        List<MatchPlayer> matchPlayers = new ArrayList<>();
        matchPlayers.add(matchPlayer);

        match.setMatchPlayers(matchPlayers);
        match.setHostId(4L);
        match.setMatchGoal(100);
        match.setStarted(false);
        match.setPlayer1(user);

        game = new Game();
        game.setGameId(1L);
        game.setMatch(match);
        game.setGameNumber(1);
        game.setCurrentSlot(1);

        match.getGames().add(game);
    }

    @Test
    public void testGetPlayerPollingSuccess() {
        // === Setup Users ===
        User user = new User();
        user.setId(4L);
        user.setUsername("testuser");
        user.setIsAiPlayer(false);

        User p2 = new User();
        p2.setId(5L);
        p2.setUsername("bot2");
        p2.setIsAiPlayer(false);
        User p3 = new User();
        p3.setId(6L);
        p3.setUsername("bot3");
        p3.setIsAiPlayer(false);
        User p4 = new User();
        p4.setId(7L);
        p4.setUsername("bot4");
        p4.setIsAiPlayer(false);

        // === Setup Match ===
        Match match = new Match();
        match.setMatchId(1L);
        match.setPlayer1(user);
        match.setPlayer2(p2);
        match.setPlayer3(p3);
        match.setPlayer4(p4);
        match.setHostId(4L);
        match.setMatchGoal(100);
        match.setPhase(MatchPhase.READY);
        match.setAiPlayers(new HashMap<>());

        // === Setup MatchPlayer ===
        MatchPlayer matchPlayer = new MatchPlayer();
        matchPlayer.setUser(user);
        matchPlayer.setSlot(1);

        MatchPlayer mp2 = new MatchPlayer();
        mp2.setUser(p2);
        mp2.setSlot(2);

        MatchPlayer mp3 = new MatchPlayer();
        mp3.setUser(p3);
        mp3.setSlot(3);

        MatchPlayer mp4 = new MatchPlayer();
        mp4.setUser(p4);
        mp4.setSlot(4);

        matchPlayer.setGameScore(0);
        mp2.setGameScore(0);
        mp3.setGameScore(0);
        mp4.setGameScore(0);

        match.setMatchPlayers(List.of(matchPlayer, mp2, mp3, mp4));

        // === Setup Game ===
        Game game = new Game();
        game.setGameId(1L);
        game.setGameNumber(1);
        game.setPhase(GamePhase.FIRSTTRICK);
        game.setMatch(match);
        game.setCurrentTrickNumber(2);
        game.setPreviousTrickWinnerSlot(4);
        game.setPreviousTrickPoints(0);
        game.setTrickLeaderSlot(1);
        game.setHeartsBroken(false);
        game.setCurrentSlot(1);

        matchPlayer.setHand("2C");

        // === Tie Game to Match ===
        match.setGames(List.of(game));

        // === Mock Repositories ===
        when(userRepository.findUserByToken("1234")).thenReturn(user);
        when(matchRepository.findMatchByMatchId(1L)).thenReturn(match);
        when(matchPlayerRepository.findByUserAndMatch(user, match)).thenReturn(matchPlayer);
        when(gameRepository.findFirstByMatchAndPhaseNotIn(eq(match), anyList())).thenReturn(game);
        when(matchPlayerRepository.findByUserAndMatchAndSlot(user, match, 1)).thenReturn(matchPlayer);

        // === Act ===
        System.out.println("Players in match: " + match.getMatchPlayers().size());
        PollingDTO result = gameService.getPlayerPolling(user, match);

        // === Assert ===
        assertNotNull(result);
        assertEquals(1L, result.getMatchId());
        assertEquals(4, result.getHostId());
        assertEquals(100, result.getMatchGoal());
        assertEquals(GamePhase.FIRSTTRICK, result.getGamePhase());
        assertEquals(MatchPhase.READY, result.getMatchPhase());
        assertEquals(List.of("testuser", "bot2", "bot3", "bot4"), result.getMatchPlayers());

        // Hand
        assertEquals(1, result.getPlayerCards().size());
        assertEquals("2C", result.getPlayerCards().get(0).getCard().getCode());

        // Current Trick
        assertNotNull(result.getCurrentTrick()); // likely empty in this mock
        assertEquals(0, result.getCurrentTrick().size());

        // Previous Trick
        assertNotNull(result.getPreviousTrick());
        // assertEquals(4, result.getPreviousTrick().size());
        // assertEquals("2", result.getPreviousTrick().get(0).getRank());
        // assertEquals("C", result.getPreviousTrick().get(0).getSuit());

        // Trick Info
        assertEquals(4, result.getPreviousTrickWinnerSlot());
        assertEquals(0, result.getPreviousTrickPoints());
        assertEquals(1, result.getCurrentTrickLeaderSlot());
    }

    @Test
    public void testStartMatchSuccess() {
        // Create and link the Match
        Match match = new Match();
        match.setMatchId(1L);

        Game game = new Game();
        game.setGameId(42L);
        game.setMatch(match);

        // Mock repository lookups
        given(matchRepository.findById(1L)).willReturn(Optional.of(match));
        given(gameRepository.findById(42L)).willReturn(Optional.of(game));

        // Mocks
        User user = Mockito.mock(User.class);

        MatchPlayer matchPlayer = Mockito.mock(MatchPlayer.class);

        User bot2 = mock(User.class);
        User bot3 = mock(User.class);
        User bot4 = mock(User.class);

        given(bot2.getUsername()).willReturn("bot2");
        given(bot3.getUsername()).willReturn("bot3");
        given(bot4.getUsername()).willReturn("bot4");

        User p2 = mock(User.class);
        User p3 = mock(User.class);
        User p4 = mock(User.class);

        when(p2.getUsername()).thenReturn("bot2");
        when(p3.getUsername()).thenReturn("bot3");
        when(p4.getUsername()).thenReturn("bot4");

        match.setPlayer2(p2);
        match.setPlayer3(p3);
        match.setPlayer4(p4);

        // Mock basic user and match setup
        Mockito.when(user.getId()).thenReturn(4L);
        match.setHostId(4L);
        match.setPlayer1(user);
        match.setInvites(Map.of(1, 1L));
        match.setJoinRequests(Map.of(1L, "accepted"));
        match.setAiPlayers(Map.of(1, 0, 2, 1, 3, 2));
        match.setMatchPlayers(List.of(matchPlayer));
        match.setGames(new ArrayList<>());

        // Simulate game creation with ID
        Game savedGame = new Game();
        savedGame.setGameId(1L);
        savedGame.setMatch(match);

        Mockito.when(gameRepository.save(Mockito.any(Game.class))).thenAnswer(invocation -> {
            Game game2 = invocation.getArgument(0);
            game2.setGameId(1L);
            game2.setMatch(match);
            return game2;
        });

        Mockito.when(gameRepository.findById(1L)).thenReturn(Optional.of(savedGame));

        // Mock deck and cards
        NewDeckResponse newDeckResponse = new NewDeckResponse();
        newDeckResponse.setDeck_id("9876");
        newDeckResponse.setSuccess(true);
        newDeckResponse.setRemaining(52);
        newDeckResponse.setShuffled(true);

        DrawCardResponse drawCardResponse = new DrawCardResponse();
        drawCardResponse.setDeck_id("9876");
        drawCardResponse.setSuccess(true);
        drawCardResponse.setRemaining(0);
        drawCardResponse.setCards(new ArrayList<>(List.of(
                createCard("3H"),
                createCard("0H"),
                createCard("0S"),
                createCard("0D"),
                createCard("0C"))));

        // Mock repositories
        Mockito.when(userRepository.findUserByToken("1234")).thenReturn(user);
        Mockito.when(matchRepository.findMatchByMatchId(1L)).thenReturn(match);
        Mockito.when(matchPlayerRepository.save(Mockito.any())).thenReturn(matchPlayer);
        Mockito.when(gameStatsRepository.findByRankSuit(Mockito.any())).thenReturn(new GameStats());
        Mockito.when(gameStatsRepository.save(Mockito.any())).thenReturn(new GameStats());

        // Important: repeat this to support the async call in subscribe()
        Mockito.when(matchRepository.findMatchByMatchId(Mockito.any())).thenReturn(match);

        // Mock external API calls
        Mockito.when(externalApiClientService.createNewDeck()).thenReturn(Mono.just(newDeckResponse));
        Mockito.when(externalApiClientService.drawCard("9876", 52)).thenReturn(Mono.just(drawCardResponse));

        // ACT
        gameService.startMatch(1L, "1234", null);

        // VERIFY
        // Mockito.verify(matchRepository, Mockito.atLeastOnce()).save(Mockito.any());
        Mockito.verify(gameRepository, Mockito.atLeastOnce()).save(Mockito.any());
        Mockito.verify(externalApiClientService).createNewDeck();
        Mockito.verify(externalApiClientService).drawCard("9876", 52);
        Mockito.verify(gameStatsService, Mockito.atLeastOnce()).initializeGameStats(Mockito.any(), Mockito.any());
    }

    @Test
    public void testMakePassingHappen() {
        // Setup game + match linkage
        match.setMatchId(1L);
        match.setPlayer1(user); // Ensure player is in slot 1
        game.setGameId(42L);
        game.setMatch(match);
        game.setPhase(GamePhase.PASSING);
        match.setGames(List.of(game));

        // Register slot mapping
        MatchPlayer matchPlayer = new MatchPlayer();
        matchPlayer.setUser(user);
        matchPlayer.setMatch(match);
        matchPlayer.setSlot(1);
        match.setMatchPlayers(List.of(matchPlayer));

        System.out.println("MatchPlayers size: " + match.getMatchPlayers().size());
        match.getMatchPlayers().forEach(mp -> System.out.println(
                "Slot: " + mp.getSlot() + ", User ID: " + (mp.getUser() != null ? mp.getUser().getId() : "null")));

        // Mock owned cards for slot 1 (rank + suit)
        List<GameStats> gameStatsList = new ArrayList<>();
        gameStatsList.add(createGameStat(game, Rank._3, Suit.H)); // 3H
        gameStatsList.add(createGameStat(game, Rank._0, Suit.H)); // 0H
        gameStatsList.add(createGameStat(game, Rank._0, Suit.S)); // 0S

        // Card codes being passed
        List<String> cards = List.of("3H", "0H", "0S");

        GamePassingDTO dto = new GamePassingDTO();
        dto.setPlayerId(user.getId());
        dto.setCards(cards);

        // === Mocks ===
        given(userService.getUserByToken("1234")).willReturn(user);
        given(matchRepository.findMatchByMatchId(1L)).willReturn(match);
        given(gameRepository.findFirstByMatchAndPhaseNotIn(eq(match), anyList())).willReturn(game);

        // Simulate card ownership
        given(gameStatsRepository.findByRankSuitAndGameAndCardHolder("3H", game, 1)).willReturn(gameStatsList.get(0));
        given(gameStatsRepository.findByRankSuitAndGameAndCardHolder("0H", game, 1)).willReturn(gameStatsList.get(1));
        given(gameStatsRepository.findByRankSuitAndGameAndCardHolder("0S", game, 1)).willReturn(gameStatsList.get(2));

        // Simulate that these cards have not yet been passed
        given(passedCardRepository.existsByGameAndFromSlotAndRankSuit(game, 1, "3H")).willReturn(false);
        given(passedCardRepository.existsByGameAndFromSlotAndRankSuit(game, 1, "0H")).willReturn(false);
        given(passedCardRepository.existsByGameAndFromSlotAndRankSuit(game, 1, "0S")).willReturn(false);

        // Fallback for any legacy check if it's still in code
        given(passedCardRepository.existsByGameAndRankSuit(game, "3H")).willReturn(false);
        given(passedCardRepository.existsByGameAndRankSuit(game, "0H")).willReturn(false);
        given(passedCardRepository.existsByGameAndRankSuit(game, "0S")).willReturn(false);

        // Return a dummy PassedCard on save
        given(passedCardRepository.save(Mockito.any())).willReturn(new PassedCard());

        // === ACT ===
        gameService.passingAcceptCards(game, matchPlayer, dto);

        // === VERIFY ===
    }

    private CardResponse createCard(String code) {
        CardResponse card = new CardResponse();
        card.setCode(code);
        return card;
    }

    private void setRankSuit(GameStats gs, String code) {
        String rankStr = code.substring(0, code.length() - 1);
        String suitStr = code.substring(code.length() - 1);

        Rank rank = Rank.fromSymbol(rankStr); // You'll need this to exist
        Suit suit = Suit.fromSymbol(suitStr); // You'll need this too

        gs.setRank(rank);
        gs.setSuit(suit);
    }

}
