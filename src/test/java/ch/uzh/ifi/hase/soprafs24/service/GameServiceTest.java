package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.AiMatchPlayerState;
import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.constant.Strategy;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
    private GameTrickService gameTrickService = Mockito.mock(GameTrickService.class);

    @Mock
    private MatchMessageService matchMessageService = Mockito.mock(MatchMessageService.class);

    @Mock
    private MatchRepository matchRepository = Mockito.mock(MatchRepository.class);

    @Mock
    private MatchPlayerRepository matchPlayerRepository = Mockito.mock(MatchPlayerRepository.class);

    @Mock
    private MatchSummaryService matchSummaryService = Mockito.mock(MatchSummaryService.class);

    @InjectMocks
    private GameService gameService = new GameService(

            aiPlayingService,
            cardPassingService,
            cardRulesService,
            gameRepository,
            gameStatsService,
            gameTrickService,
            matchMessageService,
            matchRepository,
            matchPlayerRepository,
            matchSummaryService);

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
    public void testPlaySingleAiTurn_executesTurnSuccessfully() {
        // Arrange
        game.setCurrentMatchPlayerSlot(2);
        game.setPhase(GamePhase.FINALTRICK);

        User userAI = new User();
        userAI.setUsername("AI1");
        userAI.setIsAiPlayer(true);

        MatchPlayer aiPlayer = new MatchPlayer();
        aiPlayer.setUser(userAI);
        aiPlayer.setMatch(match);
        aiPlayer.setMatchPlayerSlot(2);
        aiPlayer.setAiMatchPlayerState(AiMatchPlayerState.THINKING);
        aiPlayer.setHand("3C");
        aiPlayer.setIsAiPlayer(true);

        match.setMatchPlayers(new ArrayList<>(List.of(aiPlayer)));

        given(aiPlayingService.selectCardToPlay(any(), any(), any())).willReturn("3C");
        doNothing().when(cardRulesService).validateMatchPlayerCardCode(any(), any(), any());
        doNothing().when(gameStatsService).recordCardPlay(any(), any(), any());
        given(matchPlayerRepository.findByMatchAndMatchPlayerSlot(any(), anyInt())).willReturn(aiPlayer);

        // Act
        boolean result = gameService.playSingleAiTurn(match, game, aiPlayer);

        // Assert
        assertTrue(result);
        assertEquals(AiMatchPlayerState.READY, aiPlayer.getAiMatchPlayerState());
        verify(aiPlayingService).selectCardToPlay(eq(game), eq(aiPlayer), eq(Strategy.LEFTMOST));
        verify(cardRulesService).validateMatchPlayerCardCode(eq(game), eq(aiPlayer), eq("3C"));
        verify(gameStatsService).recordCardPlay(eq(game), eq(aiPlayer), eq("3C"));
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
