package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.constant.TrickPhase;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.MatchSummary;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PollingDTO;

public class PollingServiceTest {

    @Mock
    private CardRulesService cardRulesService;

    @Mock
    private MatchMessageService matchMessageService;

    @Mock
    private GameTrickService gameTrickService;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @InjectMocks
    private PollingService pollingService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetPlayerPolling_returnsCorrectDTO() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setUsername("Player1");
        user.setAvatar(1);

        Match match = new Match();
        match.setMatchId(100L);
        match.setMatchGoal(100);
        match.setHostId(1L);
        match.setPhase(MatchPhase.IN_PROGRESS);
        match.setPlayer1(user);
        match.setMatchSummary(new MatchSummary());

        MatchPlayer matchPlayer = new MatchPlayer();
        matchPlayer.setUser(user);
        matchPlayer.setMatchPlayerSlot(1);
        matchPlayer.setHand("AS,KH");
        matchPlayer.setLastPollTime(Instant.now().minusSeconds(60));
        matchPlayer.setMatchScore(42);

        match.setMatchPlayers(List.of(matchPlayer));

        Game game = new Game();
        game.setGameId(200L);
        game.setGameNumber(1);
        game.setPhase(GamePhase.NORMALTRICK);
        game.setTrickPhase(TrickPhase.RUNNINGTRICK);
        game.setCurrentMatchPlayerSlot(1);
        game.setHeartsBroken(false);
        match.setGames(List.of(game));

        when(matchPlayerRepository.findByUserAndMatch(user, match)).thenReturn(matchPlayer);
        when(cardRulesService.getPlayableCardsForMatchPlayerPolling(any(), any()))
                .thenReturn("AS");
        when(gameTrickService.prepareTrickDTO(any(), any(), any())).thenReturn(null);
        when(gameTrickService.preparePreviousTrickDTO(any(), any(), any())).thenReturn(null);
        when(matchMessageService.messages(any(), any(), any())).thenReturn(List.of());

        // Act
        PollingDTO dto = pollingService.getPlayerPolling(user, match, gameRepository, matchPlayerRepository);

        // Assert
        assertNotNull(dto);
        assertEquals(100L, dto.getMatchId());
        assertEquals(200L, dto.getPlayerCards().get(0).getGameId());
        assertEquals(2, dto.getPlayerCards().size());
        assertTrue(dto.getPlayerCardsAsString().contains("AS"));
        assertTrue(dto.getPlayableCardsAsString().contains("AS"));
        assertEquals(0, dto.getPlayerSlot());
    }

    @Test
    void testGetPlayerPollingForPostMatchPhase_gameResult() {
        Match match = new Match();
        match.setPhase(MatchPhase.RESULT);
        match.setMatchId(1L);
        match.setMatchGoal(100);
        match.setHostId(1L);
        match.setMatchSummary(new MatchSummary());

        User user = new User();
        user.setId(1L);
        match.setPlayer1(user);

        PollingDTO dto = pollingService.getPlayerPollingForPostMatchPhase(user, match, true);

        assertEquals(MatchPhase.RESULT, dto.getMatchPhase());
        assertEquals(1L, dto.getMatchId());
    }

    @Test
    void testGetSpectatorPolling_resultPhase() {
        Match match = new Match();
        match.setPhase(MatchPhase.RESULT);
        match.setMatchId(1L);
        match.setMatchGoal(100);
        match.setHostId(1L);
        match.setMatchSummary(new MatchSummary());

        User user = new User();
        user.setId(99L);

        PollingDTO dto = pollingService.getSpectatorPolling(user, match);

        assertEquals(MatchPhase.FINISHED, dto.getMatchPhase());
    }

    @Test
    void testRenderFinalMatchSummaryHtml_insertsPersonalSummary() {
        MatchSummary summary = new MatchSummary();
        summary.setMatchSummaryHtml("Base Summary <!--°--> End");
        summary.setMatchSummaryMatchPlayerSlot1("Well played!");

        User user = new User();
        user.setId(1L);

        Match match = new Match();
        match.setPlayer1(user);
        match.setMatchSummary(summary);

        String rendered = pollingService.renderFinalMatchSummaryHtml(user, match);

        assertTrue(rendered.contains("Well played!"));
        assertTrue(rendered.contains("personalSummary"));
    }

    @Test
    void testGetPersonalizedMatchSummary_returnsCorrectSlotSummary() {
        MatchSummary summary = new MatchSummary();
        summary.setMatchSummaryMatchPlayerSlot2("Bravo!");

        User user = new User();
        user.setId(2L);

        Match match = new Match();
        User player2 = new User();
        player2.setId(2L);
        match.setPlayer2(player2);
        match.setMatchSummary(summary);

        String result = pollingService.getPersonalizedMatchSummary(user, match);
        assertEquals("Bravo!", result);
    }

    @Test
    void testGetPlayerPollingForPostMatchPhase_unexpectedPhaseThrows() {
        Match match = new Match();
        match.setPhase(MatchPhase.SETUP); // not RESULT, FINISHED, or ABORTED

        User user = new User();
        user.setId(1L);

        Exception exception = assertThrows(IllegalStateException.class,
                () -> pollingService.getPlayerPollingForPostMatchPhase(user, match, false));

        assertTrue(exception.getMessage().contains("unexpected MatchPhase"));
    }

}
