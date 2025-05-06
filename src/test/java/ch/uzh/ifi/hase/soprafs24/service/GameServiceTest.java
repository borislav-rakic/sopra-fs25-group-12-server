package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.AiMatchPlayerState;
import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.repository.*;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePassingDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
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
    private GameStatsService gameStatsService = Mockito.mock(GameStatsService.class);

    @Mock
    private MatchMessageService matchMessageService = Mockito.mock(MatchMessageService.class);

    @Mock
    private MatchRepository matchRepository = Mockito.mock(MatchRepository.class);

    @Mock
    private MatchPlayerRepository matchPlayerRepository = Mockito.mock(MatchPlayerRepository.class);

    @InjectMocks
    private GameService gameService = new GameService(

            aiPlayingService,
            cardPassingService,
            cardRulesService,
            gameRepository,
            gameStatsService,
            matchMessageService,
            matchRepository,
            matchPlayerRepository);

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
    public void testPlayAiTurnsUntilHuman() {
        game.setCurrentMatchPlayerSlot(2);
        game.setPhase(GamePhase.FINALTRICK);

        User userAI1 = new User();
        userAI1.setUsername("AI1");
        userAI1.setIsAiPlayer(true);

        MatchPlayer matchPlayerAI1 = new MatchPlayer();
        matchPlayerAI1.setUser(userAI1);
        matchPlayerAI1.setMatch(match);
        matchPlayerAI1.setMatchPlayerSlot(2);
        matchPlayerAI1.setAiMatchPlayerState(AiMatchPlayerState.THINKING);
        matchPlayerAI1.setHand("3C");
        matchPlayerAI1.setIsAiPlayer(true);

        match.getMatchPlayers().add(matchPlayerAI1);

        given(gameRepository.findGameByGameId(Mockito.any())).willReturn(game);
        given(aiPlayingService.selectCardToPlay(Mockito.any(), Mockito.any(), Mockito.any())).willReturn("3C", "4C",
                "5C");

        Mockito.doNothing().when(cardRulesService).validateMatchPlayerCardCode(Mockito.any(), Mockito.any(),
                Mockito.any());
        Mockito.doNothing().when(gameStatsService).recordCardPlay(Mockito.any(), Mockito.any(), Mockito.any());

        gameService.playAiTurnsUntilHuman(game.getGameId());

        verify(gameRepository).findGameByGameId(Mockito.any());
        verify(aiPlayingService).selectCardToPlay(Mockito.any(), Mockito.any(), Mockito.any());
        verify(cardRulesService).validateMatchPlayerCardCode(Mockito.any(), Mockito.any(), Mockito.any());
        verify(gameStatsService).recordCardPlay(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testPlayCardAsHuman() {
        match.setPhase(MatchPhase.IN_PROGRESS);
        game.setCurrentMatchPlayerSlot(1);
        game.setPhase(GamePhase.NORMALTRICK);
        game.setCurrentPlayOrder(4);
        game.setCurrentTrickNumber(2);

        Mockito.doNothing().when(cardRulesService).validateMatchPlayerCardCode(Mockito.any(), Mockito.any(),
                Mockito.any());
        Mockito.doNothing().when(gameStatsService).recordCardPlay(Mockito.any(), Mockito.any(), Mockito.any());

        gameService.playCardAsHuman(game, matchPlayer, "5C");

        verify(cardRulesService).validateMatchPlayerCardCode(Mockito.any(), Mockito.any(), Mockito.any());
        verify(gameStatsService).recordCardPlay(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testUpdateGameBasedOnPlayOrderFINALTRICK() {
        game.setPhase(GamePhase.FINALTRICK);
        game.setCurrentPlayOrder(52);

        GamePhase expected = GamePhase.RESULT;

        gameService.updateGamePhaseBasedOnPlayOrder(game);

        assertEquals(expected, game.getPhase());
    }

    @Test
    public void testUpdateGameBasedOnPlayOrderNORMALTRICKBeforeFINALTRICK() {
        game.setPhase(GamePhase.NORMALTRICK);
        game.setCurrentPlayOrder(48);

        GamePhase expected = GamePhase.FINALTRICK;

        gameService.updateGamePhaseBasedOnPlayOrder(game);

        assertEquals(expected, game.getPhase());
    }

    @Test
    public void testUpdateGameBasedOnPlayOrderFIRSTTRICK() {
        game.setPhase(GamePhase.FIRSTTRICK);
        game.setCurrentPlayOrder(4);

        GamePhase expected = GamePhase.NORMALTRICK;

        gameService.updateGamePhaseBasedOnPlayOrder(game);

        assertEquals(expected, game.getPhase());
    }

    @Test
    public void testUpdateGameBasedOnPlayOrderPASSING() {
        game.setPhase(GamePhase.PASSING);
        game.setCurrentPlayOrder(0);

        GamePhase expected = GamePhase.FIRSTTRICK;

        gameService.updateGamePhaseBasedOnPlayOrder(game);

        assertEquals(expected, game.getPhase());
    }

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
