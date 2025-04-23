package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.model.Card;
import ch.uzh.ifi.hase.soprafs24.model.CardResponse;
import ch.uzh.ifi.hase.soprafs24.model.DrawCardResponse;
import ch.uzh.ifi.hase.soprafs24.model.NewDeckResponse;
import ch.uzh.ifi.hase.soprafs24.repository.*;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePassingDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayedCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayerMatchInformationDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;

import static org.mockito.BDDMockito.given;

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
import static org.mockito.Mockito.*;

import java.util.Optional;

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
    private PassedCardRepository passedCardRepository = Mockito.mock(PassedCardRepository.class);

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
    private Game game;

    @BeforeEach
    public void setup() {
        user = new User();
        user.setUsername("username");
        user.setPassword("password");
        user.setId(1L);
        user.setToken("1234");

        match = new Match();

        matchPlayer = new MatchPlayer();
        matchPlayer.setPlayerId(user);
        matchPlayer.setMatch(match);
        matchPlayer.setSlot(1);

        MatchPlayerCards matchPlayerCard = new MatchPlayerCards();
        matchPlayerCard.setMatchPlayer(matchPlayer);
        matchPlayerCard.setCard("5C");

        List<MatchPlayerCards> matchPlayerCards = new ArrayList<>();
        matchPlayer.setCardsInHand(matchPlayerCards);
        matchPlayer.getCardsInHand().add(matchPlayerCard);

        List<MatchPlayer> matchPlayers = new ArrayList<>();
        matchPlayers.add(matchPlayer);

        match.setMatchPlayers(matchPlayers);
        match.setHost(user.getUsername());
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
        assertEquals("2C", result.getPlayerCards().get(0).getCard().getCode());

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

    @Test
    public void testPlayCard() {
        List<GameStats> gameStatsList = new ArrayList<>();

        // Adds 51 different cards to game.playedCards
        for (Rank rank : Rank.values()) {
            for (Suit suit : Suit.values()) {
                String rankSuit = rank.toString() + suit.toString();
                if (!rankSuit.equals("5C")) {
                    GameStats gameStats = new GameStats();
                    gameStats.setGame(game);
                    gameStats.setRank(rank);
                    gameStats.setSuit(suit);
                    gameStatsList.add(gameStats);
                }
            }
        }

        game.getPlayedCards().addAll(gameStatsList);

        PlayedCardDTO playedCardDTO = new PlayedCardDTO();
        playedCardDTO.setCard("5C");
        playedCardDTO.setGameId(game.getGameId());
        playedCardDTO.setPlayerId(user.getId());

        given(userService.getUserByToken(Mockito.any())).willReturn(user);

        given(gameRepository.findById(Mockito.any())).willReturn(Optional.of(game));

        given(matchPlayerRepository.findByUserAndMatch(Mockito.any(), Mockito.any())).willReturn(matchPlayer);
        given(matchPlayerRepository.save(Mockito.any())).willReturn(matchPlayer);

        given(gameStatsRepository.save(Mockito.any())).willReturn(new GameStats());

        given(gameRepository.save(Mockito.any())).willReturn(game);

        gameService.playCard("1234", 1L, playedCardDTO);

        verify(matchPlayerRepository).save(Mockito.any());
        verify(gameStatsRepository).save(Mockito.any());
        verify(gameRepository).save(Mockito.any());
    }

    @Test
    public void testMakePassingHappen() {
        List<GameStats> gameStatsList = new ArrayList<>();

        GameStats gameStats = new GameStats();
        gameStats.setGame(game);
        gameStats.setRank(Rank._3);
        gameStats.setSuit(Suit.H);
        gameStatsList.add(gameStats);

        gameStats = new GameStats();
        gameStats.setGame(game);
        gameStats.setRank(Rank._0);
        gameStats.setSuit(Suit.H);
        gameStatsList.add(gameStats);

        gameStats = new GameStats();
        gameStats.setGame(game);
        gameStats.setRank(Rank._0);
        gameStats.setSuit(Suit.S);
        gameStatsList.add(gameStats);

        List<String> cards = new ArrayList<>();
        cards.add("3H");
        cards.add("0H");
        cards.add("0S");

        GamePassingDTO gamePassingDTO = new GamePassingDTO();
        gamePassingDTO.setCards(cards);
        gamePassingDTO.setGameId(game.getGameId());
        gamePassingDTO.setPlayerId(user.getId());

        given(userService.getUserByToken(Mockito.any())).willReturn(user);

        given(gameRepository.findGameByMatch_MatchId(Mockito.any())).willReturn(game);

        // Returns GameStats entry 3H first, then on the second call 0H, and then 0S
        given(gameStatsRepository.findByRankSuitAndGameAndCardHolder(Mockito.anyString(), Mockito.any(Game.class), Mockito.anyInt()))
                .willReturn(gameStatsList.get(0), gameStatsList.get(1), gameStatsList.get(2));

        given(passedCardRepository.existsByGameAndRankSuit(Mockito.any(), Mockito.any())).willReturn(false);

        given(passedCardRepository.save(Mockito.any())).willReturn(new PassedCard());

        gameService.makePassingHappen(1L, gamePassingDTO, "1234");

        verify(passedCardRepository, Mockito.times(3)).save(Mockito.any());
    }

    @Test
    public void testCollectPassedCards() {
        User user1 = user;
        User user2 = new User();
        user2.setId(2L);
        match.setPlayer2(user2);
        User user3 = new User();
        user3.setId(3L);
        match.setPlayer3(user3);
        User user4 = new User();
        user4.setId(4L);
        match.setPlayer4(user4);

        User fromPlayer = user1;
        User toPlayer = user2;

        int counter = 0;

        List<PassedCard> passedCardsList = new ArrayList<>();

        for (Rank rank : Rank.values()) {
            for (Suit suit : Suit.values()) {
                String rankSuit = rank.toString() + suit.toString();

                PassedCard passedCard = new PassedCard();
                passedCard.setGame(game);
                passedCard.setGameNumber(game.getGameNumber());
                passedCard.setFromPlayer(fromPlayer);
                passedCard.setRankSuit(rankSuit);

                passedCardsList.add(passedCard);

                counter++;
                if (counter == 3 || counter == 6 || counter == 8) {
                    break;
                }
            }

            if (counter == 3) {
                fromPlayer = user2;
                toPlayer = user3;
            } else if (counter == 6) {
                fromPlayer = user3;
                toPlayer = user4;
            } else if (counter == 8) {
                fromPlayer = user4;
                toPlayer = user1;
            } else {
                break;
            }
        }

        given(passedCardRepository.findByGame(Mockito.any())).willReturn(passedCardsList);

        given(gameStatsRepository.findByRankSuitAndGameAndCardHolder(Mockito.anyString(), Mockito.any(Game.class), Mockito.anyInt()))
            .willReturn(new GameStats());
        given(gameStatsRepository.save(Mockito.any())).willReturn(new GameStats());

        doNothing().when(passedCardRepository).deleteAll();

        gameService.collectPassedCards(game);

        verify(passedCardRepository).deleteAll(Mockito.any());
    }

    private CardResponse createCard(String code) {
        CardResponse card = new CardResponse();
        card.setCode(code);
        return card;
    }

}
