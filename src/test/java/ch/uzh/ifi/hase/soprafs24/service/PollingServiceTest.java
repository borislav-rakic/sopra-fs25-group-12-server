package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.constant.TrickPhase;
import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PollingDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

        when(gameRepository.findActiveGameByMatchId(100L)).thenReturn(game);
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
}
