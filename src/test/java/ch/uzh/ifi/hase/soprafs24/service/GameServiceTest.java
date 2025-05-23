package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.*;
import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.repository.*;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePassingDTO;
import ch.uzh.ifi.hase.soprafs24.util.CardUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
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

    @Mock
    private UserRepository userRepository = Mockito.mock(UserRepository.class);

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
            matchSummaryService,
            userRepository);

    private Match match;
    private User user;
    private MatchPlayer matchPlayer;
    private Game game;

    @BeforeEach
    public void setup() {
        match = new Match();
        match.setHostId(4L);
        match.setMatchGoal(100);
        match.setStarted(false);

        List<MatchPlayer> matchPlayersList = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            User user = new User();
            user.setId((long) i + 1);
            user.setUsername("Person" + i);

            MatchPlayer mp = new MatchPlayer();
            mp.setUser(user);
            mp.setMatch(match);
            mp.setIsAiPlayer(false);
            mp.setMatchPlayerSlot(i + 1);
            mp.setGameScore(i == 0 ? 26 : 0); // Simulate a moon shot by first player

            if (i == 0) {
                mp.addCardCodeToHand("5C");
                mp.addCardCodeToHand("KH");
                mp.addCardCodeToHand("7S");
            }

            matchPlayersList.add(mp);

            if (i == 0) {
                matchPlayer = mp; // Save for reference in test
                match.setPlayer1(user); // Optional depending on game logic
            }
        }

        match.setMatchPlayers(matchPlayersList);

        game = new Game();
        game.setGameId(1L);
        game.setPhase(GamePhase.NORMALTRICK);
        game.setTrickPhase(TrickPhase.TRICKJUSTCOMPLETED);
        game.setTrickJustCompletedTime(Instant.now().minusMillis(1600));
        game.setCurrentTrick(new ArrayList<>());
        game.setPreviousTrick(new ArrayList<>());
        game.setMatch(match);
        game.setGameNumber(1);
        game.setCurrentMatchPlayerSlot(1);

        match.addGame(game);

        user = matchPlayersList.get(0).getUser(); // if you still need `user` reference
        user.setPassword("password");
        user.setToken("1234");
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

    @Test
    public void testAdvanceTrickPhaseIfOwnerPollingTrickJustCompleted() {
        given(gameRepository.save(Mockito.any())).willReturn(game);
        given(cardRulesService.trickConsistsOnlyOfHearts(Mockito.any())).willReturn(true);
        doNothing().when(matchMessageService).addMessage(Mockito.any(), Mockito.any(), Mockito.any());

        gameService.advanceTrickPhaseIfOwnerPolling(game);

        verify(gameRepository).save(Mockito.any());
        verify(cardRulesService).trickConsistsOnlyOfHearts(Mockito.any());
        verify(matchMessageService).addMessage(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testAdvanceTrickPhaseIfOwnerPollingProcessingTrick() {
        game.setTrickPhase(TrickPhase.PROCESSINGTRICK);
        game.setPhase(GamePhase.NORMALTRICK);
        game.setCurrentPlayOrder(48);

        doNothing().when(gameTrickService).clearTrick(Mockito.any(), Mockito.any());
        doNothing().when(gameTrickService).updateGamePhaseBasedOnPlayOrder(Mockito.any());
        doNothing().when(matchMessageService).addMessage(Mockito.any(), Mockito.any(), Mockito.any());

        given(gameRepository.save(Mockito.any())).willReturn(game);

        gameService.advanceTrickPhaseIfOwnerPolling(game);

        verify(gameTrickService).clearTrick(Mockito.any(), Mockito.any());
        verify(gameTrickService).updateGamePhaseBasedOnPlayOrder(Mockito.any());
        verify(matchMessageService).addMessage(Mockito.any(), Mockito.any(), Mockito.any());
        verify(gameRepository).save(Mockito.any());
    }

    @Test
    public void testFinalizeGameIfCompleteNotResultPhase() {
        game.setPhase(GamePhase.NORMALTRICK);

        boolean expected = false;

        boolean result = gameService.finalizeGameIfComplete(game);

        assertEquals(expected, result);
    }

    @Test
    public void testFinalizeGameIfCompleteResultPhase() {
        game.setPhase(GamePhase.RESULT);
        game.setCurrentPlayOrder(4);

        boolean expected = false;

        boolean result = gameService.finalizeGameIfComplete(game);

        assertEquals(expected, result);
    }

    @Test
    public void testFinalizeGameScoresMoonShot() {
        // Mock repository and service calls using the already-initialized match and
        // game
        given(matchPlayerRepository.findByMatch(Mockito.any())).willReturn(match.getMatchPlayers());
        given(gameRepository.save(Mockito.any())).willReturn(game);
        given(matchPlayerRepository.save(Mockito.any())).willAnswer(invocation -> invocation.getArgument(0)); // Return
                                                                                                              // the
                                                                                                              // saved
                                                                                                              // player
        given(matchRepository.save(Mockito.any())).willReturn(match);
        doNothing().when(matchSummaryService).saveGameResultHtml(Mockito.any(), Mockito.any(), Mockito.any());

        // Act
        gameService.finalizeGameScores(game);

        // Assert: Verify expected repository and service interactions
        verify(matchPlayerRepository).findByMatch(Mockito.any());
        verify(gameRepository).save(Mockito.any());
        verify(matchPlayerRepository, atLeastOnce()).save(Mockito.any()); // Usually each player is saved
        verify(matchSummaryService).saveGameResultHtml(Mockito.any(), Mockito.any(), Mockito.any());
        verify(matchRepository).save(Mockito.any());
    }

    @Test
    public void testResetNonAiPlayersReady() {
        match.getMatchPlayers().get(0).setReady(true);

        given(matchRepository.save(Mockito.any())).willReturn(match);

        boolean expected = false;

        gameService.resetNonAiPlayersReady(game);

        verify(matchRepository).save(Mockito.any());
        assertEquals(expected, match.getMatchPlayers().get(0).getIsReady());
    }

    @Test
    public void testPassingAcceptCards() {
        GamePassingDTO gamePassingDTO = new GamePassingDTO();
        List<String> cards = Arrays.asList("6C", "7C", "8C");
        gamePassingDTO.setCards(cards);
        gamePassingDTO.setPlayerId(1L);

        matchPlayer.addCardCodeToHand("2C");

        given(cardPassingService.passingAcceptCards(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean()))
                .willReturn(12);

        doNothing().when(cardPassingService).collectPassedCards(Mockito.any());

        given(gameRepository.save(Mockito.any())).willReturn(game);
        given(gameRepository.saveAndFlush(Mockito.any())).willReturn(game);

        gameService.passingAcceptCards(game, matchPlayer, gamePassingDTO, true);

        verify(cardPassingService).passingAcceptCards(Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.anyBoolean());
        verify(cardPassingService).collectPassedCards(Mockito.any());
        verify(gameRepository).save(Mockito.any());
        verify(gameRepository).saveAndFlush(Mockito.any());
    }

    @Test
    public void testAssignTwoOfClubsLeaderError() {
        assertThrows(
                IllegalStateException.class,
                () -> gameService.assignTwoOfClubsLeader(game),
                "Expected IllegalStateException to be thrown");
    }

    @Test
    public void testResetAllPlayersReady() {
        // Use the existing match players set in @BeforeEach
        for (MatchPlayer mp : match.getMatchPlayers()) {
            mp.setReady(true); // Make them ready so the reset has something to change
        }

        given(matchRepository.findById(Mockito.any())).willReturn(Optional.of(match));

        // Act
        gameService.resetAllPlayersReady(1L);

        // Verify
        verify(matchRepository).findById(Mockito.any());

        for (MatchPlayer mp : match.getMatchPlayers()) {
            assertFalse(mp.getIsReady(), "Player should be reset to not ready");
        }
    }

    @Test
    public void testAssertConsistentGameStateError1() {
        game.setPhase(GamePhase.NORMALTRICK);
        match.setPhase(MatchPhase.RESULT);

        assertThrows(
                IllegalStateException.class,
                () -> gameService.assertConsistentGameState(game),
                "Expected IllegalStateException to be thrown");
    }

    @Test
    public void testAssertConsistentGameStateError2() {
        game.setPhase(GamePhase.NORMALTRICK);
        match.setPhase(MatchPhase.IN_PROGRESS);
        game.setCurrentPlayOrder(2);
        game.setCurrentPlayOrder(3);

        assertThrows(
                IllegalStateException.class,
                () -> gameService.assertConsistentGameState(game),
                "Expected IllegalStateException to be thrown");
    }

    @Test
    public void testDoPlayOrderAndTrickPhaseMatchFirstTrickError() {
        game.setPhase(GamePhase.FIRSTTRICK);
        game.setCurrentPlayOrder(-1);
        game.setTrickPhase(TrickPhase.TRICKJUSTCOMPLETED);

        assertThrows(
                IllegalStateException.class,
                () -> gameService.doPlayOrderAndTrickPhaseMatch(game),
                "Expected IllegalStateException to be thrown");
    }

    @Test
    public void testDoPlayOrderAndTrickPhaseMatchNormalTrickError() {
        game.setPhase(GamePhase.NORMALTRICK);
        game.setCurrentPlayOrder(-1);
        game.setTrickPhase(TrickPhase.TRICKJUSTCOMPLETED);

        assertThrows(
                IllegalStateException.class,
                () -> gameService.doPlayOrderAndTrickPhaseMatch(game),
                "Expected IllegalStateException to be thrown");
    }

    @Test
    public void testDoPlayOrderAndTrickPhaseFinalTrickError() {
        game.setPhase(GamePhase.FINALTRICK);
        game.setCurrentPlayOrder(-1);
        game.setTrickPhase(TrickPhase.TRICKJUSTCOMPLETED);

        assertThrows(
                IllegalStateException.class,
                () -> gameService.doPlayOrderAndTrickPhaseMatch(game),
                "Expected IllegalStateException to be thrown");
    }

    @Test
    public void testDoPlayOrderAndTrickPhaseResultError2() {
        game.setPhase(GamePhase.RESULT);
        game.setCurrentPlayOrder(-1);
        game.setTrickPhase(TrickPhase.TRICKJUSTCOMPLETED);

        assertThrows(
                IllegalStateException.class,
                () -> gameService.doPlayOrderAndTrickPhaseMatch(game),
                "Expected IllegalStateException to be thrown");
    }

    @Test
    public void testDoPlayOrderAndTrickPhasePassingError3() {
        game.setPhase(GamePhase.PASSING);
        game.setCurrentPlayOrder(-1);
        game.setTrickPhase(TrickPhase.TRICKJUSTCOMPLETED);

        assertThrows(
                IllegalStateException.class,
                () -> gameService.doPlayOrderAndTrickPhaseMatch(game),
                "Expected IllegalStateException to be thrown");
    }

}
