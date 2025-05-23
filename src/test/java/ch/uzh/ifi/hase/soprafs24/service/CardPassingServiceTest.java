package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.PassedCard;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.exceptions.GameplayException;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameStatsRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.PassedCardRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePassingDTO;

@ExtendWith(MockitoExtension.class)
public class CardPassingServiceTest {

    @InjectMocks
    private CardPassingService cardPassingService;

    @Mock
    private AiPassingService aiPassingService;

    @Mock
    private CardRulesService cardRulesService;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameStatsRepository gameStatsRepository;

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @Mock
    private PassedCardRepository passedCardRepository;

    private MatchPlayer matchPlayer;
    private Game game;
    private User user;
    private Match match;

    @BeforeEach
    void setup() {

        user = new User();
        user.setUsername("username");
        user.setPassword("password");
        user.setId(4L);
        user.setToken("1234");

        matchPlayer = new MatchPlayer();
        matchPlayer.setMatchPlayerSlot(1);
        matchPlayer.setHand("2C,3H,4D");
        matchPlayer.setUser(new User());

        match = new Match();
        match.setMatchPlayers(List.of(matchPlayer));
        matchPlayer.setMatch(match);

        game = new Game();
        game.setGameId(42L);
        game.setMatch(match);
        game.setGameNumber(1);
    }

    @Test
    void testCollectPassedCards_whenGameIsNull() {
        assertThrows(ResponseStatusException.class, () -> {
            cardPassingService.collectPassedCards(null);
        });
    }

    @Test
    void testCollectPassedCards_whenMatchIsNull() {
        game.setMatch(null);
        assertThrows(ResponseStatusException.class, () -> {
            cardPassingService.collectPassedCards(game);
        });
    }

    @Test
    void testPassingAcceptCards_whenAllCardsPassed() {
        GamePassingDTO dto = new GamePassingDTO();
        dto.setCards(List.of("2C", "3H", "4D"));

        when(passedCardRepository.existsByGameAndFromMatchPlayerSlotAndRankSuit(game, 1, "2C")).thenReturn(false);
        when(passedCardRepository.existsByGameAndRankSuit(game, "2C")).thenReturn(false);

        // Act
        cardPassingService.passingAcceptCards(game, matchPlayer, dto, false);

        // Assert
        verify(passedCardRepository).saveAll(anyList());
        verify(passedCardRepository).flush();
    }

    @Test
    void testPassingValidCards_successfullySavesCards() {
        GamePassingDTO dto = new GamePassingDTO();
        dto.setCards(List.of("2C", "3H", "4D"));

        // All cards are valid, owned, and not passed
        for (String card : dto.getCards()) {
            when(passedCardRepository.existsByGameAndFromMatchPlayerSlotAndRankSuit(game, 1, card))
                    .thenReturn(false);
            when(passedCardRepository.existsByGameAndRankSuit(game, card)).thenReturn(false);
        }

        // Act
        cardPassingService.passingAcceptCards(game, matchPlayer, dto, false);

        // Assert
        verify(passedCardRepository).saveAll(anyList());
        verify(passedCardRepository).flush();
    }

    @Test
    void testPassingFailsWithTooFewCards() {
        GamePassingDTO dto = new GamePassingDTO();
        dto.setCards(List.of("2C", "3H"));

        assertThrows(ResponseStatusException.class,
                () -> cardPassingService.passingAcceptCards(game, matchPlayer, dto, false));
    }

    @Test
    void testPassingFailsWithDuplicateCards() {
        GamePassingDTO dto = new GamePassingDTO();
        dto.setCards(List.of("2C", "2C", "3H"));

        assertThrows(ResponseStatusException.class,
                () -> cardPassingService.passingAcceptCards(game, matchPlayer, dto, false));
    }

    @Test
    void testPassingFailsWithInvalidFormat() {
        GamePassingDTO dto = new GamePassingDTO();
        dto.setCards(List.of("2C", "X1", "3H"));

        assertThrows(ResponseStatusException.class,
                () -> cardPassingService.passingAcceptCards(game, matchPlayer, dto, false));
    }

    @Test
    void testPassingFailsWhenCardNotOwned() {
        matchPlayer.setHand("2C,3H"); // No "4D"

        GamePassingDTO dto = new GamePassingDTO();
        dto.setCards(List.of("2C", "3H", "4D"));

        assertThrows(ResponseStatusException.class,
                () -> cardPassingService.passingAcceptCards(game, matchPlayer, dto, false));
    }

    @Test
    void testPassingFailsWhenCardAlreadyPassedByPlayer() {
        GamePassingDTO dto = new GamePassingDTO();
        dto.setCards(List.of("2C", "3H", "4D"));

        when(passedCardRepository.existsByGameAndFromMatchPlayerSlotAndRankSuit(
                eq(game), eq(1), eq("2C"))).thenReturn(true);

        assertThrows(ResponseStatusException.class,
                () -> cardPassingService.passingAcceptCards(game, matchPlayer, dto, false));
    }

    @Test
    public void testPlayerSlotToMatchPlayerSlot() {
        assertEquals(1, cardPassingService.playerSlotToMatchPlayerSlot(0));
        assertEquals(4, cardPassingService.playerSlotToMatchPlayerSlot(3));
    }

    @Test
    void testPlayerSlotToMatchPlayerSlot_whenInvalidSlot() {
        assertThrows(IllegalArgumentException.class, () -> {
            cardPassingService.playerSlotToMatchPlayerSlot(5); // Invalid slot > 3
        });
    }

    @Test
    public void testMatchPlayerSlotToPlayerSlot() {
        assertEquals(0, cardPassingService.matchPlayerSlotToPlayerSlot(1));
        assertEquals(3, cardPassingService.matchPlayerSlotToPlayerSlot(4));
    }

    @Test
    void testMatchPlayerSlotToPlayerSlot_whenInvalidSlot() {
        assertThrows(IllegalArgumentException.class, () -> {
            cardPassingService.matchPlayerSlotToPlayerSlot(5); // Invalid slot > 4
        });
    }

    @Test
    void passingAcceptCards_validInput_savesCards() {
        // Arrange
        Game game = new Game();
        Match match = new Match();
        MatchPlayer matchPlayer = new MatchPlayer();

        matchPlayer.setMatchPlayerSlot(1);
        matchPlayer.setHand("2H,3D,4S");
        matchPlayer.setMatch(match);
        match.setMatchPlayers(List.of(matchPlayer));
        game.setMatch(match);

        GamePassingDTO dto = new GamePassingDTO();
        dto.setCards(List.of("2H", "3D", "4S"));

        // Mock repository behavior: all cards not passed before
        for (String card : dto.getCards()) {
            when(passedCardRepository.existsByGameAndFromMatchPlayerSlotAndRankSuit(game, 1, card)).thenReturn(false);
            when(passedCardRepository.existsByGameAndRankSuit(game, card)).thenReturn(false);
        }

        when(passedCardRepository.countByGame(game)).thenReturn(3); // Initial state before passing

        // Act
        int result = cardPassingService.passingAcceptCards(game, matchPlayer, dto, false);

        // Assert
        assertEquals(3, result);
        verify(passedCardRepository, times(1)).saveAll(any());
        verify(passedCardRepository, times(1)).flush();
    }

    @Test
    void testCollectPassedCards_whenNotAllCardsPassed_throws() {
        when(passedCardRepository.findByGame(game)).thenReturn(List.of()); // fewer than 12

        assertThrows(GameplayException.class, () -> cardPassingService.collectPassedCards(game));
    }

    @Test
    void testCollectPassedCards_whenGameStatMissing_throws() {
        // 3 passed cards from slot 1
        PassedCard card1 = new PassedCard(game, "2C", 1, 1);
        PassedCard card2 = new PassedCard(game, "3H", 1, 1);
        PassedCard card3 = new PassedCard(game, "4D", 1, 1);
        List<PassedCard> passedCards = List.of(card1, card2, card3);

        // Setup players
        matchPlayer.setMatchPlayerSlot(1);
        MatchPlayer receiver = new MatchPlayer();
        receiver.setMatchPlayerSlot(2);
        match.setMatchPlayers(List.of(matchPlayer, receiver));
        game.setMatch(match);

        // Stub dependencies
        lenient().when(passedCardRepository.findByGame(game)).thenReturn(passedCards);
        lenient().when(cardRulesService.determinePassingDirection(1)).thenReturn(Map.of(1, 2));
        lenient().when(gameStatsRepository.findByRankSuitAndGameAndCardHolder("2C", game, 1)).thenReturn(null); // trigger
                                                                                                                // failure

        // Should throw due to missing GameStat
        assertThrows(GameplayException.class, () -> cardPassingService.collectPassedCards(game));
    }

    @Test
    void testCollectPassedCards_whenPassingDirectionInvalid_throws() {
        PassedCard passedCard = new PassedCard(game, "2C", 1, 1);
        List<PassedCard> passedCards = List.of(passedCard, new PassedCard(game, "3H", 1, 1),
                new PassedCard(game, "4D", 1, 1));

        when(passedCardRepository.findByGame(game)).thenReturn(passedCards);

        assertThrows(GameplayException.class, () -> cardPassingService.collectPassedCards(game));
    }

    @Test
    void testPassingAcceptCards_triggersAiPassing_whenAllHumanCardsPassed() {
        GamePassingDTO dto = new GamePassingDTO();
        dto.setCards(List.of("2C", "3H", "4D"));

        // Set up a human MatchPlayer
        User humanUser = new User();
        humanUser.setIsAiPlayer(false);

        MatchPlayer humanPlayer = new MatchPlayer();
        humanPlayer.setMatchPlayerSlot(1);
        humanPlayer.setUser(humanUser);
        humanPlayer.setHand("2C,3H,4D");
        humanPlayer.setMatch(new Match());

        // Match contains just the one human
        Match match = new Match();
        match.setMatchPlayers(List.of(humanPlayer));
        humanPlayer.setMatch(match);

        Game game = new Game();
        game.setGameNumber(1);
        game.setMatch(match);

        // Stubs
        for (String card : dto.getCards()) {
            when(passedCardRepository.existsByGameAndFromMatchPlayerSlotAndRankSuit(game, 1, card)).thenReturn(false);
            when(passedCardRepository.existsByGameAndRankSuit(game, card)).thenReturn(false);
        }

        // First call simulates 3 human cards passed, triggering AI pass
        when(passedCardRepository.countByGame(game)).thenReturn(3).thenReturn(12);

        int count = cardPassingService.passingAcceptCards(game, humanPlayer, dto, false);

        assertEquals(12, count);
        verify(aiPassingService).passForAllAiPlayers(game);
    }

    @Test
    void testPassingFailsWithInvalidRegexFormat() {
        GamePassingDTO dto = new GamePassingDTO();
        dto.setCards(List.of("ZZ", "3H", "4D")); // "ZZ" invalid

        assertThrows(ResponseStatusException.class,
                () -> cardPassingService.passingAcceptCards(game, matchPlayer, dto, false));
    }

    @Test
    void testCollectPassedCards_successfullyReassignsCards() {
        List<PassedCard> passedCards = List.of(
                new PassedCard(game, "2C", 1, 1),
                new PassedCard(game, "3H", 1, 1),
                new PassedCard(game, "4D", 1, 1),
                new PassedCard(game, "5S", 2, 1),
                new PassedCard(game, "6H", 2, 1),
                new PassedCard(game, "7C", 2, 1),
                new PassedCard(game, "8D", 3, 1),
                new PassedCard(game, "9H", 3, 1),
                new PassedCard(game, "0S", 3, 1),
                new PassedCard(game, "JH", 4, 1),
                new PassedCard(game, "QD", 4, 1),
                new PassedCard(game, "KC", 4, 1));

        MatchPlayer p1 = new MatchPlayer();
        p1.setMatchPlayerSlot(1);
        p1.setHand("2C,3H,4D");
        MatchPlayer p2 = new MatchPlayer();
        p2.setMatchPlayerSlot(2);
        p2.setHand("5S,6H,7C");
        MatchPlayer p3 = new MatchPlayer();
        p3.setMatchPlayerSlot(3);
        p3.setHand("8D,9H,0S");
        MatchPlayer p4 = new MatchPlayer();
        p4.setMatchPlayerSlot(4);
        p4.setHand("JH,QD,KC");

        match.setMatchPlayers(List.of(p1, p2, p3, p4));
        game.setMatch(match);

        when(passedCardRepository.findByGame(game)).thenReturn(passedCards);
        when(cardRulesService.determinePassingDirection(1)).thenReturn(Map.of(
                1, 2, 2, 3, 3, 4, 4, 1));

        for (String code : List.of("2C", "3H", "4D", "5S", "6H", "7C", "8D", "9H", "0S", "JH", "QD", "KC")) {
            GameStats stat = new GameStats();
            stat.setCardFromString(code);
            when(gameStatsRepository.findByRankSuitAndGameAndCardHolder(eq(code), eq(game), anyInt())).thenReturn(stat);
        }

        cardPassingService.collectPassedCards(game);

        verify(passedCardRepository).deleteAll(passedCards);
        verify(passedCardRepository).flush();
        verify(gameStatsRepository).saveAll(anyList());
        verify(gameStatsRepository).flush();
    }

    @Test
    void testFindMatchPlayer_whenNoPlayerFound_throws() {
        match.setMatchPlayers(List.of()); // no players
        game.setMatch(match);

        // This will internally trigger findMatchPlayer through collectPassedCards
        when(passedCardRepository.findByGame(game)).thenReturn(List.of(
                new PassedCard(game, "2C", 1, 1),
                new PassedCard(game, "3H", 1, 1),
                new PassedCard(game, "4D", 1, 1),
                new PassedCard(game, "5S", 2, 1),
                new PassedCard(game, "6H", 2, 1),
                new PassedCard(game, "7C", 2, 1),
                new PassedCard(game, "8D", 3, 1),
                new PassedCard(game, "9H", 3, 1),
                new PassedCard(game, "0S", 3, 1),
                new PassedCard(game, "JH", 4, 1),
                new PassedCard(game, "QD", 4, 1),
                new PassedCard(game, "KC", 4, 1)));

        when(cardRulesService.determinePassingDirection(1)).thenReturn(Map.of(
                1, 2, 2, 3, 3, 4, 4, 1));

        assertThrows(IllegalStateException.class, () -> cardPassingService.collectPassedCards(game));
    }

}
