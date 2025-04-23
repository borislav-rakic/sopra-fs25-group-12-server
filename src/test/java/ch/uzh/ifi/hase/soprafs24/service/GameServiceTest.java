package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.model.Card;
import ch.uzh.ifi.hase.soprafs24.model.CardResponse;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeast;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.constant.Rank;
import ch.uzh.ifi.hase.soprafs24.constant.Suit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.given;
import java.util.Optional;
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
    private GameStatsRepository gameStatsRepository = Mockito.mock(GameStatsRepository.class);

    @Mock
    private ExternalApiClientService externalApiClientService = Mockito.mock(ExternalApiClientService.class);

    @MockBean
    private UserService userService = Mockito.mock(UserService.class);

    @MockBean
    private GameStatsService gameStatsService = Mockito.mock(GameStatsService.class);

    @Mock
    private PassedCardRepository passedCardRepository;

    @Mock
    private MatchPlayerCardsRepository matchPlayerCardsRepository;

    @InjectMocks
    private GameService gameService = new GameService(
            matchRepository,
            userRepository,
            matchPlayerRepository,
            gameStatsRepository,
            gameRepository,
            matchPlayerCardsRepository,
            passedCardRepository,
            externalApiClientService,
            userService,
            gameStatsService);

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

        List<MatchPlayerCards> matchPlayerCards = new ArrayList<>();
        MatchPlayerCards matchPlayerCard = new MatchPlayerCards();
        matchPlayerCard.setMatchPlayer(matchPlayer);
        matchPlayerCard.setCard("5C");
        matchPlayerCards.add(matchPlayerCard);

        matchPlayers.get(0).setCardsInHand(matchPlayerCards);

        match.setMatchPlayers(matchPlayers);
        match.setHost(user.getUsername());
        match.setMatchGoal(100);
        match.setStarted(false);
        match.setPlayer1(user);

        Game game = new Game();
        game.setGameId(1L);
        game.setMatch(match);
        game.setGameNumber(1);

        match.getGames().add(game);

        matchPlayer = new MatchPlayer();
        matchPlayer.setMatch(match);
        matchPlayer.setPlayerId(user);
        matchPlayer.setMatchPlayerId(1L);

    }

    @Test
    public void testGetPlayerMatchInformationSuccess() {
        // Create actual User instances
        User user = new User();
        user.setId(42L);
        user.setUsername("testuser");

        User p2 = new User();
        p2.setId(2L);
        p2.setUsername("bot2");
        User p3 = new User();
        p3.setId(3L);
        p3.setUsername("bot3");
        User p4 = new User();
        p4.setId(4L);
        p4.setUsername("bot4");

        // Create Match and assign users
        Match match = new Match();
        match.setMatchId(1L);
        match.setPlayer1(user);
        match.setPlayer2(p2);
        match.setPlayer3(p3);
        match.setPlayer4(p4);
        match.setHost("hostUser");
        match.setMatchGoal(100);
        match.setPhase(MatchPhase.READY);
        match.setCurrentSlot(1);
        match.setAiPlayers(new HashMap<>());

        // Setup MatchPlayer
        MatchPlayer matchPlayer = new MatchPlayer();
        matchPlayer.setPlayerId(user);
        MatchPlayerCards handCard = new MatchPlayerCards();
        handCard.setCard("2C");
        matchPlayer.setCardsInHand(List.of(handCard));
        match.setMatchPlayers(List.of(matchPlayer));

        // Setup Game
        Game game = new Game();
        game.setGameId(1L);
        game.setGameNumber(1);
        game.setPhase(GamePhase.FINISHED);
        game.setMatch(match);

        // Simulate a complete trick with 4 cards — all Clubs (no points)
        GameStats gs1 = new GameStats();
        gs1.setRank(Rank._2);
        gs1.setSuit(Suit.C);
        gs1.setPlayedBy(1);
        GameStats gs2 = new GameStats();
        gs2.setRank(Rank._3);
        gs2.setSuit(Suit.C);
        gs2.setPlayedBy(2);
        GameStats gs3 = new GameStats();
        gs3.setRank(Rank._4);
        gs3.setSuit(Suit.C);
        gs3.setPlayedBy(3);
        GameStats gs4 = new GameStats();
        gs4.setRank(Rank._5);
        gs4.setSuit(Suit.C);
        gs4.setPlayedBy(4);

        List<GameStats> playedCards = List.of(gs1, gs2, gs3, gs4);
        game.setPlayedCards(new ArrayList<>(playedCards));
        match.setGames(List.of(game));

        // Mocks
        when(userRepository.findUserByToken("1234")).thenReturn(user);
        when(matchRepository.findMatchByMatchId(1L)).thenReturn(match);
        when(matchPlayerRepository.findByUserAndMatch(user, match)).thenReturn(matchPlayer);

        // Act
        PlayerMatchInformationDTO result = gameService.getPlayerMatchInformation("1234", 1L);

        // Assert general match/game info
        assertEquals(1L, result.getMatchId());
        assertEquals("hostUser", result.getHost());
        assertEquals(100, result.getMatchGoal());
        assertEquals(GamePhase.FINISHED, result.getGamePhase());
        assertEquals(MatchPhase.READY, result.getMatchPhase());
        assertEquals(List.of("testuser", "bot2", "bot3", "bot4"), result.getMatchPlayers());

        // Assert hand
        assertEquals(1, result.getPlayerCards().size());
        assertEquals("2C", result.getPlayerCards().get(0).getCard());

        // Assert current trick (should include 4 cards)
        assertNotNull(result.getCurrentTrick());
        assertEquals(4, result.getCurrentTrick().size());
        assertEquals("2", result.getCurrentTrick().get(0).getRank());
        assertEquals("Clubs", result.getCurrentTrick().get(0).getSuit());

        // Assert trick winner (slot 4 played highest Club = 5C)
        assertEquals(4, result.getLastTrickWinnerSlot());
        assertEquals(0, result.getLastTrickPoints()); // no points in this trick

        // Assert trick leader (first player was slot 1)
        assertEquals(1, result.getTrickLeaderSlot());
    }

    @Test
    public void testStartMatchSuccess() {
        // Mocks
        User user = Mockito.mock(User.class);
        Match match = new Match();
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
        Mockito.when(user.getUsername()).thenReturn("hostUser");
        Mockito.when(user.getId()).thenReturn(1L);
        match.setHost("hostUser");
        match.setPlayer1(user);
        match.setInvites(Map.of(1, 1L));
        match.setJoinRequests(Map.of(1L, "accepted"));
        match.setAiPlayers(Map.of(1, 0, 2, 1, 3, 2));
        match.setMatchPlayers(List.of(matchPlayer));
        match.setGames(new ArrayList<>());

        // Simulate game creation with ID
        Game savedGame = new Game();
        savedGame.setGameId(42L);
        savedGame.setMatch(match);

        Mockito.when(gameRepository.save(Mockito.any(Game.class))).thenAnswer(invocation -> {
            Game game = invocation.getArgument(0);
            game.setGameId(42L);
            return game;
        });

        Mockito.when(gameRepository.findById(42L)).thenReturn(Optional.of(savedGame));

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
        gameService.startMatch(1L, "1234");

        // VERIFY
        Mockito.verify(matchRepository, Mockito.atLeast(2)).save(Mockito.any());
        Mockito.verify(gameRepository, Mockito.atLeastOnce()).save(Mockito.any());
        Mockito.verify(externalApiClientService).createNewDeck();
        Mockito.verify(externalApiClientService).drawCard("9876", 52);
        Mockito.verify(gameStatsService, Mockito.atLeastOnce()).initializeGameStats(Mockito.any(), Mockito.any());
    }

    private CardResponse createCard(String code) {
        CardResponse card = new CardResponse();
        card.setCode(code);
        return card;
    }

}
