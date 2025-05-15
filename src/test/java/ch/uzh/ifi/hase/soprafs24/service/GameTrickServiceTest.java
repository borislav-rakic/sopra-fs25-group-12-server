package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.TrickPhase;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.repository.GameStatsRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.TrickDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameTrickServiceTest {

    private MatchPlayerRepository matchPlayerRepository;
    private CardRulesService cardRulesService;
    private GameStatsRepository gameStatsRepository;
    private GameTrickService gameTrickService;

    private Game game;
    private Match match;
    private MatchPlayer player;

    @BeforeEach
    void setup() {
        matchPlayerRepository = mock(MatchPlayerRepository.class);
        cardRulesService = mock(CardRulesService.class);
        gameStatsRepository = mock(GameStatsRepository.class);

        gameTrickService = new GameTrickService(matchPlayerRepository, cardRulesService, gameStatsRepository);

        game = new Game();
        game.setCurrentPlayOrder(0);
        game.setCurrentTrick(new ArrayList<>());
        game.setPhase(GamePhase.PASSING); // initial phase
        match = new Match();
        player = new MatchPlayer();
        player.setMatchPlayerSlot(1);
    }

    @Test
    void addCardToTrick_updatesTrickAndPlayOrder() {
        gameTrickService.addCardToTrick(match, game, player, "QS");

        assertEquals(1, game.getCurrentPlayOrder());
        assertEquals(1, game.getCurrentTrick().size());
        assertEquals("QS", game.getCurrentTrick().get(0));
    }

    @Test
    void updateGamePhaseBasedOnPlayOrder_setsCorrectPhase_firstTrick() {
        game.setCurrentPlayOrder(1);
        game.setPhase(GamePhase.PASSING);

        gameTrickService.updateGamePhaseBasedOnPlayOrder(game);

        assertEquals(GamePhase.FIRSTTRICK, game.getPhase());
        assertEquals(TrickPhase.READYFORFIRSTCARD, game.getTrickPhase());
    }

    @Test
    void updateGamePhaseBasedOnPlayOrder_setsCorrectPhase_finalTrick() {
        game.setCurrentPlayOrder(49);
        game.setPhase(GamePhase.NORMALTRICK);

        gameTrickService.updateGamePhaseBasedOnPlayOrder(game);

        assertEquals(GamePhase.FINALTRICK, game.getPhase());
    }

    @Test
    void updateGamePhaseBasedOnPlayOrder_setsResultPhase() {
        game.setCurrentPlayOrder(53);
        game.setPhase(GamePhase.FINALTRICK);

        gameTrickService.updateGamePhaseBasedOnPlayOrder(game);

        assertEquals(GamePhase.RESULT, game.getPhase());
    }

    @Test
    void determineWinnerOfTrick_setsWinnerSlot() {
        when(cardRulesService.determineTrickWinner(game)).thenReturn(2);
        gameTrickService.determineWinnerOfTrick(match, game);
        assertEquals(2, game.getPreviousTrickWinnerMatchPlayerSlot());
    }

    @Test
    void determinePointsOfTrick_addsPointsToCorrectPlayer() {
        game.setPreviousTrickWinnerMatchPlayerSlot(1);
        when(cardRulesService.calculateTrickPoints(game, 1)).thenReturn(10);
        when(matchPlayerRepository.findByMatchAndMatchPlayerSlot(match, 1)).thenReturn(player);

        player.setGameScore(5);
        gameTrickService.determinePointsOfTrick(match, game);

        assertEquals(15, player.getGameScore());
        verify(matchPlayerRepository).save(player);
    }

    @Test
    void afterCardPlayed_setsPhaseAndSlot() {
        game.setCurrentTrick(List.of("2C", "3D", "4H"));
        game.setCurrentMatchPlayerSlot(3);
        game.setMatch(match);
        match.setFastForwardMode(false);

        gameTrickService.afterCardPlayed(game);

        assertEquals(4, game.getCurrentMatchPlayerSlot());
    }

    @Test
    void clearTrick_resetsTrickProperly() {
        game.setCurrentTrick(new ArrayList<>(List.of("QS", "AH")));
        game.setPhase(GamePhase.NORMALTRICK);
        game.setCurrentTrickNumber(1);
        game.setTrickPhase(TrickPhase.TRICKJUSTCOMPLETED);

        gameTrickService.clearTrick(match, game);

        assertEquals(TrickPhase.READYFORFIRSTCARD, game.getTrickPhase());
        assertEquals(2, game.getCurrentTrickNumber());
        assertTrue(game.getCurrentTrick().isEmpty());
    }

    @Test
    void handlePotentialTrickCompletion_fastForwardModeTrue_clearsTrickImmediately() {
        game.setCurrentTrick(List.of("2C", "3D", "4H", "5S"));
        game.setCurrentTrickNumber(1);
        game.setMatch(match);
        match.setFastForwardMode(true);
        game.setTrickLeaderMatchPlayerSlot(1);

        when(cardRulesService.determineTrickWinner(game)).thenReturn(2);
        when(cardRulesService.calculateTrickPoints(game, 2)).thenReturn(5);
        when(matchPlayerRepository.findByMatchAndMatchPlayerSlot(match, 2)).thenReturn(player);
        when(gameStatsRepository.findByGameAndTrickNumber(game, 1)).thenReturn(List.of());

        gameTrickService.afterCardPlayed(game); // indirectly calls handlePotentialTrickCompletion

        assertEquals(TrickPhase.READYFORFIRSTCARD, game.getTrickPhase());
    }

    @Test
    void prepareTrickDTO_returnsCorrectRelativePositions() {
        Game game = new Game();
        game.setCurrentTrick(List.of("2C", "3D", "4H")); // cards by slots 2, 3, 4
        game.setTrickLeaderMatchPlayerSlot(2); // leader = slot 2

        MatchPlayer pollingPlayer = new MatchPlayer();
        pollingPlayer.setMatchPlayerSlot(2); // "me" = slot 2

        TrickDTO dto = gameTrickService.prepareTrickDTO(new Match(), game, pollingPlayer);

        assertEquals(3, dto.getCards().size());
        assertEquals(0, dto.getCards().get(0).getPosition()); // slot 2 = me
        assertEquals(1, dto.getCards().get(1).getPosition()); // slot 3 = left
        assertEquals(2, dto.getCards().get(2).getPosition()); // slot 4 = across
        assertEquals(0, dto.getTrickLeaderPosition()); // leader = me
        assertNull(dto.getWinningPosition()); // trick not complete
    }

    @Test
    void preparePreviousTrickDTO_includesWinnerPosition() {
        Game game = new Game();
        game.setTrickPhase(TrickPhase.TRICKJUSTCOMPLETED);
        game.setPreviousTrick(List.of("QS", "KH"));
        game.setPreviousTrickLeaderMatchPlayerSlot(2); // <-- leader is slot 2
        game.setPreviousTrickWinnerMatchPlayerSlot(3); // <-- winner is slot 3

        MatchPlayer pollingPlayer = new MatchPlayer();
        pollingPlayer.setMatchPlayerSlot(2); // <-- this is "me"

        TrickDTO dto = gameTrickService.preparePreviousTrickDTO(new Match(), game, pollingPlayer);

        // Trick order is [2, 3, 4, 1] — only first 2 used
        // Relative positions: 2 (me) = 0, 3 = 1
        assertEquals(0, dto.getCards().get(0).getPosition());
        assertEquals(1, dto.getCards().get(1).getPosition());
        assertEquals(1, dto.getWinningPosition()); // winner is slot 3 = position 1 relative to 2
    }

    @Test
    void handlePotentialTrickCompletion_recordsAndTransfersCards() {
        Match match = mock(Match.class);
        Game game = new Game();
        game.setCurrentTrick(List.of("QS", "KH", "2D", "AC"));
        game.setCurrentTrickNumber(1);
        game.setMatch(match);
        game.setTrickLeaderMatchPlayerSlot(1);

        when(cardRulesService.determineTrickWinner(game)).thenReturn(2);
        when(cardRulesService.calculateTrickPoints(game, 2)).thenReturn(13);

        MatchPlayer winner = new MatchPlayer();
        winner.setMatchPlayerSlot(2);
        winner.setGameScore(0);
        winner.setTakenCards(new ArrayList<>());
        when(matchPlayerRepository.findByMatchAndMatchPlayerSlot(match, 2)).thenReturn(winner);

        GameStats stat1 = new GameStats();
        stat1.setCardFromString("QS");
        when(gameStatsRepository.findByGameAndTrickNumber(game, 1)).thenReturn(List.of(stat1));

        gameTrickService.handlePotentialTrickCompletion(match, game);

        assertEquals(13, winner.getGameScore());
        assertEquals(List.of("QS"), winner.getTakenCards());
        assertEquals("QS", game.getPreviousTrick().get(0));
        assertEquals(2, game.getPreviousTrickWinnerMatchPlayerSlot());
    }

}
