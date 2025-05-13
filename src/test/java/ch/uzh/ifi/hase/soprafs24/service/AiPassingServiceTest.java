package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.Strategy;
import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.PassedCardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class AiPassingServiceTest {

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @Mock
    private PassedCardRepository passedCardRepository;

    @InjectMocks
    private AiPassingService aiPassingService;

    private MatchPlayer aiPlayer;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        User user = new User();
        user.setId(1L); // will map to LEFTMOST

        aiPlayer = new MatchPlayer();
        aiPlayer.setIsAiPlayer(true);
        aiPlayer.setMatchPlayerSlot(1);
        aiPlayer.setHand("QS,KH,AH,3D,5S,9H,JD,2C,4C,6D,7H,8S,0C");

        aiPlayer.setUser(user);
    }

    @Test
    public void selectCardsToPass_LEFTMOST_returnsFirstThreeCards() {
        List<String> result = aiPassingService.selectCardsToPass(aiPlayer, Strategy.LEFTMOST);
        assertEquals(List.of("2C", "4C", "0C"), result);
    }

    @Test
    public void selectCardsToPass_RANDOM_returnsThreeCards() {
        List<String> result = aiPassingService.selectCardsToPass(aiPlayer, Strategy.RANDOM);
        assertEquals(3, result.size());
    }

    @Test
    public void selectCardsToPass_DUMPHIGHESTFACEFIRST_returnsThreeCards() {
        List<String> result = aiPassingService.selectCardsToPass(aiPlayer, Strategy.DUMPHIGHESTFACEFIRST);
        assertEquals(3, result.size());
    }

    @Test
    public void selectCardsToPass_throwsIfHandTooSmall() {
        aiPlayer.setHand("KH,AH");
        assertThrows(IllegalStateException.class,
                () -> aiPassingService.selectCardsToPass(aiPlayer, Strategy.LEFTMOST));
    }

    @Test
    public void isPotentialMoonShooter_detectsShooter() {
        List<String> moonHand = List.of("KH", "QH", "AH", "9H", "8H", "7H", "QS", "KS", "AS");
        assertTrue(aiPassingService.isPotentialMoonShooter(moonHand));
    }

    @Test
    public void isPotentialMoonShooter_returnsFalseIfNotShooter() {
        List<String> normalHand = List.of("2C", "3C", "4D", "5H", "6S", "7D", "8H", "9H", "TH", "JH", "QD", "KD", "AD");
        assertFalse(aiPassingService.isPotentialMoonShooter(normalHand));
    }

    @Test
    public void dumpHighestScoringFaceFirst_returnsTop3() {
        List<String> hand = List.of("QS", "KH", "4D", "2C", "JH");
        List<String> result = aiPassingService.dumpHighestScoringFaceFirst(hand);
        assertEquals(3, result.size());
    }

    @Test
    public void passForAllAiPlayers_passesCardsAndSaves() {
        Game game = new Game();
        game.setGameNumber(1);
        Match match = mock(Match.class);
        game.setMatch(match);

        when(match.requireMatchPlayerBySlot(anyInt())).thenAnswer(invocation -> {
            int slot = invocation.getArgument(0);
            if (slot == 1)
                return aiPlayer; // Only slot 1 has an AI player
            return new MatchPlayer(); // Return dummy players (non-AI)
        });

        when(match.requireMatchPlayerBySlot(anyInt())).thenAnswer(invocation -> {
            int slot = invocation.getArgument(0);
            if (slot == 1) {
                return aiPlayer;
            } else {
                MatchPlayer dummy = mock(MatchPlayer.class);
                when(dummy.getIsAiPlayer()).thenReturn(false);
                return dummy;
            }
        });

        when(match.requireMatchPlayerBySlot(1)).thenReturn(aiPlayer);
        when(passedCardRepository.existsByGameAndRankSuit(any(), any())).thenReturn(false);

        aiPassingService.passForAllAiPlayers(game);

        verify(passedCardRepository, times(3)).save(any());
        verify(matchPlayerRepository, atLeastOnce()).flush();
    }

    @Test
    public void passForAllAiPlayers_throwsIfNoMatch() {
        Game game = new Game();
        game.setMatch(null);
        assertThrows(IllegalStateException.class, () -> aiPassingService.passForAllAiPlayers(game));
    }

    @Test
    public void getStrategyForUserId_returnsCorrectStrategy() throws Exception {
        Strategy strategy = Strategy.valueOf("DUMPHIGHESTFACEFIRST");
        assertEquals(strategy, Strategy.DUMPHIGHESTFACEFIRST);
        assertEquals(Strategy.LEFTMOST, aiPassingService.getStrategyForUserId(1L));
        assertEquals(Strategy.RANDOM, aiPassingService.getStrategyForUserId(2L));
        assertEquals(Strategy.LEFTMOST, aiPassingService.getStrategyForUserId(99L)); // default fallback
    }
}
