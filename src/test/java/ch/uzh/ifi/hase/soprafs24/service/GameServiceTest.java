package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.repository.*;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePassingDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PollingDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class GameServiceTest {
    @Mock
    private AiPlayingService aiPlayingService = Mockito.mock(AiPlayingService.class);

    @Mock
    private CardPassingService cardPassingService = Mockito.mock(CardPassingService.class);

    @Mock
    private CardRulesService cardRulesService = Mockito.mock(CardRulesService.class);

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

    @InjectMocks
    private GameService gameService = new GameService(

            aiPlayingService,
            cardPassingService,
            cardRulesService,
            gameRepository,
            gameStatsService,
            matchPlayerRepository,
            matchRepository);

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
        matchPlayer.setMatchPlayerSlot(1);

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
        game.setCurrentMatchPlayerSlot(1);

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
        matchPlayer.setMatchPlayerSlot(1);

        MatchPlayer mp2 = new MatchPlayer();
        mp2.setUser(p2);
        mp2.setMatchPlayerSlot(2);

        MatchPlayer mp3 = new MatchPlayer();
        mp3.setUser(p3);
        mp3.setMatchPlayerSlot(3);

        MatchPlayer mp4 = new MatchPlayer();
        mp4.setUser(p4);
        mp4.setMatchPlayerSlot(4);

        matchPlayer.setGameScore(0);
        mp2.setGameScore(0);
        mp3.setGameScore(0);
        mp4.setGameScore(0);

        match.setMatchPlayers(List.of(matchPlayer, mp2, mp3, mp4));

        // === Setup Game ===
        Game game = new Game();
        game.setGameId(1L);
        game.setGameNumber(1);
        game.setPhase(GamePhase.PASSING);
        game.setMatch(match);
        game.setCurrentTrickNumber(2);
        game.setPreviousTrickWinnerMatchPlayerSlot(4);
        game.setPreviousTrickPoints(0);
        game.setTrickLeaderMatchPlayerSlot(1);
        game.setHeartsBroken(false);
        game.setCurrentMatchPlayerSlot(1);

        matchPlayer.setHand("2C");

        // === Tie Game to Match ===
        match.setGames(List.of(game));

        // === Mock Repositories ===
        when(matchRepository.findMatchByMatchId(1L)).thenReturn(match);
        when(matchPlayerRepository.findByUserAndMatch(user, match)).thenReturn(matchPlayer);
        when(gameRepository.findFirstByMatchAndPhaseNotIn(eq(match), anyList())).thenReturn(game);
        when(matchPlayerRepository.findByUserAndMatchAndMatchPlayerSlot(user, match, 1)).thenReturn(matchPlayer);

        // === Act ===
        System.out.println("Players in match: " + match.getMatchPlayers().size());
        PollingDTO result = gameService.getPlayerPolling(user, match);

        // === Assert ===
        assertNotNull(result);
        assertEquals(1L, result.getMatchId());
        assertEquals(4, result.getHostId());
        assertEquals(100, result.getMatchGoal());
        // assertEquals(GamePhase.FIRSTTRICK, result.getGamePhase());
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
        assertEquals(4, result.getPreviousTrickWinnerMatchPlayerSlot());
        assertEquals(0, result.getPreviousTrickPoints());
        assertEquals(1, result.getCurrentTrickLeaderMatchPlayerSlot());
    }

    /*
     * @Test
     * public void testStartMatchSuccess() {
     * // Create and link the Match
     * Match match = new Match();
     * match.setMatchId(1L);
     * 
     * Game game = new Game();
     * game.setGameId(42L);
     * game.setMatch(match);
     * 
     * // Mock repository lookups
     * given(matchRepository.findById(1L)).willReturn(Optional.of(match));
     * given(gameRepository.findById(42L)).willReturn(Optional.of(game));
     * 
     * // Mocks
     * User user = Mockito.mock(User.class);
     * 
     * MatchPlayer matchPlayer = Mockito.mock(MatchPlayer.class);
     * 
     * User bot2 = mock(User.class);
     * User bot3 = mock(User.class);
     * User bot4 = mock(User.class);
     * 
     * given(bot2.getUsername()).willReturn("bot2");
     * given(bot3.getUsername()).willReturn("bot3");
     * given(bot4.getUsername()).willReturn("bot4");
     * 
     * User p2 = mock(User.class);
     * User p3 = mock(User.class);
     * User p4 = mock(User.class);
     * 
     * when(p2.getUsername()).thenReturn("bot2");
     * when(p3.getUsername()).thenReturn("bot3");
     * when(p4.getUsername()).thenReturn("bot4");
     * 
     * match.setPlayer2(p2);
     * match.setPlayer3(p3);
     * match.setPlayer4(p4);
     * 
     * // Mock basic user and match setup
     * Mockito.when(user.getId()).thenReturn(4L);
     * match.setHostId(4L);
     * match.setPlayer1(user);
     * match.setInvites(Map.of(1, 1L));
     * match.setJoinRequests(Map.of(1L, "accepted"));
     * match.setAiPlayers(Map.of(1, 0, 2, 1, 3, 2));
     * match.setMatchPlayers(List.of(matchPlayer));
     * match.setGames(new ArrayList<>());
     * 
     * // Simulate game creation with ID
     * Game savedGame = new Game();
     * savedGame.setGameId(1L);
     * savedGame.setMatch(match);
     * 
     * Mockito.when(gameRepository.save(Mockito.any(Game.class))).thenAnswer(
     * invocation -> {
     * Game game2 = invocation.getArgument(0);
     * game2.setGameId(1L);
     * game2.setMatch(match);
     * return game2;
     * });
     * 
     * Mockito.when(gameRepository.findById(1L)).thenReturn(Optional.of(savedGame));
     * 
     * // Mock deck and cards
     * NewDeckResponse newDeckResponse = new NewDeckResponse();
     * newDeckResponse.setDeck_id("9876");
     * newDeckResponse.setSuccess(true);
     * newDeckResponse.setRemaining(GameConstants.FULL_DECK_CARD_COUNT);
     * newDeckResponse.setShuffled(true);
     * 
     * DrawCardResponse drawCardResponse = new DrawCardResponse();
     * drawCardResponse.setDeck_id("9876");
     * drawCardResponse.setSuccess(true);
     * drawCardResponse.setRemaining(0);
     * drawCardResponse.setCards(new ArrayList<>(List.of(
     * createCard("3H"),
     * createCard("0H"),
     * createCard("0S"),
     * createCard("0D"),
     * createCard("0C"))));
     * 
     * // Mock repositories
     * Mockito.when(matchRepository.findMatchByMatchId(1L)).thenReturn(match);
     * Mockito.when(matchPlayerRepository.save(Mockito.any())).thenReturn(
     * matchPlayer);
     * Mockito.when(gameStatsRepository.findByRankSuit(Mockito.any())).thenReturn(
     * new GameStats());
     * Mockito.when(gameStatsRepository.save(Mockito.any())).thenReturn(new
     * GameStats());
     * 
     * // Important: repeat this to support the async call in subscribe()
     * Mockito.when(matchRepository.findMatchByMatchId(Mockito.any())).thenReturn(
     * match);
     * 
     * // Mock external API calls
     * Mockito.when(externalApiClientService.createNewDeck()).thenReturn(Mono.just(
     * newDeckResponse));
     * Mockito.when(externalApiClientService.drawCard("9876",
     * GameConstants.FULL_DECK_CARD_COUNT))
     * .thenReturn(Mono.just(drawCardResponse));
     * match.setPhase(MatchPhase.READY);
     * // gameService.startMatch(1L, "1234", null);
     * // THIS TEST NEEDS A COMPLETE OVERHAUL!
     * 
     * // VERIFY
     * // Mockito.verify(matchRepository,
     * Mockito.atLeastOnce()).save(Mockito.any());
     * Mockito.verify(gameRepository, Mockito.atLeastOnce()).save(Mockito.any());
     * Mockito.verify(externalApiClientService).createNewDeck();
     * Mockito.verify(externalApiClientService).drawCard("9876",
     * GameConstants.FULL_DECK_CARD_COUNT);
     * Mockito.verify(gameStatsService,
     * Mockito.atLeastOnce()).initializeGameStats(Mockito.any(), Mockito.any());
     * }
     */

    @Test
    public void testPassingDelegatesToCardPassingService() {
        // Arrange
        Game game = new Game();
        MatchPlayer player = new MatchPlayer();
        GamePassingDTO dto = new GamePassingDTO();

        // Act
        gameService.passingAcceptCards(game, player, dto, false);

        // Assert
        verify(cardPassingService).passingAcceptCards(game, player, dto, false);
        verify(gameRepository).saveAndFlush(game);
    }

}
