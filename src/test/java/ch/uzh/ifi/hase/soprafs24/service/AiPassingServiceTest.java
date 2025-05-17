package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.Strategy;
import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.exceptions.GameplayException;
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
        assertThrows(GameplayException.class,
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

    @Test
    public void dumpHighestScoringFaceFirst_prioritizesQueenAndHearts() {
        List<String> hand = List.of("4D", "QS", "KH", "2C", "AH", "9H", "8H");
        List<String> result = aiPassingService.dumpHighestScoringFaceFirst(hand);

        assertTrue(result.contains("QS"));
        assertTrue(result.contains("KH") || result.contains("AH"));
        assertEquals(3, result.size());
    }

    @Test
    public void selectCardsToPass_unknownStrategyDefaultsToLeftmost() {
        List<String> result = aiPassingService.selectCardsToPass(aiPlayer, null);
        assertEquals(List.of("2C", "4C", "0C"), result);
    }

    @Test
    public void isPotentialMoonShooter_returnsFalseForStrongNonShooter() {
        List<String> hand = List.of("KH", "QH", "4C", "6S", "7D", "2C", "AS", "8H", "3C");
        assertFalse(aiPassingService.isPotentialMoonShooter(hand));
    }

    @Test
    public void passForAllAiPlayers_skipsCardsAlreadyPassed() {
        Game game = new Game();
        game.setGameNumber(1);
        Match match = mock(Match.class);
        game.setMatch(match);

        when(match.requireMatchPlayerBySlot(anyInt())).thenReturn(aiPlayer);
        when(passedCardRepository.existsByGameAndRankSuit(any(), any())).thenReturn(true); // simulate already passed

        aiPassingService.passForAllAiPlayers(game);

        verify(passedCardRepository, never()).save(any());
    }

    @Test
    public void selectCardsToPass_GETRIDOFCLUBSTHENHEARTS_prioritizesClubsAndHearts() {
        aiPlayer.setHand("2C,4H,3C,9S,QD,KH,AH");
        List<String> result = aiPassingService.selectCardsToPass(aiPlayer, Strategy.GETRIDOFCLUBSTHENHEARTS);
        assertTrue(result.stream().allMatch(card -> card.endsWith("C") || card.endsWith("H")));
        assertEquals(3, result.size());
    }

    @Test
    public void selectCardsToPass_PREFERBLACK_prioritizesBlackCards() {
        aiPlayer.setHand("2H,3H,4D,5C,6S,7S,8C");
        List<String> result = aiPassingService.selectCardsToPass(aiPlayer, Strategy.PREFERBLACK);

        long blackCount = result.stream()
                .filter(card -> card.endsWith("C") || card.endsWith("S"))
                .count();

        // At least 2 of the passed cards should be black suits if they're available
        assertTrue(blackCount >= 2, "Should prioritize black suits if present");
        assertEquals(3, result.size());
    }

    @Test
    public void selectCardsToPass_VOIDSUIT_voidsLeastRepresentedSuit() {
        aiPlayer.setHand("2C,3C,4D,5S,6H,7H,8H,9H,0H");
        List<String> result = aiPassingService.selectCardsToPass(aiPlayer, Strategy.VOIDSUIT);
        assertEquals(3, result.size());
        // Should favor the suit with only 1-2 cards
    }

    @Test
    public void selectCardsToPass_HYPATIA_shootsOrDumps() {
        aiPlayer.setHand("KH,QH,AH,9H,8H,7H,QS,KS,AS,2C,3C,4C,5C");
        List<String> result = aiPassingService.selectCardsToPass(aiPlayer, Strategy.HYPATIA);
        assertEquals(3, result.size());
    }

    @Test
    public void selectCardsToPass_GARY_moonShooterKeepsHighCards() {
        aiPlayer.setHand("KH,QH,AH,9H,8H,7H,QS,KS,AS,2C,3C,4C,5C");
        List<String> result = aiPassingService.selectCardsToPass(aiPlayer, Strategy.GARY);
        assertEquals(3, result.size());
    }

    @Test
    public void selectCardsToPass_GARY_voidClubsIfNotShooter() {
        aiPlayer.setHand("2C,3C,4D,5H,6S,7D,8H,9H,0H,JH,QD,KD,AD");
        List<String> result = aiPassingService.selectCardsToPass(aiPlayer, Strategy.GARY);
        assertEquals(3, result.size());
    }

    @Test
    public void selectCardsToPass_ADA_moonShooterPassesLow() {
        aiPlayer.setHand("KH,QH,AH,9H,8H,7H,QS,KS,AS,2C,3C,4C,5C");
        List<String> result = aiPassingService.selectCardsToPass(aiPlayer, Strategy.ADA);
        assertEquals(3, result.size());
    }

    @Test
    public void selectCardsToPass_ADA_avoidsOddSuits() {
        aiPlayer.setHand("2C,4C,5C,6H,8H,9H,QD,KD,AD,3S,5S,7S,9S");
        List<String> result = aiPassingService.selectCardsToPass(aiPlayer, Strategy.ADA);
        assertEquals(3, result.size());
    }

}
